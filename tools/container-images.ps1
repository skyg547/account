[CmdletBinding()]
param(
    [ValidateSet('List', 'VerifyPackages', 'Build')]
    [string]$Mode = 'List',

    [string[]]$Target,

    [ValidateSet('docker', 'podman')]
    [string]$Engine = 'docker',

    [string]$TagPrefix = 'account',

    [string]$OutputPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$manifestPath = Join-Path $repositoryRoot 'deploy\image-targets.json'
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$targetFilter = @(
    $Target |
        ForEach-Object { $_ -split ',' } |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ }
)

$targets = @($manifest.targets)
if ($targetFilter.Count -gt 0) {
    $targets = @($targets | Where-Object { $targetFilter -contains $_.name })
    if ($targets.Count -ne $targetFilter.Count) {
        $matched = @($targets | Select-Object -ExpandProperty name)
        $missing = @($targetFilter | Where-Object { $matched -notcontains $_ })
        throw "Unknown image target(s): $($missing -join ', ')"
    }
}

function Invoke-CapturedProcess {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$Arguments,

        [Parameter(Mandatory)]
        [string]$WorkingDirectory
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $escapedArguments = foreach ($argument in $Arguments) {
        if ($argument -notmatch '[\s"]') {
            $argument
            continue
        }
        '"' + ($argument -replace '(\\*)"', '$1$1\"' -replace '(\\+)$', '$1$1') + '"'
    }
    $startInfo.Arguments = $escapedArguments -join ' '

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $standardOutputTask = $process.StandardOutput.ReadToEndAsync()
    $standardErrorTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $standardOutput = $standardOutputTask.Result
    $standardError = $standardErrorTask.Result

    [pscustomobject]@{
        ExitCode = $process.ExitCode
        Output   = ($standardOutput + "`n" + $standardError).Trim()
        Command  = "$FilePath $($Arguments -join ' ')"
    }
}

function Test-JavaPackage {
    param([object]$ImageTarget)

    if (-not $ImageTarget.enabled) {
        return [pscustomobject]@{
            Name      = $ImageTarget.name
            Kind      = $ImageTarget.kind
            Status    = 'BLOCKED'
            Project   = $ImageTarget.gradleProject
            Artifact  = $null
            ExitCode  = $null
            Evidence  = $ImageTarget.blockedBy
        }
    }

    $result = Invoke-CapturedProcess `
        -FilePath $gradleWrapper `
        -Arguments @(
            "$($ImageTarget.gradleProject):bootJar",
            '--offline',
            '--console=plain',
            '--max-workers=1',
            '--no-daemon'
        ) `
        -WorkingDirectory $repositoryRoot

    $artifactDirectory = Join-Path $repositoryRoot "$($ImageTarget.jarDirectory)\build\libs"
    $artifacts = @()
    if ($result.ExitCode -eq 0 -and (Test-Path -LiteralPath $artifactDirectory -PathType Container)) {
        $artifacts = @(
            Get-ChildItem -LiteralPath $artifactDirectory -File -Filter '*.jar' |
                Where-Object { $_.Name -notmatch '-plain\.jar$' }
        )
    }

    [pscustomobject]@{
        Name      = $ImageTarget.name
        Kind      = $ImageTarget.kind
        Status    = if ($result.ExitCode -eq 0 -and $artifacts.Count -eq 1) {
            'PASS'
        } elseif ($result.ExitCode -eq 0) {
            'FAIL_ARTIFACT_COUNT'
        } else {
            'FAIL'
        }
        Project   = $ImageTarget.gradleProject
        Artifact  = if ($artifacts.Count -eq 1) { $artifacts[0].FullName } else { $null }
        ExitCode  = $result.ExitCode
        Evidence  = if ($result.ExitCode -eq 0) {
            "executable artifact count=$($artifacts.Count)"
        } else {
            (@($result.Output -split '\r?\n') | Select-Object -Last 12) -join "`n"
        }
    }
}

