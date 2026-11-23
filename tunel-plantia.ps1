# Mantenedor del tunel USB Plantia
# Re-establece "adb reverse tcp:8080" cada vez que el celular reaparece.
# No requiere administrador. Ejecutar con: powershell -ExecutionPolicy Bypass -File tunel-plantia.ps1
# Opcional: -Adb <ruta de adb.exe> y -Serial <serial del telefono> (o las variables PLANTIA_ADB y PLANTIA_SERIAL).
# Sin serial se usa el telefono conectado. Antes iban fijos la ruta de un usuario y el serial de un telefono (QA-32).
param(
    [string]$Adb = $env:PLANTIA_ADB,
    [string]$Serial = $env:PLANTIA_SERIAL
)
if (-not $Adb) {
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $Adb = Join-Path $sdk "platform-tools\adb.exe"
}
$adb = $Adb
$serial = if ($Serial) { [regex]::Escape($Serial) } else { "\S+" }

Write-Host "Tunel Plantia activo. Ctrl+C para detener."
while ($true) {
    try {
        $devices = & $adb devices 2>$null | Select-String -Pattern "^\s*$serial\s+device$"
        if ($devices) {
            $lista = (& $adb reverse --list 2>$null) -join " "
            if ($lista -notmatch "8080") {
                & $adb reverse tcp:8080 tcp:8080 2>$null | Out-Null
                Write-Host ("{0} tunel repuesto (adb reverse tcp:8080)" -f (Get-Date -Format HH:mm:ss))
            }
        }
    } catch { }
    Start-Sleep -Seconds 4
}
