[CmdletBinding()]
param(
    [ValidateSet('Inventory', 'TaskContract', 'Packaging', 'Libraries', 'LocalJar', 'ProfileJar', 'Frontend', 'All')]
    [string]$Mode = 'All',

    [string]$OutputPath,

    [string]$PackagingResultPath,

    [string[]]$Project,

    [ValidateRange(30, 3600)]
    [int]$GradleTimeoutSeconds = 600,

    [ValidateRange(5, 180)]
    [int]$StartupTimeoutSeconds = 30
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
$frontendRoot = Join-Path $repositoryRoot 'frontend'
$isolatedRuntimeEnvironmentNameAllowlist = @(
    'SystemRoot',
    'WINDIR',
    'ComSpec',
    'PATH',
    'PATHEXT',
    'TEMP',
    'TMP',
    'JAVA_HOME',
    'LANG',
    'LC_ALL',
    'LC_CTYPE',
    'TZ'
)
$projectFilter = @(
    $Project |
        ForEach-Object { $_ -split ',' } |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ }
)

function Stop-ScopedProcessTree {
    param(
        [Parameter(Mandatory)]
        [System.Diagnostics.Process]$Process
    )

    if ($Process.HasExited) {
        return ''
    }

    try {
        $killTreeMethod = $Process.GetType().GetMethod('Kill', [type[]]@([bool]))
        if ($null -ne $killTreeMethod) {
            [void]$killTreeMethod.Invoke($Process, [object[]]@($true))
        } elseif ($env:OS -eq 'Windows_NT' -and (Get-Command 'taskkill.exe' -ErrorAction SilentlyContinue)) {
            $taskKillOutput = & taskkill.exe /PID $Process.Id /T /F 2>&1
            if ($LASTEXITCODE -ne 0 -and -not $Process.HasExited) {
                throw "taskkill failed: $($taskKillOutput -join ' ')"
            }
        } else {
            $Process.Kill()
        }
        if (-not $Process.WaitForExit(5000)) {
            throw "process $($Process.Id) did not exit within 5 seconds after tree termination"
        }
        ''
    } catch {
        $_.Exception.Message
    }
}

function Invoke-ProcessCapture {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$Arguments,

        [Parameter(Mandatory)]
        [string]$WorkingDirectory,

        [int]$TimeoutSeconds = $GradleTimeoutSeconds
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
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
    $completed = $process.WaitForExit($TimeoutSeconds * 1000)

    $terminationError = ''
    if (-not $completed) {
        $terminationError = Stop-ScopedProcessTree -Process $process
        [void]$process.WaitForExit(5000)
        if (-not $process.HasExited) {
            $terminationError = (@(
                $terminationError,
                "process $($process.Id) is still running after the termination wait"
            ) | Where-Object { $_ }) -join '; '
        }
    } else {
        $process.WaitForExit()
    }

    $standardOutputReady = $standardOutputTask.Wait(5000)
    $standardErrorReady = $standardErrorTask.Wait(5000)
    $standardOutput = if ($standardOutputReady) { $standardOutputTask.Result } else { '' }
    $standardError = if ($standardErrorReady) { $standardErrorTask.Result } else { '' }
    if (-not $completed) {
        $standardError = ($standardError + "`nAUDIT_PROCESS_TIMEOUT after $TimeoutSeconds seconds").Trim()
    }
    if ($terminationError) {
        $standardError = ($standardError + "`nAUDIT_PROCESS_TREE_TERMINATION_FAILED: $terminationError").Trim()
    }

    [pscustomobject]@{
        ExitCode = if ($completed) { $process.ExitCode } else { $null }
        StdOut   = $standardOutput
        StdErr   = $standardError
        Command  = "$FilePath $($Arguments -join ' ')"
        TimedOut = -not $completed
        TerminationError = $terminationError
    }
}

