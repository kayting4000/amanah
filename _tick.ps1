$root = 'c:\Users\MSU\Desktop\amanah'
$out  = Join-Path $env:TEMP 'amanah-watch.txt'
$images = ((& docker images --format '{{.Repository}}:{{.Tag}}' 2>&1 | Where-Object { $_ -match 'amanah' }) -join ',')
$containers = ((& docker ps -a --filter 'name=amanah' --format '{{.Names}}={{.Status}}' 2>&1) -join '; ')
$logPath = Join-Path $root 'e2e.log'
$logBytes = if (Test-Path $logPath) { (Get-Item $logPath).Length } else { 0 }
$value = (Get-Date -Format 'HH:mm:ss') + ' img=[' + $images + '] ctr=[' + $containers + '] logBytes=' + $logBytes
Add-Content -Path $out -Value $value -Encoding utf8
