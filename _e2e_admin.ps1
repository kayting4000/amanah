$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$root = 'c:\Users\MSU\Desktop\amanah'
$rep  = $root + '\e2e-admin-report.txt'
$pass = 0
$fail = 0
$res = New-Object System.Collections.ArrayList

function Check($ok, $name, $detail) {
  if ($ok) { $script:pass = $script:pass + 1; $t = 'PASS' } else { $script:fail = $script:fail + 1; $t = 'FAIL' }
  [void]$script:res.Add('[' + $t + '] ' + $name + '  ::  ' + $detail)
}
function Call($verb, $url, $token, $body) {
  $headers = @{}
  if ($token) { $headers['Authorization'] = 'Bearer ' + $token }
  $p = @{ Uri = $url; Method = $verb; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 40 }
  if ($body) { $p['Body'] = $body; $p['ContentType'] = 'application/json' }
  try { $r = Invoke-WebRequest @p; return @{ status = [int]$r.StatusCode; text = $r.Content } }
  catch {
    $resp = $_.Exception.Response
    if ($resp -is [System.Net.WebResponse]) {
      $s = New-Object System.IO.StreamReader($resp.GetResponseStream())
      return @{ status = [int]$resp.StatusCode; text = $s.ReadToEnd() }
    }
    return @{ status = -1; text = $_.Exception.Message }
    return @{ status = -1; text = $_.Exception.Message }
  }
}
function J($o) { return ($o | ConvertTo-Json -Compress) }
function Sql($q) { return ((& docker exec -e MYSQL_PWD=rootpass amanah_mysql mysql -uroot -N -B -e $q amanah_db 2>&1 | Out-String)).Trim() }
function Num($v) { try { return [int](([string]$v).Trim()) } catch { return -1 } }

$customerUser = Sql 'SELECT u.username FROM users u JOIN customers c ON c.user_id = u.id JOIN accounts a ON a.customer_id = c.id WHERE a.id = 1 LIMIT 1;'
$customerUserRow = Sql 'SELECT u.id FROM users u JOIN customers c ON c.user_id = u.id JOIN accounts a ON a.customer_id = c.id WHERE a.id = 1 LIMIT 1;'
$acctRow = Sql 'SELECT account_number, balance FROM accounts WHERE id = 1;'
[void]$res.Add('target customer=' + $customerUser + ' (userId=' + $customerUserRow + ') account1=' + $acctRow)

$al = Call 'POST' ($base + '/api/auth/login') $null '{"username":"admin","password":"Admin@1234"}'
$ab = $al.text | ConvertFrom-Json
$adminTok = $ab.data.token
Check ($al.status -eq 200 -and $ab.data.role -eq 'ADMIN') 'admin login -> ADMIN token' ("status=" + $al.status + " role=" + $ab.data.role)

$cl = Call 'POST' ($base + '/api/auth/login') $null (J @{ username = $customerUser; password = 'Smoke@12345' })
$cb = $cl.text | ConvertFrom-Json
$custTok = $cb.data.token
Check ($cl.status -eq 200 -and $cb.data.role -eq 'CUSTOMER') 'customer login -> CUSTOMER token' ("status=" + $cl.status + " role=" + $cb.data.role)

$notAdmin = Call 'PUT' ($base + '/api/admin/accounts/1/freeze') $custTok $null
Check ($notAdmin.status -eq 403) 'customer hitting /api/admin/** -> 403 (RBAC)' ("status=" + $notAdmin.status + " body=" + $notAdmin.text.Substring(0, [Math]::Min(90, $notAdmin.text.Length)))

$freeze = Call 'PUT' ($base + '/api/admin/accounts/1/freeze') $adminTok $null
$fb = $freeze.text | ConvertFrom-Json
Check ($freeze.status -eq 200 -and $fb.data.status -eq 'FROZEN') 'admin freezes account 1' ("status=" + $freeze.status + " accountStatus=" + $fb.data.status)

$depFrozen = Call 'POST' ($base + '/api/accounts/1/deposit') $custTok (J @{ amount = 10.00 })
$dfb = $depFrozen.text | ConvertFrom-Json
Check ($depFrozen.status -eq 400 -and $dfb.message -match 'FROZEN') 'deposit into FROZEN account -> 400 AccountNotActive' ("status=" + $depFrozen.status + " message=" + $dfb.message)

$unfreeze = Call 'PUT' ($base + '/api/admin/accounts/1/unfreeze') $adminTok $null
$ufb = $unfreeze.text | ConvertFrom-Json
Check ($unfreeze.status -eq 200 -and $ufb.data.status -eq 'ACTIVE') 'admin unfreezes account 1' ("status=" + $unfreeze.status + " accountStatus=" + $ufb.data.status)

$dep = Call 'POST' ($base + '/api/accounts/1/deposit') $custTok (J @{ amount = 10.00; description = 'post-unfreeze deposit' })
$db2 = $dep.text | ConvertFrom-Json
$ref = $db2.data.referenceNumber
Check ($dep.status -eq 200 -and $ref.Length -eq 20 -and $ref -match '^TXN[0-9]{17}$') 'deposit works again after unfreeze (20-char ref)' ("status=" + $dep.status + " ref=" + $ref + " after=" + $db2.data.balanceAfter)

$cust = Call 'GET' ($base + '/api/admin/customers/1') $adminTok $null
Check ($cust.status -eq 200) 'admin GET /api/admin/customers/1' ("status=" + $cust.status + " body=" + $cust.text.Substring(0, [Math]::Min(150, $cust.text.Length)))

$susp = Call 'PUT' ($base + '/api/admin/users/' + $customerUserRow + '/suspend') $adminTok $null
$loginSusp = Call 'POST' ($base + '/api/auth/login') $null (J @{ username = $customerUser; password = 'Smoke@12345' })
Check ($susp.status -eq 200 -and $loginSusp.status -eq 401) 'suspended user cannot log in' ("suspendStatus=" + $susp.status + " loginStatus=" + $loginSusp.status)

$act = Call 'PUT' ($base + '/api/admin/users/' + $customerUserRow + '/activate') $adminTok $null
$loginAct = Call 'POST' ($base + '/api/auth/login') $null (J @{ username = $customerUser; password = 'Smoke@12345' })
Check ($act.status -eq 200 -and $loginAct.status -eq 200) 'reactivated user can log in again' ("activateStatus=" + $act.status + " loginStatus=" + $loginAct.status)

$refOut = Sql 'SELECT COUNT(*), MIN(CHAR_LENGTH(reference_number)), MAX(CHAR_LENGTH(reference_number)), COUNT(DISTINCT reference_number) FROM transactions;'
$p = @($refOut -split "`t")
Check (($p.Count -ge 4) -and ((Num $p[0]) -ge 5) -and ((Num $p[1]) -eq 20) -and ((Num $p[2]) -eq 20) -and ((Num $p[0]) -eq (Num $p[3]))) 'all DB references still exactly 20 chars and unique' ("summary=" + $refOut)

$outs = New-Object System.Collections.ArrayList
[void]$outs.Add('AMANAH - admin/RBAC verification (live stack on http://localhost:8080)')
[void]$outs.Add('run at ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
[void]$outs.Add('')
foreach ($l in $res) { [void]$outs.Add($l) }
[void]$outs.Add('')
[void]$outs.Add('TOTAL: ' + ($pass + $fail) + '    PASS: ' + $pass + '    FAIL: ' + $fail)
Set-Content -Path $rep -Value $outs -Encoding utf8