function Invoke-ProcessCaptureWithTimeout {
    param(
        [Parameter(Mandatory)]
        [string]$FilePath,

        [Parameter(Mandatory)]
        [string[]]$Arguments,

        [Parameter(Mandatory)]
        [string]$WorkingDirectory,

        [Parameter(Mandatory)]
        [int]$TimeoutSeconds,

        [string[]]$EnvironmentVariableNamesToKeep = @()
    )

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $removedEnvironmentVariables = [System.Collections.Generic.List[string]]::new()
    if ($EnvironmentVariableNamesToKeep.Count -gt 0) {
        $keptEnvironmentVariables = [System.Collections.Generic.HashSet[string]]::new(
            [System.StringComparer]::OrdinalIgnoreCase
        )
        foreach ($environmentVariableName in $EnvironmentVariableNamesToKeep) {
            [void]$keptEnvironmentVariables.Add($environmentVariableName)
        }
        foreach ($environmentVariableName in @($startInfo.EnvironmentVariables.Keys)) {
            if (-not $keptEnvironmentVariables.Contains($environmentVariableName)) {
                [void]$startInfo.EnvironmentVariables.Remove($environmentVariableName)
                $removedEnvironmentVariables.Add($environmentVariableName)
            }
        }
    }

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
    $completed = $process.WaitForExit($TimeoutSeconds * 1000)

    $terminationError = ''
    if (-not $completed) {
        $terminationError = Stop-ScopedProcessTree -Process $process
        [void]$process.WaitForExit(5000)
        if (-not $process.HasExited) {
            $terminationError = (@(
                $terminationError,
                "process $($process.Id) is still running after the termination wait"
            ) | Where-Object { $_ }) -join '; '
        }
    } else {
        $process.WaitForExit()
    }

    $standardOutputReady = $standardOutputTask.Wait(5000)
    $standardErrorReady = $standardErrorTask.Wait(5000)
    $standardOutput = if ($standardOutputReady) { $standardOutputTask.Result } else { '' }
    $standardError = if ($standardErrorReady) { $standardErrorTask.Result } else { '' }
    if (-not $completed) {
        $standardError = ($standardError + "`nAUDIT_PROCESS_TIMEOUT after $TimeoutSeconds seconds").Trim()
    }
    if ($terminationError) {
        $standardError = ($standardError + "`nAUDIT_PROCESS_TREE_TERMINATION_FAILED: $terminationError").Trim()
    }

    [pscustomobject]@{
        ExitCode = if ($completed) { $process.ExitCode } else { $null }
        StdOut   = $standardOutput
        StdErr   = $standardError
        Command  = "$FilePath $($Arguments -join ' ')"
        TimedOut = -not $completed
        TerminationError = $terminationError
        EnvironmentVariablesRemoved = @($removedEnvironmentVariables | Sort-Object -Unique)
    }
}

function Protect-EvidenceText {
    param([string]$Text)

    if (-not $Text) {
        return ''
    }

    $protected = $Text
    $sourceLocations = [System.Collections.Generic.List[string]]::new()
    $sourceLocationTokenPrefix = "__AUDIT_SOURCE_$([System.Guid]::NewGuid().ToString('N'))_"
    $protected = [regex]::Replace(
        $protected,
        '(?im)(?<prefix>\bat\s+[a-z0-9_.$<>/]+\()(?<source>[a-z0-9_$-]+\.(?:java|kt|kts|groovy):\d+)(?=\))',
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)

            $index = $sourceLocations.Count
            $sourceLocations.Add($match.Groups['source'].Value)
            "$($match.Groups['prefix'].Value)${sourceLocationTokenPrefix}${index}__"
        }
    )
    $protected = [regex]::Replace(
        $protected,
        '(?im)^(?<indent>\s*)(?<source>[a-z0-9_$-]+\.(?:java|class|kt|kts|groovy|gradle|xml|yml|yaml|properties|md|ps1):\d+)\s*$',
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)

            $index = $sourceLocations.Count
            $sourceLocations.Add($match.Groups['source'].Value)
            "$($match.Groups['indent'].Value)${sourceLocationTokenPrefix}${index}__"
        }
    )
    $protected = [regex]::Replace(
        $protected,
        '(?i)(?<!\S)(?<source>(?:[a-z]:[\\/]+|\.{1,2}[\\/]+)[^\s()]*\.(?:java|class|kt|kts|groovy|gradle|xml|yml|yaml|properties|md|ps1):\d+)\b',
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)

            $index = $sourceLocations.Count
            $sourceLocations.Add($match.Groups['source'].Value)
            "${sourceLocationTokenPrefix}${index}__"
        }
    )
    $protected = $protected -replace '(?i)-Duser\.home=[^\s]+', '-Duser.home=[ISOLATED_USER_HOME]'
    $protected = $protected -replace '(?i)([a-z]:[\\/]+users[\\/]+)[^\\/\s]+', '$1[REDACTED_USER]'
    $protected = $protected -replace '(?i)jdbc:postgresql://[^\s"'']+', 'jdbc:postgresql://[REDACTED]'
    $protected = $protected -replace '(?i)\b(?:https?|r2dbc|redis|kafka)://[^\s"'']+', '[REDACTED_URI]'
    $protected = $protected -replace '(?i)\b(host|hostname|server|node|address)\s*[:=]\s*(?!(?:localhost|127\.0\.0\.1|0\.0\.0\.0)(?::\d{2,5})?\b)[^\s,;()]+', '$1=[REDACTED_HOST]'
    $protected = $protected -replace '(?i)\b((?:connect(?:ing|ed)?|connection)\s+to)\s+(?!(?:localhost|127\.0\.0\.1|0\.0\.0\.0)(?::\d{2,5})?\b)[^\s,;()]+', '$1 [REDACTED_HOST]'
    $protected = [regex]::Replace(
        $protected,
        '(?i)\[(?<address>[0-9a-f:]+)(?:%[0-9a-z_.-]+)?\](?::\d{1,5})?',
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)

            $address = $null
            if ([System.Net.IPAddress]::TryParse($match.Groups['address'].Value, [ref]$address) -and
                -not [System.Net.IPAddress]::IsLoopback($address)) {
                return '[REDACTED_IPV6]'
            }
            $match.Value
        }
    )
    $protected = [regex]::Replace(
        $protected,
        '(?i)(?<![0-9a-z:])(?<address>(?:[0-9a-f]{0,4}:){2,}[0-9a-f]{0,4})(?:%[0-9a-z_.-]+)?(?![0-9a-z:])',
        [System.Text.RegularExpressions.MatchEvaluator]{
            param($match)

            $address = $null
            if ([System.Net.IPAddress]::TryParse($match.Groups['address'].Value, [ref]$address) -and
                -not [System.Net.IPAddress]::IsLoopback($address)) {
                return '[REDACTED_IPV6]'
            }
            $match.Value
        }
    )
    $protected = $protected -replace '(?<![a-zA-Z0-9_.-])(?!(?:127|0)\.)\d{1,3}(?:\.\d{1,3}){3}\b', '[REDACTED_IP]'
    $protected = $protected -replace '(?i)(?<![a-z0-9_.-])(?!(?:localhost|127\.0\.0\.1|0\.0\.0\.0):)[a-z][a-z0-9.-]*:\d{2,5}\b', '[REDACTED_HOST]'
    $protected = $protected -replace '(?i)\b(password|passwd|secret|token|credential|username|user)\s*[:=]\s*[^\s,;]+', '$1=[REDACTED]'
    for ($index = 0; $index -lt $sourceLocations.Count; $index++) {
        $protected = $protected.Replace(
            "${sourceLocationTokenPrefix}${index}__",
            $sourceLocations[$index]
        )
    }
    $protected
}

