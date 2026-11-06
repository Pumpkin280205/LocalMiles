# Takes screenshots from your phone over USB, one screen at a time
#
# Before you start:
#   1. On the phone turn on Developer options > USB debugging, plug it in and tap "Allow"
#   2. Check adb works:  adb devices   (your phone should show as "device")
#      adb is in <Android SDK>\platform-tools
#
# How to run (PowerShell, from this folder):
#   .\capture-screens.ps1                 # goes through the whole list in docs\SHOT_LIST.md
#   .\capture-screens.ps1 -OutDir raw     # pick the output folder (default is raw)

param([string]$OutDir = "raw")

$shots = @(
    "01-login", "02-biometric", "03-settings", "04-driver-stops", "05-offline-banner",
    "06-pod", "07-live-map", "08-fleet", "09-client-tracking", "10-notification",
    "11-languages", "12-dispatcher-dashboard"
)

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Host "adb not found. Add <Android SDK>\platform-tools to PATH." -ForegroundColor Red
    exit 1
}
New-Item -ItemType Directory -Force $OutDir | Out-Null

foreach ($name in $shots) {
    $answer = Read-Host "Show the '$name' screen on the phone, then press Enter (type s to skip)"
    if ($answer -eq "s") { continue }
    # take the screenshot on the phone then copy it over (using > breaks the PNG in PowerShell)
    adb shell screencap -p /sdcard/lm_shot.png
    adb pull /sdcard/lm_shot.png (Join-Path $OutDir "$name.png") | Out-Null
    adb shell rm /sdcard/lm_shot.png
    Write-Host "saved $OutDir\$name.png" -ForegroundColor Green
}
Write-Host "Done. Next: python frame_screenshots.py $OutDir play-store-shots"
