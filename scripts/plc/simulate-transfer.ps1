<#
.SYNOPSIS
Simulates PLC events for an MCS transfer order.

.EXAMPLE
.\scripts\plc\simulate-transfer.ps1 -TransferId 12 -Scenario Success

.EXAMPLE
.\scripts\plc\simulate-transfer.ps1 -TransferId 12 -Scenario Success -Mode DirectMcs

.EXAMPLE
.\scripts\plc\simulate-transfer.ps1 -TransferId 12 -Scenario EquipmentError -DryRun
#>

param(
    [Parameter(Mandatory = $true)]
    [long]$TransferId,

    [ValidateSet("Success", "EquipmentError", "SensorMismatch", "InterlockBlocked", "Timeout")]
    [string]$Scenario = "Success",

    [ValidateSet("PlcApi", "DirectMcs")]
    [string]$Mode = "PlcApi",

    [string]$BaseUrl = "http://localhost:8081",

    [string]$EquipmentCd = "CV-001",

    [string]$StartLocationCd = "",

    [string]$EndLocationCd = "",

    [int]$DelaySeconds = 3,

    [switch]$SkipStart,

    [switch]$OnlyStart,

    [switch]$OnlyComplete,

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param(
        [string]$Message
    )

    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

function Send-PlcEvent {
    param(
        [string]$EventType,
        [string]$EventStatus = "NORMAL",
        [string]$LocationCd = "",
        [string]$ErrorCode = "",
        [string]$Message = ""
    )

    $body = @{
        equipmentCd = $EquipmentCd
        eventType = $EventType
        eventStatus = $EventStatus
        targetType = "TRANSFER"
        targetId = $TransferId
        locationCd = $LocationCd
        errorCode = $ErrorCode
        message = $Message
        eventDtm = (Get-Date).ToString("s")
    }

    $json = $body | ConvertTo-Json -Depth 5
    $uri = "$BaseUrl/api/plc/events"

    if ($DryRun) {
        Write-Step "DRY-RUN PLC API POST $uri"
        Write-Host $json
        return
    }

    Write-Step "Send PLC event: $EventType"
    try {
        Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json; charset=utf-8" -Body $json | Out-Null
        Write-Step "PLC event request succeeded: $EventType"
    } catch {
        Write-Step "PLC event request failed: $EventType"
        Write-Step $_.Exception.Message

        if ($_.Exception.Response -ne $null) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseText = $reader.ReadToEnd()
            if (![string]::IsNullOrWhiteSpace($responseText)) {
                Write-Host "----- SERVER RESPONSE BEGIN -----"
                Write-Host $responseText
                Write-Host "----- SERVER RESPONSE END -----"
            }
        }

        throw
    }
}

function Send-DirectMcsStatus {
    param(
        [string]$Status
    )

    $uri = "$BaseUrl/transfers/$TransferId/status"
    $body = "status=$Status"

    if ($DryRun) {
        Write-Step "DRY-RUN MCS STATUS POST $uri body=$body"
        return
    }

    Write-Step "Request MCS transfer status: $Status"
    try {
        $request = [System.Net.HttpWebRequest]::Create($uri)
        $request.Method = "POST"
        $request.ContentType = "application/x-www-form-urlencoded"
        $request.AllowAutoRedirect = $false

        $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
        $request.ContentLength = $bytes.Length
        $stream = $request.GetRequestStream()
        $stream.Write($bytes, 0, $bytes.Length)
        $stream.Close()

        $response = $request.GetResponse()
        $statusCode = [int]$response.StatusCode
        $response.Close()

        if ($statusCode -ge 200 -and $statusCode -lt 400) {
            Write-Step "MCS transfer status request succeeded: $Status (HTTP $statusCode)"
            return
        }

        throw "Unexpected HTTP status: $statusCode"
    } catch {
        Write-Step "MCS transfer status request failed: $Status"
        Write-Step $_.Exception.Message

        if ($_.Exception.Response -ne $null) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseText = $reader.ReadToEnd()
            if (![string]::IsNullOrWhiteSpace($responseText)) {
                Write-Host "----- SERVER RESPONSE BEGIN -----"
                Write-Host $responseText
                Write-Host "----- SERVER RESPONSE END -----"
            }
        }

        throw
    }
}