function Get-TextFileContent {
    param([string[]]$Paths)

    $content = foreach ($path in $Paths) {
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            Get-Content -LiteralPath $path -Raw
        }
    }
    $content -join "`n"
}

function Get-ProjectDirectory {
    param([string]$ProjectPath)

    $segments = $ProjectPath.TrimStart(':').Split(':')
    Join-Path $repositoryRoot ($segments -join [System.IO.Path]::DirectorySeparatorChar)
}

function Get-ProjectClassification {
    param(
        [string]$ProjectPath,
        [bool]$HasBootApplication
    )

    if ($ProjectPath -eq ':app') {
        return 'phantom'
    }
    if ($ProjectPath -in @(':config-server', ':discovery', ':gateway')) {
        return 'infra-server'
    }
    if ($ProjectPath -eq ':migration-runner') {
        return 'cli'
    }
    if ($ProjectPath -match ':(api|mart-api|ecl-api)$') {
        return 'api'
    }
    if ($ProjectPath -match ':(batch|mart-batch|ecl-batch)$') {
        return 'batch'
    }
    if ($ProjectPath -match ':(core|mart-core|ecl-core)$' -or
        $ProjectPath -in @(':contracts', ':shared-kernel')) {
        return 'library'
    }
    if ($HasBootApplication) {
        return 'standalone-server'
    }
    return 'aggregator'
}

