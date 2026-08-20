[CmdletBinding()]
param(
    [string]$EnvFile,

    [switch]$Template,

    [switch]$SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function ConvertFrom-ComposeEnvValue {
    param(
        [Parameter(Mandatory)]
        [AllowEmptyString()]
        [string]$RawValue,

        [Parameter(Mandatory)]
        [int]$LineNumber
    )

    if ($RawValue.Contains('\')) {
        throw "Backslash escape sequences are forbidden at line $LineNumber."
    }

    if ($RawValue.StartsWith("'")) {
        if ($RawValue.Length -lt 2 -or -not $RawValue.EndsWith("'")) {
            throw "Unterminated single-quoted value at line $LineNumber."
        }
        return $RawValue.Substring(1, $RawValue.Length - 2)
    }

    if ($RawValue.StartsWith('"')) {
        if ($RawValue.Length -lt 2 -or -not $RawValue.EndsWith('"')) {
            throw "Unterminated double-quoted value at line $LineNumber."
        }
        if ($RawValue.Contains('$')) {
            throw "Interpolated double-quoted values are forbidden at line $LineNumber. Use a single-quoted literal secret."
        }
        return $RawValue.Substring(1, $RawValue.Length - 2)
    }

    if ($RawValue.Contains('$')) {
        throw "Interpolated unquoted values are forbidden at line $LineNumber. Use a single-quoted literal secret."
    }

    # Compose treats a hash preceded by whitespace as an inline comment.
    # Validate the effective value, not comment padding that could make a
    # weak password appear long enough.
    return [regex]::Replace($RawValue, '\s+#.*$', '').TrimEnd()
}

function Assert-ProductionPostgreSqlUrl {
    param(
        [Parameter(Mandatory)]
        [string]$Url,

        [switch]$AllowTemplateHost
    )

    $match = [regex]::Match(
        $Url,
        '^jdbc:postgresql://(?<authority>[^/?#]+)/(?<database>[A-Za-z0-9_-]+)\?(?<query>[^#]+)$',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )
    if (-not $match.Success) {
        throw 'Every production database URL must be a PostgreSQL JDBC URL with an explicit query.'
    }

    $authority = $match.Groups['authority'].Value
    if ($authority.Contains('@')) {
        throw 'Production database URLs must not embed user information.'
    }
    if ($authority -match '(?i)(localhost|127\.0\.0\.1|host\.docker\.internal)') {
        throw 'Production database URLs must not use a local-container shortcut.'
    }
    if (-not $AllowTemplateHost -and $authority -match '(?i)\.example\.invalid(?::\d+)?$') {
        throw 'Production database URLs must not use a documentation-only host.'
    }

    $query = @{}
    foreach ($segment in $match.Groups['query'].Value.Split('&')) {
        $pair = $segment.Split('=', 2)
        if ($pair.Count -ne 2 -or -not $pair[0]) {
            throw 'Production database URL contains an invalid query parameter.'
        }
        $key = $pair[0].ToLowerInvariant()
        if ($query.ContainsKey($key)) {
            throw 'Production database URL contains a duplicate query parameter.'
        }
        $query[$key] = $pair[1]
    }

    if (-not $query.ContainsKey('sslmode') -or $query['sslmode'] -cne 'verify-full') {
        throw 'Every production database URL must contain exactly one sslmode=verify-full parameter.'
    }
    foreach ($forbiddenKey in @('user', 'password', 'sslfactory', 'sslfactoryarg')) {
        if ($query.ContainsKey($forbiddenKey)) {
            throw 'Production database URL contains a forbidden credential or TLS override parameter.'
        }
    }
}

function Assert-Throws {
    param(
        [Parameter(Mandatory)]
        [scriptblock]$Action
    )

    try {
        & $Action
    } catch {
        return
    }
    throw 'Validator self-test expected a rejection.'
}

function Test-IsMissingRequiredValue {
    param(
        [Parameter(Mandatory)]
        [string]$Name,

        [AllowNull()]
        [AllowEmptyString()]
        [string]$Value,

        [switch]$AllowTemplateEmpty
    )

    if (-not [string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }

    if ($AllowTemplateEmpty -and (
            $Name -eq 'ENCRYPT_KEY' -or
            $Name -like '*_DB_PASSWORD' -or
            $Name -like '*_JWT_SECRET' -or
            $Name -eq 'AUTH_DEFAULT_PASSWORD' -or
            $Name -eq 'AUTH_INTERNAL_API_TOKEN'
        )) {
        return $false
    }

    return $true
}

if ($SelfTest) {
    if ((ConvertFrom-ComposeEnvValue -RawValue 'password # this text is not part of the value' -LineNumber 1) -ne 'password') {
        throw 'Validator self-test failed to apply Compose inline-comment semantics.'
    }
    Assert-Throws { ConvertFrom-ComposeEnvValue -RawValue '"password${UNSET_LONG_NAME}"' -LineNumber 2 }
    Assert-Throws { ConvertFrom-ComposeEnvValue -RawValue '"\t\t\t\t\t\t\t\t"' -LineNumber 3 }
    Assert-ProductionPostgreSqlUrl -Url 'jdbc:postgresql://db.example.invalid:5432/account?sslmode=verify-full' -AllowTemplateHost
    Assert-Throws {
        Assert-ProductionPostgreSqlUrl -Url 'jdbc:postgresql://db.example.invalid:5432/account?sslmode=verify-full&sslmode=disable' -AllowTemplateHost
    }
    Assert-Throws {
        Assert-ProductionPostgreSqlUrl -Url 'jdbc:postgresql://user@db.example.invalid:5432/account?sslmode=verify-full' -AllowTemplateHost
    }
    Assert-Throws {
        Assert-ProductionPostgreSqlUrl -Url 'jdbc:postgresql://db.example.invalid:5432/account?sslmode=verify-full&password=hidden' -AllowTemplateHost
    }
    if (-not (Test-IsMissingRequiredValue -Name 'ENCRYPT_KEY' -Value '   ')) {
        throw 'Validator self-test failed to reject a whitespace-only production ENCRYPT_KEY.'
    }
    if (Test-IsMissingRequiredValue -Name 'ENCRYPT_KEY' -Value '' -AllowTemplateEmpty) {
        throw 'Validator self-test failed to allow the blank ENCRYPT_KEY contract in template mode.'
    }
    [pscustomobject]@{
        Status = 'PASS'
        Mode   = 'SelfTest'
    }
    return
}

if (-not $EnvFile) {
    throw 'EnvFile is required unless SelfTest is selected.'
}

$repositoryRoot = $PSScriptRoot
$composePath = Join-Path $repositoryRoot 'compose.prod.yml'
$resolvedEnvFile = [System.IO.Path]::GetFullPath($EnvFile)

if (-not (Test-Path -LiteralPath $resolvedEnvFile -PathType Leaf)) {
    throw 'Production environment file was not found.'
}
if (-not $Template -and $resolvedEnvFile.EndsWith('.example', [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Refusing to validate an example file as a production environment.'
}

$values = @{}
$lineNumber = 0
foreach ($line in Get-Content -LiteralPath $resolvedEnvFile) {
    $lineNumber++
    $trimmed = $line.Trim()
    if (-not $trimmed -or $trimmed.StartsWith('#')) {
        continue
    }

    $separator = $line.IndexOf('=')
    if ($separator -lt 1) {
        throw "Invalid environment entry at line $lineNumber."
    }

    $name = $line.Substring(0, $separator).Trim()
    $rawValue = $line.Substring($separator + 1).Trim()
    if ($name -notmatch '^[A-Z][A-Z0-9_]*$') {
        throw "Invalid environment variable name at line $lineNumber."
    }
    if ($values.ContainsKey($name)) {
        throw "Duplicate environment variable name at line $lineNumber."
    }

    $value = ConvertFrom-ComposeEnvValue -RawValue $rawValue -LineNumber $lineNumber
    $values[$name] = $value
}

$composeText = Get-Content -LiteralPath $composePath -Raw
$requiredNames = @(
    [regex]::Matches($composeText, '\$\{([A-Z][A-Z0-9_]*):\?[^}]+\}') |
        ForEach-Object { $_.Groups[1].Value } |
        Sort-Object -Unique
)
$interpolatedNames = @(
    [regex]::Matches($composeText, '\$\{([A-Z][A-Z0-9_]*)(?::[-?])') |
        ForEach-Object { $_.Groups[1].Value } |
        Sort-Object -Unique
)

$missingNames = @($requiredNames | Where-Object { -not $values.ContainsKey($_) })
if ($missingNames.Count -gt 0) {
    throw "Missing $($missingNames.Count) required production environment variable(s)."
}

if (-not $Template) {
    $hostOverrideNames = @(
        $interpolatedNames |
            Where-Object {
                $null -ne [Environment]::GetEnvironmentVariable($_, 'Process')
            }
    )
    if ($hostOverrideNames.Count -gt 0) {
        throw "Host environment overrides $($hostOverrideNames.Count) required production variable(s). Clear them before validation and Compose execution."
    }
}

if (-not $values.ContainsKey('PROD_BATCH_JOB_ENABLED') -or $values['PROD_BATCH_JOB_ENABLED'] -cne 'false') {
    throw 'PROD_BATCH_JOB_ENABLED must be exactly false; approved one-job execution overrides it only on docker compose run.'
}

$emptyNames = @(
    $requiredNames |
        Where-Object {
            Test-IsMissingRequiredValue -Name $_ -Value $values[$_] -AllowTemplateEmpty:$Template
        }
)
if ($emptyNames.Count -gt 0) {
    throw "Empty value for $($emptyNames.Count) required production environment variable(s)."
}

$imageNames = @($requiredNames | Where-Object { $_ -like '*_IMAGE' })
foreach ($name in $imageNames) {
    $image = $values[$name]
    if ($image -notmatch '^[a-zA-Z0-9._/:~-]+@sha256:[0-9a-f]{64}$') {
        throw 'Every production image must use a syntactically valid sha256 digest.'
    }
    if (-not $Template -and $image -match '@sha256:0{64}$') {
        throw 'Placeholder image digests are not allowed in a production environment.'
    }
}

$databaseUrlNames = @($requiredNames | Where-Object { $_ -like '*_DB_URL' })
foreach ($name in $databaseUrlNames) {
    $url = $values[$name]
    Assert-ProductionPostgreSqlUrl -Url $url -AllowTemplateHost:$Template
}

if (-not $Template) {
    foreach ($name in $requiredNames) {
        if ($values[$name] -match '(?i)(?:^|[./])example\.invalid(?::|/|$)') {
            throw 'Documentation-only endpoints are not allowed in a production environment.'
        }
    }

    $weakPasswords = @('postgres', 'password', 'dev_pass', 'change_me', 'replace_me')
    foreach ($name in @($requiredNames | Where-Object { $_ -like '*_DB_PASSWORD' })) {
        $password = $values[$name]
        if ($password.Length -lt 16 -or $weakPasswords -contains $password.ToLowerInvariant()) {
            throw 'A production database password does not meet the minimum policy.'
        }
    }

    foreach ($name in @($requiredNames | Where-Object { $_ -like '*_JWT_SECRET' })) {
        if ($values[$name].Length -lt 32) {
            throw 'JWT secrets must be at least 32 characters.'
        }
    }

    if ($values['AUTH_DEFAULT_PASSWORD'].Length -lt 16 -or
            $weakPasswords -contains $values['AUTH_DEFAULT_PASSWORD'].ToLowerInvariant()) {
        throw 'AUTH_DEFAULT_PASSWORD does not meet the minimum production policy.'
    }
    if ($values['AUTH_INTERNAL_API_TOKEN'].Length -lt 32) {
        throw 'AUTH_INTERNAL_API_TOKEN must be at least 32 characters.'
    }
}

foreach ($name in @($requiredNames | Where-Object { $_ -like '*_DB_USER' })) {
    if ($values[$name] -match '(?i)(^|_)owner$') {
        throw 'Long-running production services must use a least-privilege runtime database user, not an owner role.'
    }
}

if ($composeText -match '(?m)^\s*build\s*:') {
    throw 'Production Compose must not contain source build directives.'
}
if ($composeText -match '(?i)(jdbc:h2:|ddl-auto:\s*(create|create-drop|update))') {
    throw 'Production Compose contains a forbidden database fallback or schema mutation policy.'
}

[pscustomobject]@{
    Status            = 'PASS'
    TemplateMode      = [bool]$Template
    RequiredVariables = $requiredNames.Count
    ImmutableImages   = $imageNames.Count
    PostgreSqlUrls    = $databaseUrlNames.Count
}