function Build-Image {
    param([object]$ImageTarget)

    if (-not $ImageTarget.enabled) {
        return [pscustomobject]@{
            Name     = $ImageTarget.name
            Status   = 'BLOCKED'
            Image    = $null
            ExitCode = $null
            Evidence = $ImageTarget.blockedBy
        }
    }

    $tag = "$TagPrefix/$($ImageTarget.name):local"
    if ($ImageTarget.kind -eq 'frontend') {
        # Next bakes rewrites() into routes-manifest.json during `npm run build`,
        # so GATEWAY_INTERNAL_URL has to reach the builder stage as a build arg.
        # Supplying it at container runtime is too late. Without forwarding
        # buildArgs the Containerfile default becomes the only reachable value,
        # and a gateway at a different address silently yields an image whose
        # /api/* calls 404.
        $arguments = @(
            'build',
            '--file', $ImageTarget.containerfile
        )
        if ($ImageTarget.PSObject.Properties.Name -contains 'buildArgs' -and $null -ne $ImageTarget.buildArgs) {
            foreach ($name in $ImageTarget.buildArgs.PSObject.Properties.Name) {
                $value = $ImageTarget.buildArgs.$name
                # A same-named environment variable wins, so an operator can point
                # at a different gateway without editing the tracked manifest.
                $override = [Environment]::GetEnvironmentVariable($name)
                if (-not [string]::IsNullOrWhiteSpace($override)) {
                    $value = $override
                }
                $arguments += @('--build-arg', "$name=$value")
            }
        }
        $arguments += @('--tag', $tag, $ImageTarget.context)
    } else {
        $moduleDockerfile = "$($ImageTarget.jarDirectory)/Dockerfile"
        $arguments = @(
            'build',
            '--file', $moduleDockerfile,
            '--tag', $tag,
            '.'
        )
    }

    $result = Invoke-CapturedProcess `
        -FilePath (Get-Command $Engine -ErrorAction Stop).Source `
        -Arguments $arguments `
        -WorkingDirectory $repositoryRoot

    [pscustomobject]@{
        Name     = $ImageTarget.name
        Status   = if ($result.ExitCode -eq 0) { 'PASS' } else { 'FAIL' }
        Image    = $tag
        ExitCode = $result.ExitCode
        Evidence = if ($result.ExitCode -eq 0) {
            'local image built; no push performed'
        } else {
            (@($result.Output -split '\r?\n') | Select-Object -Last 12) -join "`n"
        }
    }
}

$report = [ordered]@{
    Manifest = $manifestPath
    Mode     = $Mode
}

switch ($Mode) {
    'List' {
        $report.Targets = $targets
    }
    'VerifyPackages' {
        $report.Results = @(
            $targets |
                Where-Object { $_.kind -ne 'frontend' } |
                ForEach-Object { Test-JavaPackage -ImageTarget $_ }
        )
        $report.Frontend = [pscustomobject]@{
            Name          = 'frontend'
            Status        = 'STATIC_ONLY'
            Containerfile = 'frontend/Containerfile'
            Evidence      = 'npm install/build is intentionally not performed by VerifyPackages'
        }
    }
    'Build' {
        $report.Results = @($targets | ForEach-Object { Build-Image -ImageTarget $_ })
    }
}

$json = $report | ConvertTo-Json -Depth 8
if ($OutputPath) {
    $resolvedOutput = if ([System.IO.Path]::IsPathRooted($OutputPath)) {
        [System.IO.Path]::GetFullPath($OutputPath)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputPath))
    }
    [System.IO.File]::WriteAllText(
        $resolvedOutput,
        $json,
        [System.Text.UTF8Encoding]::new($false)
    )
    Write-Output "Wrote container image result: $resolvedOutput"
} else {
    Write-Output $json
}

$failedResults = @()
if ($Mode -in @('VerifyPackages', 'Build')) {
    $failedResults = @($report.Results | Where-Object { $_.Status -like 'FAIL*' })
}
if ($failedResults.Count -gt 0) {
    throw "Container image validation failed for: $(
        ($failedResults | Select-Object -ExpandProperty Name) -join ', '
    )"
}