function Get-GradleProjectPaths {
    $result = Invoke-ProcessCapture `
        -FilePath $gradleWrapper `
        -Arguments @('projects', '--offline', '--console=plain', '--no-daemon') `
        -WorkingDirectory $repositoryRoot

    if ($result.ExitCode -ne 0) {
        throw "Gradle project inventory failed.`n$($result.StdOut)`n$($result.StdErr)"
    }

    $paths = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    foreach ($match in [regex]::Matches($result.StdOut, "Project '(:[^']+)'")) {
        [void]$paths.Add($match.Groups[1].Value)
    }
    @($paths | Sort-Object)
}

function Get-RuntimeInventory {
    $rootContainerfile = Join-Path $repositoryRoot 'Containerfile'
    $rootDevComposeText = Get-TextFileContent -Paths @(
        (Join-Path $repositoryRoot 'docker-compose.yml')
    )
    $rootProdComposeText = Get-TextFileContent -Paths @(
        (Join-Path $repositoryRoot 'compose.prod.yml')
    )

    $inventory = foreach ($projectPath in Get-GradleProjectPaths) {
        $projectDirectory = Get-ProjectDirectory -ProjectPath $projectPath
        $buildFile = Join-Path $projectDirectory 'build.gradle'
        $buildText = Get-TextFileContent -Paths @($buildFile)

        $javaRoot = Join-Path $projectDirectory 'src\main\java'
        $applicationFiles = @()
        if (Test-Path -LiteralPath $javaRoot -PathType Container) {
            $applicationFiles = @(
                Get-ChildItem -LiteralPath $javaRoot -Recurse -File -Filter '*.java' |
                    Where-Object {
                        Select-String -LiteralPath $_.FullName -Pattern '@SpringBootApplication' -Quiet
                    }
            )
        }

        $resourceRoot = Join-Path $projectDirectory 'src\main\resources'
        $resourceFiles = @()
        if (Test-Path -LiteralPath $resourceRoot -PathType Container) {
            $resourceFiles = @(
                Get-ChildItem -LiteralPath $resourceRoot -File |
                    Where-Object { $_.Name -match '^application.*\.(yml|yaml|properties)$' }
            )
        }
        $resourcePaths = @($resourceFiles | ForEach-Object { $_.FullName })
        $resourceText = Get-TextFileContent -Paths $resourcePaths
        $localResourceFiles = @(
            $resourceFiles | Where-Object {
                $_.Name -match '^application-local\.(yml|yaml|properties)$'
            }
        )
        $localResourceText = Get-TextFileContent -Paths @(
            $localResourceFiles | ForEach-Object { $_.FullName }
        )
        $localDocuments = @(
            foreach ($resourceFile in $resourceFiles) {
                $fileText = Get-TextFileContent -Paths @($resourceFile.FullName)
                foreach ($document in @($fileText -split '(?m)^---\s*$')) {
                    if ($document -match '(?m)^\s*(default|active):\s*local\s*$' -or
                        $document -match '(?m)^\s*on-profile:\s*local\s*$') {
                        $document
                    }
                }
            }
        )
        $localDocumentText = $localDocuments -join "`n"

        $classification = Get-ProjectClassification `
            -ProjectPath $projectPath `
            -HasBootApplication ($applicationFiles.Count -gt 0)

        $topLevelModule = $projectPath.TrimStart(':').Split(':')[0]
        $moduleDirectory = Join-Path $repositoryRoot $topLevelModule
        $dockerfile = Join-Path $moduleDirectory 'Dockerfile'
        $composeFile = Join-Path $moduleDirectory 'docker-compose.yml'
        $projectSegments = $projectPath.TrimStart(':').Split(':')
        $serviceName = if ($projectSegments.Count -eq 1) {
            $projectSegments[0]
        } elseif ($projectSegments[-1] -in @('api', 'batch')) {
            "$($projectSegments[0])-$($projectSegments[-1])"
        } elseif ($projectSegments[-1] -in @('mart-api', 'ecl-api')) {
            "$($projectSegments[0])-api"
        } elseif ($projectSegments[-1] -in @('mart-batch', 'ecl-batch')) {
            "$($projectSegments[0])-batch"
        } else {
            $projectSegments -join '-'
        }
        $composeServicePattern = "(?m)^  $([regex]::Escape($serviceName)):\s*$"

        [pscustomobject]@{
            Project              = $projectPath
            Classification       = $classification
            DirectoryExists      = Test-Path -LiteralPath $projectDirectory -PathType Container
            BuildFile            = Test-Path -LiteralPath $buildFile -PathType Leaf
            BootApplications     = $applicationFiles.Count
            BootApplicationNames = @($applicationFiles | ForEach-Object { $_.BaseName }) -join ', '
            BootJarDisabled      = $buildText -match '(?s)bootJar\s*\{[^}]*enabled\s*=\s*false'
            H2Dependency         = $buildText -match '(?m)^\s*(runtimeOnly|implementation|api)\s*(\(\s*)?[\x27\x22]com\.h2database:h2'
            PostgreSqlDependency = $buildText -match '(?m)^\s*(runtimeOnly|implementation|api)\s*(\(\s*)?[\x27\x22]org\.postgresql:postgresql'
            ExplicitH2Config     = $resourceText -match 'jdbc:h2:'
            ExplicitLocalH2Config = $localResourceText -match 'jdbc:h2:' -or
                $localDocumentText -match 'jdbc:h2:'
            ExplicitLocalProfile = $localResourceFiles.Count -gt 0 -or
                $localDocuments.Count -gt 0
            LocalProfileFiles    = @($localResourceFiles | Select-Object -ExpandProperty Name)
            ExplicitDevProfile   = @($resourceFiles | Where-Object {
                    $_.Name -match '^application-dev\.(yml|yaml|properties)$'
                }).Count -gt 0 -or
                $resourceText -match '(?m)on-profile:\s*dev\s*$'
            ExplicitProdProfile  = @($resourceFiles | Where-Object {
                    $_.Name -match '^application-prod\.(yml|yaml|properties)$'
                }).Count -gt 0 -or
                $resourceText -match '(?m)on-profile:\s*prod\s*$'
            ModuleDockerfile     = Test-Path -LiteralPath $dockerfile -PathType Leaf
            ModuleCompose        = Test-Path -LiteralPath $composeFile -PathType Leaf
            RootContainerfile    = Test-Path -LiteralPath $rootContainerfile -PathType Leaf
            RootDevComposeService = $rootDevComposeText -match $composeServicePattern
            RootProdComposeService = $rootProdComposeText -match $composeServicePattern
        }
    }

    @($inventory)
}

