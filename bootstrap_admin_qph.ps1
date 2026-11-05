$ErrorActionPreference = 'Stop'

$backend = $PSScriptRoot
if (-not (Test-Path (Join-Path $backend 'mvnw.cmd'))) {
    throw "No se encontró mvnw.cmd en $backend"
}

if ([string]::IsNullOrWhiteSpace($env:DB_URL)) {
    $env:DB_URL = 'jdbc:postgresql://ep-rapid-bird-axslgcz8-pooler.c-4.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require&currentSchema=qph_prueba'
}
if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) {
    $env:DB_USERNAME = 'neondb_owner'
}
if ([string]::IsNullOrEmpty($env:DB_PASSWORD)) {
    $securePassword = Read-Host 'Contrasena de PostgreSQL' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $secretBytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($secretBytes)
        $env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
    }
    finally {
        $random.Dispose()
        [Array]::Clear($secretBytes, 0, $secretBytes.Length)
    }
}

$username = Read-Host 'Nombre del administrador inicial (3-50 caracteres)'
$secureAdminPassword = Read-Host 'Contrasena del administrador (8-72 caracteres)' -AsSecureString
$adminPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureAdminPassword)
try {
    $env:APP_BOOTSTRAP_ADMIN_PASSWORD =
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($adminPasswordPointer)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($adminPasswordPointer)
}

if ($username -notmatch '^[a-zA-Z0-9._-]{3,50}$') {
    throw 'El nombre debe tener entre 3 y 50 caracteres: letras, numeros, punto, guion o guion bajo.'
}
if ($env:APP_BOOTSTRAP_ADMIN_PASSWORD.Length -lt 8 -or
    $env:APP_BOOTSTRAP_ADMIN_PASSWORD.Length -gt 72) {
    throw 'La contrasena debe tener entre 8 y 72 caracteres.'
}

$env:APP_BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:APP_BOOTSTRAP_ADMIN_USERNAME = $username
$env:SPRING_MAIN_WEB_APPLICATION_TYPE = 'none'
$env:DB_SCHEMA_INIT_MODE = 'always'

Push-Location $backend
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "Maven termino con codigo $LASTEXITCODE"
    }
}
finally {
    Pop-Location
    Remove-Item Env:APP_BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:APP_BOOTSTRAP_ADMIN_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:APP_BOOTSTRAP_ADMIN_ENABLED -ErrorAction SilentlyContinue
    Remove-Item Env:SPRING_MAIN_WEB_APPLICATION_TYPE -ErrorAction SilentlyContinue
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}

Write-Host "Administrador '$username' inicializado. Ahora ejecuta iniciar_prueba_qph.bat."
