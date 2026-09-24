$out = Join-Path $env:TEMP 'amanah-chk.txt'
$L = @()
$L += 'now ' + (Get-Date -Format 'HH:mm:ss')
$L += '--- running containers ---'
$L += ((& docker ps --format '{{.Names}} | {{.Image}} | {{.Status}}' 2>&1 | Out-String -Width 200)).Trim()
$L += '--- root artifacts ---'
$L += ((& Get-ChildItem 'c:\Users\MSU\Desktop\amanah' -Force | Where-Object { $_.Name -like '_*' -or $_.Name -like 'pkg*' } | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize | Out-String -Width 140)).Trim()
$L += '--- jar present: ' + (Test-Path 'c:\Users\MSU\Desktop\amanah\target\banking-1.0.0.jar')
Set-Content -Path $out -Value $L -Encoding utf8