function Convert-InventoryToMarkdown {
    param([object[]]$Inventory)

    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add('| Gradle project | Role | Boot apps | H2 dep/config | PostgreSQL dep | dev/prod config | Docker/Compose |')
    $lines.Add('| --- | --- | ---: | --- | --- | --- | --- |')

    foreach ($item in $Inventory) {
        $h2 = if ($item.H2Dependency -and $item.ExplicitLocalH2Config) {
            'runtime dependency + local config'
        } elseif ($item.H2Dependency) {
            'runtime dependency only'
        } elseif ($item.ExplicitLocalH2Config) {
            'local config only'
        } else {
            '-'
        }
        $postgres = if ($item.PostgreSqlDependency) { 'yes' } else { '-' }
        $profiles = if ($item.ExplicitDevProfile -and $item.ExplicitProdProfile) {
            'dev + prod'
        } elseif ($item.ExplicitDevProfile) {
            'dev only'
        } elseif ($item.ExplicitProdProfile) {
            'prod only'
        } else {
            '-'
        }
        $container = if ($item.RootDevComposeService -and $item.RootProdComposeService) {
            'root dev + prod'
        } elseif ($item.RootDevComposeService) {
            'root dev only'
        } elseif ($item.RootProdComposeService) {
            'root prod only'
        } elseif ($item.ModuleDockerfile -and $item.ModuleCompose) {
            'legacy module Dockerfile + Compose'
        } elseif ($item.ModuleDockerfile) {
            'legacy module Dockerfile only'
        } elseif ($item.ModuleCompose) {
            'legacy module Compose only'
        } else {
            '-'
        }
        $bootApps = if ($item.BootJarDisabled) {
            "$($item.BootApplications) (bootJar disabled)"
        } else {
            [string]$item.BootApplications
        }
        $lines.Add("| ``$($item.Project)`` | $($item.Classification) | $bootApps | $h2 | $postgres | $profiles | $container |")
    }

    $lines -join "`n"
}

function Test-TaskContract {
    param([object[]]$Inventory)

    $runnable = @(
        $Inventory | Where-Object {
            $_.Classification -in @('api', 'batch', 'infra-server', 'standalone-server', 'cli')
        }
    )

    $taskArguments = [System.Collections.Generic.List[string]]::new()
    foreach ($item in $runnable) {
        $taskArguments.Add("$($item.Project):bootJar")
    }
    $taskArguments.Add('--dry-run')
    $taskArguments.Add('--offline')
    $taskArguments.Add('--console=plain')
    $taskArguments.Add('--no-daemon')

    $result = Invoke-ProcessCapture `
        -FilePath $gradleWrapper `
        -Arguments @($taskArguments) `
        -WorkingDirectory $repositoryRoot

    [pscustomobject]@{
        ProjectCount        = $runnable.Count
        DisabledBootJars    = @($runnable | Where-Object BootJarDisabled | Select-Object -ExpandProperty Project)
        ExitCode            = $result.ExitCode
        Command             = $result.Command
        Output              = ($result.StdOut + $result.StdErr).Trim()
        EvidenceLimit       = 'task-path/configuration only; SKIPPED dry-run is not package success'
    }
}

function Get-CommandEvidence {
    param(
        [string]$StandardOutput,
        [string]$StandardError
    )

    $combined = Protect-EvidenceText -Text (($StandardOutput + "`n" + $StandardError).Trim())
    if (-not $combined) {
        return ''
    }
    $lines = @($combined -split '\r?\n')
    $important = @(
        $lines | Where-Object {
            $_ -match '(^> Task .* (FAILED|SKIPPED)$)|(^BUILD (SUCCESSFUL|FAILED))|(^FAILURE:)|(\berror:)|(^Caused by:)|(^Description:)|(\brequired a bean of type\b)|(\bcould not be (found|registered)\b)|(\bNot a managed type\b)|(\bFailed to configure a DataSource\b)|(\bFailed to determine a suitable driver\b)'
        } | ForEach-Object {
            $line = $_.Trim()
            if ($line.Length -gt 800) {
                $line.Substring(0, 797) + '...'
            } else {
                $line
            }
        }
    )
    if ($important.Count -gt 0) {
        return ($important | Select-Object -Unique | Select-Object -Last 8) -join "`n"
    }
    ($lines | Select-Object -Last 12) -join "`n"
}

