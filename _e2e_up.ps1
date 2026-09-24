$ErrorActionPreference = 'Continue'
$root = 'c:\Users\MSU\Desktop\amanah'
$log  = $root + '\e2e.log'

function Say($m) { Add-Content -Path $log -Value ((Get-Date -Format 'HH:mm:ss') + ' ' + $m) -Encoding utf8 }

Say '=== docker compose up --build -d (start) ==='
Push-Location $root
(& docker compose up --build -d 2>&1 | Out-String -Width 220) | Add-Content -Path $log -Encoding utf8
Pop-Location
Say '=== docker compose up --build -d (returned) ==='

$deadline = (Get-Date).AddMinutes(9)
$ready = $false
$last = ''
while ((Get-Date) -lt $deadline) {
  $appState = (& docker inspect -f '{{.State.Status}}' amanah_app 2>&1 | Out-String).Trim()
  $myState  = (& docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}nohealth{{end}}' amanah_mysql 2>&1 | Out-String).Trim()
  try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST `
      -ContentType 'application/json' -Body '{"username":"admin","password":"Admin@1234"}' `
      -UseBasicParsing -TimeoutSec 15
    $last = 'http ' + [int]$r.StatusCode
    if ([int]$r.StatusCode -eq 200) { $ready = $true; break }
  } catch {
    $resp = $_.Exception.Response
    if ($resp) { $last = 'http ' + [int]$resp.StatusCode } else { $last = $_.Exception.Message }
  }
  Say ("waiting: app=$appState mysql=$myState last=$last")
  Start-Sleep -Seconds 10
}

Say ("READY=" + $ready + " (admin login last=" + $last + ")")
Say '--- docker compose ps ---'
(& docker compose ps 2>&1 | Out-String -Width 220) | Add-Content -Path $log -Encoding utf8
Say '--- app log tail ---'
(& docker logs --tail 25 amanah_app 2>&1 | Out-String -Width 220) | Add-Content -Path $log -Encoding utf8
