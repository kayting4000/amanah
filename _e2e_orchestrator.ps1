$root   = 'c:\Users\MSU\Desktop\amanah'
$status = $root + '\e2e-status.txt'
$test   = $root + '\_e2e_test.ps1'

function St($m) { Add-Content -Path $status -Value ((Get-Date -Format 'HH:mm:ss') + ' ' + $m) -Encoding utf8 }

Set-Content -Path $status -Value 'orchestrator started' -Encoding utf8
St 'waiting for amanah_app to serve HTTP (max 20 min)'

$deadline = (Get-Date).AddMinutes(20)
$ready = $false
while ((Get-Date) -lt $deadline) {
  $app = ((& docker inspect -f '{{.State.Status}}' amanah_app 2>&1 | Out-String)).Trim()
  $my  = ((& docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}nohealth{{end}}' amanah_mysql 2>&1 | Out-String)).Trim()
  $code = 'none'
  try {
    $r = Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method POST `
      -ContentType 'application/json' -Body '{"username":"admin","password":"Admin@1234"}' `
      -UseBasicParsing -TimeoutSec 10
    $code = [int]$r.StatusCode
  } catch {
    $resp = $_.Exception.Response
    if ($resp) { $code = [int]$resp.StatusCode }
  }
  if ($code -ne 'none' -and $my -eq 'healthy') { $ready = $true; St ('READY (probe http ' + $code + ', mysql healthy)'); break }
  St ('waiting: app=' + $app + ' mysql=' + $my + ' probe=' + $code)
  Start-Sleep -Seconds 15
}

if ($ready) {
  St 'running smoke test'
  (& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $test 2>&1 | Out-String) | Add-Content -Path $status -Encoding utf8
  St 'smoke test finished'
} else {
  St 'NOT READY - diagnostics follow'
  ((& docker compose ps 2>&1) | Out-String) | Add-Content -Path $status -Encoding utf8
  ((& docker logs --tail 80 amanah_app 2>&1) | Out-String) | Add-Content -Path $status -Encoding utf8
}