function Test-PackagingContract {
    param([object[]]$Inventory)

    $runnable = @(
        $Inventory | Where-Object {
            $_.Classification -in @('api', 'batch', 'infra-server', 'standalone-server', 'cli')
        }
    )

    $results = foreach ($item in $runnable) {
        $taskName = if ($item.BootJarDisabled) { 'compileJava' } else { 'bootJar' }
        $result = Invoke-ProcessCapture `
            -FilePath $gradleWrapper `
            -Arguments @(
                "$($item.Project):$taskName",
                '--offline',
                '--console=plain',
                '--max-workers=1',
                '--no-daemon'
            ) `
            -WorkingDirectory $repositoryRoot

        $projectDirectory = Get-ProjectDirectory -ProjectPath $item.Project
        $artifacts = @()
        if (-not $item.BootJarDisabled -and $result.ExitCode -eq 0) {
            $libraryDirectory = Join-Path $projectDirectory 'build\libs'
            if (Test-Path -LiteralPath $libraryDirectory -PathType Container) {
                $artifacts = @(
                    Get-ChildItem -LiteralPath $libraryDirectory -File -Filter '*.jar' |
                        Where-Object { $_.Name -notmatch '-plain\.jar$' } |
                        Sort-Object Name |
                        Select-Object -ExpandProperty FullName
                )
            }
        }

        [pscustomobject]@{
            Project         = $item.Project
            Classification  = $item.Classification
            Task            = $taskName
            BootJarDisabled = $item.BootJarDisabled
            ExitCode        = $result.ExitCode
            Status          = if ($item.BootJarDisabled -and $result.ExitCode -eq 0) {
                'NON_EXECUTABLE'
            } elseif ($item.BootJarDisabled) {
                'NON_EXECUTABLE_COMPILE_FAILED'
            } elseif ($result.ExitCode -eq 0 -and $artifacts.Count -eq 1) {
                'PASS'
            } elseif ($result.ExitCode -eq 0) {
                if ($artifacts.Count -eq 0) {
                    'FAIL_NO_EXECUTABLE_JAR'
                } else {
                    'FAIL_AMBIGUOUS_EXECUTABLE_JARS'
                }
            } else {
                'FAIL'
            }
            Artifacts       = $artifacts
            Command         = $result.Command
            Evidence        = if ($result.ExitCode -eq 0 -and $artifacts.Count -gt 1) {
                "Expected exactly one executable JAR, found $($artifacts.Count): $(@($artifacts | Split-Path -Leaf) -join ', ')"
            } else {
                Get-CommandEvidence `
                    -StandardOutput $result.StdOut `
                    -StandardError $result.StdErr
            }
        }
    }

    @($results)
}

function Test-LibraryContract {
    param([object[]]$Inventory)

    $projects = @(
        $Inventory | Where-Object {
            $_.Classification -in @('library', 'aggregator')
        }
    )

    $results = foreach ($item in $projects) {
        $result = Invoke-ProcessCapture `
            -FilePath $gradleWrapper `
            -Arguments @(
                "$($item.Project):test",
                "$($item.Project):jar",
                '--offline',
                '--console=plain',
                '--max-workers=1',
                '--no-daemon'
            ) `
            -WorkingDirectory $repositoryRoot

        [pscustomobject]@{
            Project  = $item.Project
            Role     = $item.Classification
            ExitCode = $result.ExitCode
            Status   = if ($result.ExitCode -eq 0) { 'PASS' } else { 'FAIL' }
            Command  = $result.Command
            Evidence = Get-CommandEvidence `
                -StandardOutput $result.StdOut `
                -StandardError $result.StdErr
        }
    }

    @($results)
}

