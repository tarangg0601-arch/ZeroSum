$ErrorActionPreference = 'Stop'

$environmentNames = @(
    'DB_URL',
    'DB_USERNAME',
    'DB_PASSWORD',
    'DB_SSLMODE',
    'FLYWAY_DB_USERNAME',
    'FLYWAY_DB_PASSWORD',
    'APP_AUTH_TOKEN_SECRET'
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$exitCode = 1
$temporaryAuthSecret = $false
try {
    $env:DB_URL = 'jdbc:postgresql://ep-rapid-haze-b5jwisg6.c-7.us-east-2.aws.neon.tech:5432/neondb?sslmode=require'
    $env:DB_USERNAME = 'neondb_owner'
    $env:DB_SSLMODE = 'require'
    Remove-Item Env:FLYWAY_DB_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:FLYWAY_DB_PASSWORD -ErrorAction SilentlyContinue

    $securePassword = Read-Host 'Enter the Neon database password (input is hidden)' -AsSecureString
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
    $securePassword.Dispose()

    if ([string]::IsNullOrWhiteSpace($env:APP_AUTH_TOKEN_SECRET)) {
        $randomBytes = New-Object byte[] 32
        $randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $randomGenerator.GetBytes($randomBytes)
        }
        finally {
            $randomGenerator.Dispose()
        }
        $env:APP_AUTH_TOKEN_SECRET = [Convert]::ToBase64String($randomBytes)
        $temporaryAuthSecret = $true
        Write-Host 'Generated a temporary JWT secret for this run. Sign in again after restarting the app.'
    }

    Push-Location $PSScriptRoot
    try {
        & .\mvnw.cmd spring-boot:run
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    foreach ($name in $environmentNames) {
        $oldValue = $previousEnvironment[$name]
        if ($null -eq $oldValue) {
            Remove-Item "Env:$name" -ErrorAction SilentlyContinue
        }
        else {
            [Environment]::SetEnvironmentVariable($name, $oldValue, 'Process')
        }
    }
}

exit $exitCode
