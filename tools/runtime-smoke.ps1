[CmdletBinding()]
param(
    [ValidateSet('Inventory', 'TaskContract', 'Packaging', 'Libraries', 'LocalJar', 'Frontend', 'All')]
    [string]$Mode = 'All',

    [string]$OutputPath,

    [string]$PackagingResultPath,

    [string[]]$Project,

    [ValidateRange(5, 180)]
    [int]$StartupTimeoutSeconds = 30
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'
$frontendRoot = Join-Path $repositoryRoot 'frontend'
$projectFilter = @(
    $Project |
        ForEach-Object { $_ -split ',' } |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ }
)

function Invoke-ProcessCapture {
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
    $standardOutput = $process.StandardOutput.ReadToEnd()
    $standardError = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    [pscustomobject]@{
        ExitCode = $process.ExitCode
        StdOut   = $standardOutput
        StdErr   = $standardError
        Command  = "$FilePath $($Arguments -join ' ')"
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
        [int]$TimeoutSeconds
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
    $completed = $process.WaitForExit($TimeoutSeconds * 1000)

    if (-not $completed) {
        $process.Kill()
    }
    $process.WaitForExit()

    [pscustomobject]@{
        ExitCode = if ($completed) { $process.ExitCode } else { $null }
        StdOut   = $standardOutputTask.Result
        StdErr   = $standardErrorTask.Result
        Command  = "$FilePath $($Arguments -join ' ')"
        TimedOut = -not $completed
    }
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

        $classification = Get-ProjectClassification `
            -ProjectPath $projectPath `
            -HasBootApplication ($applicationFiles.Count -gt 0)

        $topLevelModule = $projectPath.TrimStart(':').Split(':')[0]
        $moduleDirectory = Join-Path $repositoryRoot $topLevelModule
        $dockerfile = Join-Path $moduleDirectory 'Dockerfile'
        $composeFile = Join-Path $moduleDirectory 'docker-compose.yml'

        [pscustomobject]@{
            Project              = $projectPath
            Classification       = $classification
            DirectoryExists      = Test-Path -LiteralPath $projectDirectory -PathType Container
            BuildFile            = Test-Path -LiteralPath $buildFile -PathType Leaf
            BootApplications     = $applicationFiles.Count
            BootApplicationNames = @($applicationFiles | ForEach-Object { $_.BaseName }) -join ', '
            BootJarDisabled      = $buildText -match '(?s)bootJar\s*\{[^}]*enabled\s*=\s*false'
            H2Dependency         = $buildText -match 'com\.h2database:h2'
            PostgreSqlDependency = $buildText -match 'org\.postgresql:postgresql'
            ExplicitH2Config     = $resourceText -match 'jdbc:h2:'
            ExplicitLocalProfile = $resourceText -match '(?m)^\s*(default|active):\s*local\s*$'
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
        $h2 = if ($item.H2Dependency -and $item.ExplicitH2Config) {
            'dependency + config'
        } elseif ($item.H2Dependency) {
            'dependency only'
        } elseif ($item.ExplicitH2Config) {
            'config only'
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
        $container = if ($item.ModuleDockerfile -and $item.ModuleCompose) {
            'Dockerfile + Compose'
        } elseif ($item.ModuleDockerfile) {
            'Dockerfile only'
        } elseif ($item.ModuleCompose) {
            'Compose only'
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
            $_.Classification -in @('api', 'batch', 'infra-server', 'standalone-server')
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

    $combined = ($StandardOutput + "`n" + $StandardError).Trim()
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
            $_.Classification -in @('api', 'batch', 'infra-server', 'standalone-server')
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
            } elseif ($result.ExitCode -eq 0 -and $artifacts.Count -gt 0) {
                'PASS'
            } elseif ($result.ExitCode -eq 0) {
                'FAIL_NO_EXECUTABLE_JAR'
            } else {
                'FAIL'
            }
            Artifacts       = $artifacts
            Command         = $result.Command
            Evidence        = Get-CommandEvidence `
                -StandardOutput $result.StdOut `
                -StandardError $result.StdErr
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
        [int]$TimeoutSeconds
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

        $arguments = @('-jar', $jarPath[0])
        if ($package.Project -eq ':config-server') {
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
            $webApplicationType = if ($package.Project -eq ':budget:api') { 'servlet' } else { 'none' }
            $arguments += @(
                '--spring.profiles.active=local',
                "--spring.main.web-application-type=$webApplicationType",
                '--server.port=0',
                '--spring.cloud.config.enabled=false',
                '--spring.config.on-not-found=ignore',
                '--spring.cloud.discovery.enabled=false',
                '--spring.cloud.loadbalancer.enabled=false',
                '--spring.cloud.vault.enabled=false',
                '--eureka.client.enabled=false',
                '--management.tracing.enabled=false',
                '--spring.batch.job.enabled=false'
            )
            if ($package.Project -in @(':budget:api', ':budget:batch')) {
                $budgetDatabaseName = $package.Project.TrimStart(':').Replace(':', '_')
                $arguments += @(
                    '--spring.batch.jdbc.initialize-schema=always',
                    "--spring.datasource.url=jdbc:h2:mem:runtime_smoke_$budgetDatabaseName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        if ($package.Project -in @(':deposit:api', ':deposit:batch')) {
            $arguments += '--account.deposit.local-adapters.enabled=true'
        }
        if ($package.Project -in @(':reporting:api', ':reporting:batch')) {
            $arguments += '--account.reporting.persistence.mode=memory'
        }
        if ($package.Project -eq ':budget:api') {
            $arguments += '--auth.jwt.secret=runtime-smoke-budget-test-key-32-bytes-minimum'
        }
        $result = Invoke-ProcessCaptureWithTimeout `
            -FilePath $javaCommand.Source `
            -Arguments $arguments `
            -WorkingDirectory $repositoryRoot `
            -TimeoutSeconds $TimeoutSeconds

        $combined = ($result.StdOut + "`n" + $result.StdErr).Trim()
        $started = $combined -match '(?m)^.*Started .* in [0-9.]+ seconds'
        [pscustomobject]@{
            Project        = $package.Project
            Classification = $package.Classification
            Status         = if ($result.ExitCode -eq 0 -and $started) {
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
            Command        = $result.Command
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

if ($Mode -in @('Inventory', 'TaskContract', 'Packaging', 'Libraries', 'LocalJar', 'All')) {
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
    $report.Inventory = $runtimeInventory
    $report.InventoryMarkdown = Convert-InventoryToMarkdown -Inventory $runtimeInventory
}
if ($Mode -in @('TaskContract', 'All')) {
    $report.TaskContract = Test-TaskContract -Inventory $runtimeInventory
}
if ($Mode -eq 'LocalJar' -and $PackagingResultPath) {
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
} elseif ($Mode -in @('Packaging', 'LocalJar', 'All')) {
    $packagingResults = Test-PackagingContract -Inventory $runtimeInventory
}
if ($Mode -in @('Packaging', 'All')) {
    $report.Packaging = $packagingResults
}
if ($Mode -in @('Libraries', 'All')) {
    $report.Libraries = Test-LibraryContract -Inventory $runtimeInventory
}
if ($Mode -in @('LocalJar', 'All')) {
    $report.LocalJar = Test-LocalJarContract `
        -PackagingResults $packagingResults `
        -TimeoutSeconds $StartupTimeoutSeconds
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
