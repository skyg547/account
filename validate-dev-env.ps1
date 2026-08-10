[CmdletBinding()]
param(
    [ValidateSet('SelfContained', 'ExternalDev')]
    [string]$Mode = 'SelfContained',
    [string]$EnvFile,
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'

$contextPrefixes = @(
    'ACCOUNT_MART',
    'ASSET_LEASE',
    'AUTH',
    'BUDGET',
    'CLOSING',
    'DEPOSIT',
    'ECL',
    'EXPENDITURE_RESOLUTION',
    'INTERNAL_AUDIT',
    'JOURNAL_LEDGER',
    'LOAN',
    'MASTER_DATA',
    'PAYABLE',
    'RECEIVABLE',
    'RECONCILIATION',
    'REPORTING',
    'TAX'
)

function Read-EnvironmentFile {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Development environment file was not found."
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) {
            continue
        }
        $parts = $trimmed.Split('=', 2)
        if ($parts.Count -ne 2 -or [string]::IsNullOrWhiteSpace($parts[0])) {
            throw "Development environment file contains an invalid assignment."
        }
        if ($values.ContainsKey($parts[0])) {
            throw "Development environment file contains a duplicate variable name: $($parts[0])"
        }
        $values[$parts[0]] = $parts[1]
    }
    return $values
}

function Test-DevelopmentValues {
    param(
        [Parameter(Mandatory)][hashtable]$Values,
        [Parameter(Mandatory)][string]$ValidationMode
    )

    $errors = [System.Collections.Generic.List[string]]::new()
    $required = [System.Collections.Generic.List[string]]::new()
    $required.AddRange([string[]]@(
        'AUTH_JWT_SECRET',
        'AUTH_DEFAULT_PASSWORD',
        'AUTH_INTERNAL_API_TOKEN',
        'DEV_KAFKA_BOOTSTRAP_SERVERS',
        'DEV_REDIS_HOST',
        'DEV_REDIS_PORT'
    ))
    foreach ($prefix in $contextPrefixes) {
        $required.Add("${prefix}_DB_URL")
        $required.Add("${prefix}_DB_USER")
        $required.Add("${prefix}_DB_PASSWORD")
    }

    if ($ValidationMode -eq 'SelfContained') {
        $required.AddRange([string[]]@(
            'POSTGRES_ADMIN_PASSWORD',
            'ACCOUNT_DB_OWNER_PASSWORD',
            'ACCOUNT_DB_APP_PASSWORD',
            'ACCOUNT_DATABASES'
        ))
    }
    else {
        $required.AddRange([string[]]@(
            'DEV_DB_HOST',
            'DEV_DB_NAME',
            'DEV_DB_USER',
            'DEV_DB_PASSWORD'
        ))
    }

    foreach ($name in $required) {
        if (-not $Values.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($Values[$name])) {
            $errors.Add("Missing required variable: $name")
            continue
        }
        if ($Values[$name] -match '(?i)replace-with-|example\.invalid') {
            $errors.Add("Placeholder value is not allowed: $name")
        }
    }

    foreach ($prefix in $contextPrefixes) {
        $urlName = "${prefix}_DB_URL"
        $userName = "${prefix}_DB_USER"
        if (-not $Values.ContainsKey($urlName) -or -not $Values.ContainsKey($userName)) {
            continue
        }
        $url = $Values[$urlName]
        $urlMatch = [regex]::Match($url, '^jdbc:postgresql://[^/@]+/([a-z0-9_]+)(?:\?.*)?$')
        if (-not $urlMatch.Success) {
            $errors.Add("PostgreSQL JDBC URL is invalid: $urlName")
            continue
        }
        if ($url -match '(?i)[?&](?:user|password)=') {
            $errors.Add("Database credentials must not be embedded in URL: $urlName")
        }
        $expectedDatabase = "$($prefix.ToLowerInvariant())_dev"
        if ($urlMatch.Groups[1].Value -ne $expectedDatabase) {
            $errors.Add("Development database name must be $expectedDatabase`: $urlName")
        }
        if ($Values[$userName] -ne "${expectedDatabase}_app") {
            $errors.Add("Runtime database role must be ${expectedDatabase}_app`: $userName")
        }
        if ($ValidationMode -eq 'SelfContained' -and $url -notmatch '^jdbc:postgresql://postgres-db:5432/') {
            $errors.Add("Self-contained database URL must use postgres-db: $urlName")
        }
        if ($ValidationMode -eq 'ExternalDev' -and $url -match '^jdbc:postgresql://(?:postgres-db|localhost|127\.0\.0\.1|0\.0\.0\.0)(?::|/)') {
            $errors.Add("External development database URL uses a local shortcut: $urlName")
        }
    }

    if ($ValidationMode -eq 'SelfContained' -and $Values.ContainsKey('ACCOUNT_DATABASES')) {
        $expectedDatabases = @($contextPrefixes | ForEach-Object { "$($_.ToLowerInvariant())_dev" })
        $actualDatabases = @($Values['ACCOUNT_DATABASES'].Split(',') | ForEach-Object { $_.Trim() })
        if ($actualDatabases.Count -ne $expectedDatabases.Count -or
            (Compare-Object -ReferenceObject $expectedDatabases -DifferenceObject $actualDatabases).Count -ne 0) {
            $errors.Add('ACCOUNT_DATABASES must contain every development bounded-context database exactly once')
        }
    }
    if ($ValidationMode -eq 'SelfContained' -and $Values.ContainsKey('ACCOUNT_DB_APP_PASSWORD')) {
        foreach ($prefix in $contextPrefixes) {
            $passwordName = "${prefix}_DB_PASSWORD"
            if ($Values.ContainsKey($passwordName) -and
                $Values[$passwordName] -ne $Values['ACCOUNT_DB_APP_PASSWORD']) {
                $errors.Add("Self-contained runtime password must match ACCOUNT_DB_APP_PASSWORD: $passwordName")
            }
        }
    }
    if ($ValidationMode -eq 'ExternalDev') {
        if ($Values.ContainsKey('DEV_DB_HOST') -and
            $Values['DEV_DB_HOST'] -match '^(?i:postgres-db|localhost|127\.0\.0\.1|0\.0\.0\.0)$') {
            $errors.Add('Focused external development probe cannot use a local database host')
        }
        if ($Values.ContainsKey('DEV_DB_NAME')) {
            $expectedDatabases = @($contextPrefixes | ForEach-Object { "$($_.ToLowerInvariant())_dev" })
            if ($Values['DEV_DB_NAME'] -notin $expectedDatabases) {
                $errors.Add('Focused external development probe must select a bounded-context database')
            }
        }
        if ($Values.ContainsKey('DEV_DB_USER') -and $Values['DEV_DB_USER'] -notmatch '_app$') {
            $errors.Add('Focused external development probe must use a runtime role')
        }
    }

    if ($Values.ContainsKey('AUTH_JWT_SECRET') -and $Values['AUTH_JWT_SECRET'].Length -lt 32) {
        $errors.Add('AUTH_JWT_SECRET must be at least 32 characters')
    }
    if ($Values.ContainsKey('AUTH_INTERNAL_API_TOKEN') -and $Values['AUTH_INTERNAL_API_TOKEN'].Length -lt 32) {
        $errors.Add('AUTH_INTERNAL_API_TOKEN must be at least 32 characters')
    }
    if ($ValidationMode -eq 'SelfContained' -and
        $Values.ContainsKey('ACCOUNT_DB_OWNER_PASSWORD') -and
        $Values.ContainsKey('ACCOUNT_DB_APP_PASSWORD') -and
        $Values['ACCOUNT_DB_OWNER_PASSWORD'] -eq $Values['ACCOUNT_DB_APP_PASSWORD']) {
        $errors.Add('Development owner and runtime passwords must differ')
    }

    return $errors
}

