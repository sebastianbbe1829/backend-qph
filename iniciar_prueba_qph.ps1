$ErrorActionPreference = 'Stop'

$backend = $PSScriptRoot
$frontend = Join-Path (Split-Path -Parent $backend) 'frontend-qph'

if (-not (Test-Path (Join-Path $backend 'mvnw.cmd'))) {
    throw "No se encontró mvnw.cmd en $backend"
}
if (-not (Test-Path (Join-Path $frontend 'package.json'))) {
    throw "No se encontró package.json en $frontend"
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
$generatedJwtSecret = $false
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    $secretBytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($secretBytes)
        $env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
        $generatedJwtSecret = $true
    }
    finally {
        $random.Dispose()
        [Array]::Clear($secretBytes, 0, $secretBytes.Length)
    }
    Write-Host 'Se genero una clave JWT temporal para esta ejecucion.'
}

$env:DB_SCHEMA_INIT_MODE = 'always'

$backendCommand = "cd /d `"$backend`" && call mvnw.cmd spring-boot:run"
Start-Process -FilePath $env:ComSpec -ArgumentList '/k', $backendCommand -WorkingDirectory $backend

Write-Host 'Esperando que el backend responda en el puerto 8080 (hasta 5 minutos)...'
$backendReady = $false
$backendDeadline = [DateTime]::UtcNow.AddMinutes(5)
$waitStarted = Get-Date
$attempt = 0
while ([DateTime]::UtcNow -lt $backendDeadline) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.BeginConnect('127.0.0.1', 8080, $null, $null)
        if ($connection.AsyncWaitHandle.WaitOne(1000, $false) -and $client.Connected) {
            $client.EndConnect($connection)
            $backendReady = $true
            break
        }
    }
    catch {
    }
    finally {
        $client.Dispose()
    }
    $attempt++
    if ($attempt % 6 -eq 0) {
        Write-Host "Backend sigue iniciando... $([int]((Get-Date).Subtract($waitStarted).TotalSeconds)) segundos."
    }
    Start-Sleep -Seconds 1
}
if (-not $backendReady) {
    throw 'El backend no respondio en 5 minutos. Revisa su ventana de consola.'
}

$frontendCommand = "cd /d `"$frontend`" && call npm.cmd start -- --host 127.0.0.1"
Start-Process -FilePath $env:ComSpec -ArgumentList '/k', $frontendCommand -WorkingDirectory $frontend

Write-Host 'Esperando que Angular compile y responda por HTTP (hasta 5 minutos)...'
$frontendReady = $false
$frontendDeadline = [DateTime]::UtcNow.AddMinutes(5)
$waitStarted = Get-Date
$attempt = 0
while ([DateTime]::UtcNow -lt $frontendDeadline) {
    try {
        $secondsRemaining = [int][Math]::Ceiling(($frontendDeadline - [DateTime]::UtcNow).TotalSeconds)
        $response = Invoke-WebRequest `
            -Uri 'http://127.0.0.1:4200/login' `
            -TimeoutSec ([Math]::Min(4, $secondsRemaining)) `
            -Proxy $null `
            -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            $frontendReady = $true
            break
        }
    }
    catch {
    }
    $attempt++
    if ($attempt % 3 -eq 0) {
        Write-Host "Angular sigue compilando... $([int]((Get-Date).Subtract($waitStarted).TotalSeconds)) segundos."
    }
    Start-Sleep -Seconds 5
}
if (-not $frontendReady) {
    throw 'Angular no respondio por HTTP en 5 minutos. Revisa la ventana del frontend.'
}

Start-Process 'http://127.0.0.1:4200/login'

Write-Host ''
Write-Host 'QPH iniciado:'
Write-Host '  Backend:  http://localhost:8080'
Write-Host '  Frontend: http://127.0.0.1:4200/login'
if ($generatedJwtSecret) {
    Write-Host 'Nota: la clave JWT se regenerara al cerrar esta ventana; se cerraran las sesiones actuales.'
}
