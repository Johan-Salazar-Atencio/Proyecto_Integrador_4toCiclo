# Mantenedor del tunel USB Plantia
# Re-establece "adb reverse tcp:8080" cada vez que el celular reaparece.
# No requiere administrador. Ejecutar con: powershell -ExecutionPolicy Bypass -File tunel-plantia.ps1
$adb = "C:\Users\ACER\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$serial = "AK7BCP4228409141"

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