function Invoke-SelfTest {
    $valid = @{
        AUTH_JWT_SECRET = 'j' * 32
        AUTH_DEFAULT_PASSWORD = 'local-auth-password'
        AUTH_INTERNAL_API_TOKEN = 't' * 32
        DEV_KAFKA_BOOTSTRAP_SERVERS = 'kafka:9092'
        DEV_REDIS_HOST = 'redis'
        DEV_REDIS_PORT = '6379'
        POSTGRES_ADMIN_PASSWORD = 'admin-secret-value'
        ACCOUNT_DB_OWNER_PASSWORD = 'owner-secret-value'
        ACCOUNT_DB_APP_PASSWORD = 'runtime-secret-value'
        ACCOUNT_DATABASES = ($contextPrefixes | ForEach-Object { "$($_.ToLowerInvariant())_dev" }) -join ','
    }
    foreach ($prefix in $contextPrefixes) {
        $database = $prefix.ToLowerInvariant()
        $valid["${prefix}_DB_URL"] = "jdbc:postgresql://postgres-db:5432/${database}_dev"
        $valid["${prefix}_DB_USER"] = "${database}_dev_app"
        $valid["${prefix}_DB_PASSWORD"] = 'runtime-secret-value'
    }
    if ((Test-DevelopmentValues -Values $valid -ValidationMode 'SelfContained').Count -ne 0) {
        throw 'Self-contained positive fixture failed.'
    }

    $invalid = $valid.Clone()
    $invalid['AUTH_JWT_SECRET'] = 'short'
    $invalid['ACCOUNT_DB_APP_PASSWORD'] = $invalid['ACCOUNT_DB_OWNER_PASSWORD']
    $invalid['AUTH_DB_PASSWORD'] = 'mismatched-runtime-secret'
    if ((Test-DevelopmentValues -Values $invalid -ValidationMode 'SelfContained').Count -lt 3) {
        throw 'Negative security fixtures were not rejected.'
    }

    $external = $valid.Clone()
    $external['DEV_DB_HOST'] = 'shared-db.internal'
    $external['DEV_DB_NAME'] = 'auth_dev'
    $external['DEV_DB_USER'] = 'auth_dev_app'
    $external['DEV_DB_PASSWORD'] = 'shared-runtime-secret'
    foreach ($prefix in $contextPrefixes) {
        $database = $prefix.ToLowerInvariant()
        $external["${prefix}_DB_URL"] = "jdbc:postgresql://shared-db.internal:5432/${database}_dev?sslmode=require"
    }
    if ((Test-DevelopmentValues -Values $external -ValidationMode 'ExternalDev').Count -ne 0) {
        throw 'External development positive fixture failed.'
    }

    $invalidExternal = $external.Clone()
    $invalidExternal['DEV_DB_HOST'] = 'localhost'
    $invalidExternal['DEV_DB_USER'] = 'auth_dev_owner'
    if ((Test-DevelopmentValues -Values $invalidExternal -ValidationMode 'ExternalDev').Count -lt 2) {
        throw 'External development negative fixtures were not rejected.'
    }
    Write-Output 'Development environment validator self-test PASS'
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    throw 'EnvFile is required unless SelfTest is selected.'
}

$environmentValues = Read-EnvironmentFile -Path $EnvFile
$validationErrors = Test-DevelopmentValues -Values $environmentValues -ValidationMode $Mode
if ($validationErrors.Count -gt 0) {
    foreach ($validationError in $validationErrors) {
        Write-Error $validationError
    }
    throw "Development environment validation failed with $($validationErrors.Count) error(s)."
}

Write-Output "Development environment validation PASS ($Mode, $($environmentValues.Count) variables)"