function Test-LocalJarContract {
    param(
        [object[]]$PackagingResults,
        [int]$TimeoutSeconds,
        [bool]$ProfileOnly = $false
    )

    $javaCommand = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if ($null -eq $javaCommand) {
        return @(
            [pscustomobject]@{
                Project  = '*'
                Status   = 'BLOCKED'
                ExitCode = $null
                Command  = 'java.exe'
                Evidence = 'java.exe is unavailable'
            }
        )
    }

    $results = foreach ($package in $PackagingResults) {
        if ($package.Status -ne 'PASS') {
            [pscustomobject]@{
                Project        = $package.Project
                Classification = $package.Classification
                Status         = 'BLOCKED_BY_PACKAGING'
                ExitCode       = $null
                TimedOut       = $false
                Command        = $package.Command
                Evidence       = $package.Status
            }
            continue
        }

        $jarPath = @($package.Artifacts | Select-Object -First 1)
        if ($jarPath.Count -eq 0) {
            [pscustomobject]@{
                Project        = $package.Project
                Classification = $package.Classification
                Status         = 'BLOCKED_NO_JAR'
                ExitCode       = $null
                TimedOut       = $false
                Command        = ''
                Evidence       = 'Packaging reported PASS without an artifact path'
            }
            continue
        }

        $isolatedUserHome = Join-Path `
            ([System.IO.Path]::GetTempPath()) `
            "account-runtime-smoke-empty-home-$PID-$([System.Guid]::NewGuid().ToString('N'))"
        $arguments = @("-Duser.home=$isolatedUserHome", '-jar', $jarPath[0])
        if ($package.Classification -eq 'cli') {
            $arguments += '--list'
        } elseif ($package.Project -eq ':config-server') {
            $arguments += @(
                '--spring.profiles.active=native',
                '--server.port=0',
                '--spring.cloud.config.enabled=false'
            )
        } elseif ($package.Project -eq ':discovery') {
            $arguments += @(
                '--spring.profiles.active=local',
                '--server.port=0',
                '--spring.cloud.config.enabled=false',
                '--spring.config.on-not-found=ignore',
                '--eureka.client.register-with-eureka=false',
                '--eureka.client.fetch-registry=false',
                '--management.tracing.enabled=false'
            )
        } elseif ($package.Project -eq ':gateway') {
            $arguments += @(
                '--spring.profiles.active=local',
                '--spring.main.web-application-type=reactive',
                '--server.port=0',
                '--spring.cloud.config.enabled=false',
                '--spring.config.on-not-found=ignore',
                '--spring.cloud.discovery.enabled=false',
                '--spring.cloud.loadbalancer.enabled=false',
                '--eureka.client.enabled=false',
                '--auth.token-version-validation.enabled=false',
                '--management.tracing.enabled=false'
            )
        } else {
            $arguments += @(
                '--spring.profiles.active=local',
                '--server.port=0',
                '--spring.batch.job.enabled=false'
            )
            if (-not $ProfileOnly) {
                $webApplicationType = if ($package.Classification -eq 'api') {
                    'servlet'
                } else {
                    'none'
                }
                $arguments += @(
                    "--spring.main.web-application-type=$webApplicationType",
                    '--spring.cloud.config.enabled=false',
                    '--spring.config.on-not-found=ignore',
                    '--spring.cloud.discovery.enabled=false',
                    '--spring.cloud.loadbalancer.enabled=false',
                    '--spring.cloud.vault.enabled=false',
                    '--eureka.client.enabled=false',
                    '--management.tracing.enabled=false'
                )
                if ($package.Project -in @(
                        ':budget:api',
                        ':budget:batch',
                        ':internal-audit:api')) {
                    $databaseName = $package.Project.TrimStart(':').Replace(':', '_').Replace('-', '_')
                    $arguments += @(
                        '--spring.batch.jdbc.initialize-schema=always',
                        "--spring.datasource.url=jdbc:h2:mem:runtime_smoke_$databaseName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        '--spring.datasource.driver-class-name=org.h2.Driver',
                        '--spring.datasource.username=sa',
                        '--spring.datasource.password=',
                        '--spring.jpa.hibernate.ddl-auto=validate',
                        '--spring.flyway.enabled=true'
                    )
                } else {
                    $arguments += @(
                        '--spring.batch.jdbc.initialize-schema=always',
                        '--spring.datasource.url=jdbc:h2:mem:runtime_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                        '--spring.datasource.driver-class-name=org.h2.Driver',
                        '--spring.datasource.username=sa',
                        '--spring.datasource.password=',
                        '--spring.jpa.hibernate.ddl-auto=create-drop',
                        '--spring.flyway.enabled=false'
                    )
                }
            }
        }
        if (-not $ProfileOnly) {
            if ($package.Project -in @(':deposit:api', ':deposit:batch')) {
                $arguments += '--account.deposit.local-adapters.enabled=true'
            }
            if ($package.Project -in @(':reporting:api', ':reporting:batch')) {
                $arguments += '--account.reporting.persistence.mode=memory'
            }
            if ($package.Project -eq ':budget:api') {
                $arguments += '--auth.jwt.secret=runtime-smoke-budget-test-key-32-bytes-minimum'
            }
        }
        $result = Invoke-ProcessCaptureWithTimeout `
            -FilePath $javaCommand.Source `
            -Arguments $arguments `
            -WorkingDirectory $repositoryRoot `
            -TimeoutSeconds $TimeoutSeconds `
            -EnvironmentVariableNamesToKeep $isolatedRuntimeEnvironmentNameAllowlist

        $combined = ($result.StdOut + "`n" + $result.StdErr).Trim()
        $started = $combined -match '(?m)^.*Started .* in [0-9.]+ seconds'
        $cliSucceeded = $package.Classification -eq 'cli' -and
            $result.ExitCode -eq 0 -and
            $combined -match '(?m)^\S+\s+(READY|BLOCKED)\s*$'
        [pscustomobject]@{
            Project        = $package.Project
            Classification = $package.Classification
            Status         = if ($result.TerminationError) {
                'FAIL_PROCESS_TREE_TERMINATION'
            } elseif ($cliSucceeded) {
                'PASS_EXITED'
            } elseif ($result.ExitCode -eq 0 -and $started) {
                'PASS_EXITED'
            } elseif ($result.ExitCode -eq 0) {
                'FAIL_NO_START_MARKER'
            } elseif ($result.TimedOut -and $started) {
                'PASS_STARTED'
            } elseif ($result.TimedOut) {
                'FAIL_TIMEOUT'
            } else {
                'FAIL'
            }
            ExitCode       = $result.ExitCode
            TimedOut       = $result.TimedOut
            EnvironmentVariablesRemoved = @($result.EnvironmentVariablesRemoved)
            Command        = Protect-EvidenceText -Text $result.Command
            Evidence       = Get-CommandEvidence `
                -StandardOutput $result.StdOut `
                -StandardError $result.StdErr
        }
    }

    @($results)
}

