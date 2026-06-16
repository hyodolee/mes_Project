param(
    [string]$DbPassword,
    [switch]$NoPrompt
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$mesDir = Join-Path $root "mes_backserver"
# MCS는 mes_backserver(com.mes.mcs)로 병합되어 단일 8080에서 실행됩니다.

if (-not $DbPassword) {
    $DbPassword = $env:MES_DB_PASSWORD
}

if (-not $DbPassword -and -not $NoPrompt) {
    $secure = Read-Host "MES_DB_PASSWORD" -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $DbPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (-not $DbPassword) {
    throw "MES_DB_PASSWORD is required. Pass -DbPassword or set the MES_DB_PASSWORD environment variable."
}

function Stop-PortProcess {
    param([int]$Port)

    $connections = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        if ($connection.OwningProcess) {
            Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}

function Start-Backend {
    param(
        [string]$Name,
        [string]$Directory,
        [int]$Port
    )

    $outLog = Join-Path $Directory "bootRun.out.log"
    $errLog = Join-Path $Directory "bootRun.err.log"

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = Join-Path $Directory "gradlew.bat"
    $psi.Arguments = "bootRun"
    $psi.WorkingDirectory = $Directory
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.Environment["MES_DB_PASSWORD"] = $DbPassword

    $process = [System.Diagnostics.Process]::Start($psi)

    Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
        if ($EventArgs.Data) {
            Add-Content -Path $Event.MessageData.OutLog -Value $EventArgs.Data
        }
    } -MessageData @{ OutLog = $outLog } | Out-Null

    Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
        if ($EventArgs.Data) {
            Add-Content -Path $Event.MessageData.ErrLog -Value $EventArgs.Data
        }
    } -MessageData @{ ErrLog = $errLog } | Out-Null

    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()

    Write-Output "$Name started: PID=$($process.Id), PORT=$Port"
}

Stop-PortProcess -Port 8080

Start-Backend -Name "MES" -Directory $mesDir -Port 8080

Write-Output "Logs:"
Write-Output "  MES: $mesDir\bootRun.out.log / $mesDir\bootRun.err.log"