function Start-Transfer {
    if ($Mode -eq "DirectMcs") {
        Send-DirectMcsStatus -Status "IN_PROGRESS"
        return
    }

    Send-PlcEvent `
        -EventType "TRANSFER_STARTED" `
        -LocationCd $StartLocationCd `
        -Message "Transfer $TransferId started"
}

function Complete-Transfer {
    if ($Mode -eq "DirectMcs") {
        Send-DirectMcsStatus -Status "COMPLETED"
        return
    }

    Send-PlcEvent `
        -EventType "TRANSFER_COMPLETED" `
        -LocationCd $EndLocationCd `
        -Message "Transfer $TransferId completed"
}

function Send-Running {
    if ($Mode -eq "DirectMcs") {
        Write-Step "DirectMcs mode skips EQUIPMENT_RUNNING because PLC API is not used."
        return
    }

    Send-PlcEvent `
        -EventType "EQUIPMENT_RUNNING" `
        -LocationCd $StartLocationCd `
        -Message "$EquipmentCd is running"
}

function Send-EquipmentError {
    if ($Mode -eq "DirectMcs") {
        Write-Step "DirectMcs mode cannot store equipment error events yet. Only IN_PROGRESS is reproduced."
        return
    }

    Send-PlcEvent `
        -EventType "EQUIPMENT_ERROR" `
        -EventStatus "ERROR" `
        -LocationCd $StartLocationCd `
        -ErrorCode "MOTOR_OVERLOAD" `
        -Message "$EquipmentCd motor overload"
}

function Send-SensorMismatch {
    if ($Mode -eq "DirectMcs") {
        Write-Step "DirectMcs mode cannot store sensor mismatch events yet. Only IN_PROGRESS is reproduced."
        return
    }

    Send-PlcEvent `
        -EventType "ARRIVED_WRONG_LOCATION" `
        -EventStatus "INTERLOCK" `
        -LocationCd "WRONG-LOCATION" `
        -ErrorCode "SENSOR_LOCATION_MISMATCH" `
        -Message "Detected location is different from transfer destination"
}

function Send-InterlockBlocked {
    if ($Mode -eq "DirectMcs") {
        Write-Step "DirectMcs mode cannot store interlock events yet. Only IN_PROGRESS is reproduced."
        return
    }

    Send-PlcEvent `
        -EventType "INTERLOCK_BLOCKED" `
        -EventStatus "INTERLOCK" `
        -LocationCd $EndLocationCd `
        -ErrorCode "DESTINATION_BLOCKED" `
        -Message "Transfer completion blocked by destination interlock"
}

Write-Step "PLC simulation started: TransferId=$TransferId, Scenario=$Scenario, Mode=$Mode"

if ($OnlyStart -and $OnlyComplete) {
    throw "OnlyStart and OnlyComplete cannot be used together."
}

switch ($Scenario) {
    "Success" {
        if (!$SkipStart -and !$OnlyComplete) {
            Start-Transfer
        }
        if ($OnlyStart) {
            break
        }
        Start-Sleep -Seconds $DelaySeconds
        Send-Running
        Start-Sleep -Seconds $DelaySeconds
        Complete-Transfer
    }
    "EquipmentError" {
        Start-Transfer
        Start-Sleep -Seconds $DelaySeconds
        Send-EquipmentError
    }
    "SensorMismatch" {
        Start-Transfer
        Start-Sleep -Seconds $DelaySeconds
        Send-SensorMismatch
    }
    "InterlockBlocked" {
        Start-Transfer
        Start-Sleep -Seconds $DelaySeconds
        Send-InterlockBlocked
    }
    "Timeout" {
        Start-Transfer
        Write-Step "Completion event is intentionally skipped for timeout testing."
    }
}

Write-Step "PLC simulation finished"
