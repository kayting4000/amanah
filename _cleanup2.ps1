$root = 'c:\Users\MSU\Desktop\amanah'
Set-Location $root
$files = @(
  '_e2e0.ps1','_e2e_up.ps1','_e2e_test.ps1','_e2e_run.ps1','_e2e_orchestrator.ps1','_e2e_admin.ps1',
  '_pull2.ps1','_watchp.ps1','_tick.ps1','_sleep120.ps1','_status2.ps1','_procs.ps1','_syntax.ps1',
  '_hashrun.ps1','_hashrun2.ps1','_hashrun3.ps1','_mvntest.ps1','_pollp.ps1',
  'e2e.log','e2e-status.txt','hashcheck.txt','mvn-test2.log','e2e-report.txt','e2e-admin-report.txt'
)
foreach ($f in $files) { if (Test-Path $f) { Remove-Item $f -Force -ErrorAction SilentlyContinue } }
if (Test-Path '_hashtool') { Remove-Item '_hashtool' -Recurse -Force -ErrorAction SilentlyContinue }

$L = @()
$L += '=== remaining root entries ==='
$L += ((& Get-ChildItem -Force | Sort-Object Name | Select-Object Name, Length | Format-Table -AutoSize | Out-String -Width 120)).Trim()
$L += '=== git status --short ==='
$L += ((& git --no-pager status --short 2>&1) | Out-String).Trim()
$L += '=== git log --oneline -3 ==='
$L += ((& git --no-pager log --oneline -3 2>&1) | Out-String).Trim()
$L += '=== compose ps ==='
$L += ((& docker compose ps 2>&1) | Out-String).Trim()
$L += '=== jar ==='
$L += ((& Get-Item ($root + '\target\banking-1.0.0.jar') | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize | Out-String -Width 120)).Trim()
$L += '=== migrations ==='
$L += ((& Get-ChildItem ($root + '\src\main\resources\db\migration') | Select-Object Name, Length | Format-Table -AutoSize | Out-String -Width 120)).Trim()
Set-Content -Path (Join-Path $env:TEMP 'amanah-final2.txt') -Value $L -Encoding utf8