function Test-FrontendContract {
    $packageJson = Join-Path $frontendRoot 'package.json'
    $lockFile = Join-Path $frontendRoot 'package-lock.json'
    $nodeModules = Join-Path $frontendRoot 'node_modules'
    $npmCommand = Get-Command 'npm.cmd' -ErrorAction SilentlyContinue

    $scripts = @()
    if (Test-Path -LiteralPath $packageJson -PathType Leaf) {
        $package = Get-Content -LiteralPath $packageJson -Raw | ConvertFrom-Json
        $scripts = @($package.scripts.PSObject.Properties.Name)
    }

    [pscustomobject]@{
        Status            = if (Test-Path -LiteralPath $nodeModules -PathType Container) {
            'READY'
        } else {
            'BLOCKED'
        }
        PackageJson       = Test-Path -LiteralPath $packageJson -PathType Leaf
        LockFile          = Test-Path -LiteralPath $lockFile -PathType Leaf
        NpmAvailable      = $null -ne $npmCommand
        NodeModules       = Test-Path -LiteralPath $nodeModules -PathType Container
        HasDevScript      = $scripts -contains 'dev'
        HasBuildScript    = $scripts -contains 'build'
        HasStartScript    = $scripts -contains 'start'
        InstallAuthorized = $false
        BuildStatus       = if (Test-Path -LiteralPath $nodeModules -PathType Container) {
            'ready-to-run'
        } else {
            'blocked: node_modules absent and this tool does not install packages'
        }
    }
}

$runtimeInventory = $null
$packagingResults = $null
$report = [ordered]@{}

if ($Mode -in @('Inventory', 'TaskContract', 'Packaging', 'Libraries', 'LocalJar', 'ProfileJar', 'All')) {
    $runtimeInventory = Get-RuntimeInventory
    if ($projectFilter.Count -gt 0) {
        $runtimeInventory = @(
            $runtimeInventory | Where-Object { $projectFilter -contains $_.Project }
        )
        if ($runtimeInventory.Count -eq 0) {
            throw "No inventory projects matched -Project: $($projectFilter -join ', ')"
        }
    }
}
if ($Mode -in @('Inventory', 'All')) {
    $report.Inventory = @($runtimeInventory)
    $report.InventoryMarkdown = Convert-InventoryToMarkdown -Inventory $runtimeInventory
}
if ($Mode -in @('TaskContract', 'All')) {
    $report.TaskContract = Test-TaskContract -Inventory $runtimeInventory
}
if ($Mode -in @('LocalJar', 'ProfileJar') -and $PackagingResultPath) {
    $resolvedPackagingResult = if ([System.IO.Path]::IsPathRooted($PackagingResultPath)) {
        [System.IO.Path]::GetFullPath($PackagingResultPath)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $PackagingResultPath))
    }
    $packagingReport = Get-Content -LiteralPath $resolvedPackagingResult -Raw | ConvertFrom-Json
    $packagingResults = @($packagingReport.Packaging)
    if ($projectFilter.Count -gt 0) {
        $packagingResults = @(
            $packagingResults | Where-Object { $projectFilter -contains $_.Project }
        )
    }
    if ($packagingResults.Count -eq 0) {
        throw "Packaging result contains no Packaging entries: $resolvedPackagingResult"
    }
} elseif ($Mode -in @('Packaging', 'LocalJar', 'ProfileJar', 'All')) {
    $packagingResults = Test-PackagingContract -Inventory $runtimeInventory
}
if ($Mode -in @('Packaging', 'All')) {
    $report.Packaging = @($packagingResults)
}
if ($Mode -in @('Libraries', 'All')) {
    $report.Libraries = @(Test-LibraryContract -Inventory $runtimeInventory)
}
if ($Mode -in @('LocalJar', 'All')) {
    $report.LocalJar = @(
        Test-LocalJarContract `
            -PackagingResults $packagingResults `
            -TimeoutSeconds $StartupTimeoutSeconds
    )
}
if ($Mode -in @('ProfileJar', 'All')) {
    $report.ProfileJar = @(
        Test-LocalJarContract `
            -PackagingResults $packagingResults `
            -TimeoutSeconds $StartupTimeoutSeconds `
            -ProfileOnly $true
    )
}
if ($Mode -in @('Frontend', 'All')) {
    $report.Frontend = Test-FrontendContract
}

$json = $report | ConvertTo-Json -Depth 8
if ($OutputPath) {
    $resolvedOutput = if ([System.IO.Path]::IsPathRooted($OutputPath)) {
        [System.IO.Path]::GetFullPath($OutputPath)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputPath))
    }
    [System.IO.File]::WriteAllText($resolvedOutput, $json, [System.Text.UTF8Encoding]::new($false))
    Write-Output "Wrote runtime smoke result: $resolvedOutput"
} else {
    Write-Output $json
}

$hasFailure = $false
if ($report.Contains('TaskContract') -and $report.TaskContract.ExitCode -ne 0) {
    $hasFailure = $true
}
foreach ($resultGroupName in @('Packaging', 'Libraries', 'LocalJar', 'ProfileJar')) {
    if ($report.Contains($resultGroupName) -and
        @($report[$resultGroupName] | Where-Object {
                $_.Status -match '^(FAIL|BLOCKED|NON_EXECUTABLE)'
            }).Count -gt 0) {
        $hasFailure = $true
    }
}
if ($report.Contains('Frontend') -and $report.Frontend.Status -eq 'BLOCKED') {
    $hasFailure = $true
}
if ($hasFailure) {
    exit 1
}
