$ErrorActionPreference = 'Continue'
$root = 'c:\Users\MSU\Desktop\amanah'
$log  = $root + '\e2e.log'
$rep  = $root + '\e2e-report.txt'
$base = 'http://localhost:8080'
$suffix = (Get-Date -Format 'HHmmss')
$pass = 0
$fail = 0
$res = New-Object System.Collections.ArrayList

function Say($m) { Add-Content -Path $log -Value $m -Encoding utf8 }
function Sql($q) {
  return ((& docker exec -e MYSQL_PWD=rootpass amanah_mysql mysql -uroot -N -B -e $q amanah_db 2>&1 | Out-String)).Trim()
}
function Num($v) { try { return [int](([string]$v).Trim()) } catch { return -1 } }

function Check($ok, $name, $detail) {
  if ($ok) { $script:pass = $script:pass + 1; $tag = 'PASS' } else { $script:fail = $script:fail + 1; $tag = 'FAIL' }
  [void]$script:res.Add(('[' + $tag + '] ' + $name + '  ::  ' + $detail))
}

function Call($verb, $url, $token, $body) {
  $headers = @{}
  if ($token) { $headers['Authorization'] = 'Bearer ' + $token }
  $p = @{ Uri = $url; Method = $verb; Headers = $headers; UseBasicParsing = $true; TimeoutSec = 40 }
  if ($body) { $p['Body'] = $body; $p['ContentType'] = 'application/json' }
  try {
    $r = Invoke-WebRequest @p
    return @{ status = [int]$r.StatusCode; text = $r.Content }
  } catch {
    $resp = $_.Exception.Response
    if ($resp) {
      $s = New-Object System.IO.StreamReader($resp.GetResponseStream())
      return @{ status = [int]$resp.StatusCode; text = $s.ReadToEnd() }
    }
    return @{ status = -1; text = $_.Exception.Message }
  }
}

function J($o) { return ($o | ConvertTo-Json -Compress) }
function Short($t, $n) { if (-not $t) { return '' } ; if ($t.Length -le $n) { return $t } ; return $t.Substring(0, $n) }

Say '=== SMOKE TEST START ==='

# ---------- 1. admin login (Flyway V5 seed) ----------
$adminPass = ''
$r = Call 'POST' ($base + '/api/auth/login') $null '{"username":"admin","password":"Admin@1234"}'
if ($r.status -ne 200) {
  $r2 = Call 'POST' ($base + '/api/auth/login') $null '{"username":"admin","password":"password"}'
  if ($r2.status -eq 200) { $adminPass = 'password'; $r = $r2 }
} else { $adminPass = 'Admin@1234' }
$adminBody = $null
try { $adminBody = $r.text | ConvertFrom-Json } catch {}
$adminToken = $null
if ($adminBody -and $adminBody.data) { $adminToken = $adminBody.data.token }
Check ($r.status -eq 200) 'admin login / Flyway V5 seed' ("status=" + $r.status + " workingPassword='" + $adminPass + "' role=" + $(if ($adminBody) { $adminBody.data.role } else { 'n/a' }) + " body=" + (Short $r.text 140))
Check (-not [string]::IsNullOrEmpty($adminToken)) 'admin login returns JWT' ("tokenLen=" + $(if ($adminToken) { $adminToken.Length } else { 0 }))

# ---------- 2. register two customers ----------
$uA = 'smoke' + $suffix
$uB = 'smokeb' + $suffix
$pw = 'Smoke@12345'
$regA = Call 'POST' ($base + '/api/auth/register') $null (J @{ username = $uA; email = ($uA + '@amanah.test'); password = $pw; firstName = 'Smoke'; lastName = 'Alpha'; phone = '+60123456789'; address = 'Kuala Lumpur' })
$regB = Call 'POST' ($base + '/api/auth/register') $null (J @{ username = $uB; email = ($uB + '@amanah.test'); password = $pw; firstName = 'Smoke'; lastName = 'Beta'; phone = '+60123456780'; address = 'Cyberjaya' })
$bA = $null; $bB = $null
try { $bA = $regA.text | ConvertFrom-Json } catch {}
try { $bB = $regB.text | ConvertFrom-Json } catch {}
$tokA = if ($bA) { $bA.data.token } else { $null }
$tokB = if ($bB) { $bB.data.token } else { $null }
Check ($regA.status -eq 201) 'register customer A (BCrypt hash -> users+customers rows)' ("status=" + $regA.status + " body=" + (Short $regA.text 140))
Check ($regB.status -eq 201) 'register customer B' ("status=" + $regB.status + " body=" + (Short $regB.text 140))

$dup = Call 'POST' ($base + '/api/auth/register') $null (J @{ username = $uA; email = ($uA + '@amanah.test'); password = $pw; firstName = 'Smoke'; lastName = 'Alpha'; phone = '+60123456789' })
Check ($dup.status -eq 409) 'duplicate registration -> 409 Conflict' ("status=" + $dup.status + " body=" + (Short $dup.text 120))

$loginA = Call 'POST' ($base + '/api/auth/login') $null (J @{ username = $uA; password = $pw })
Check ($loginA.status -eq 200) 'customer login with hashed password' ("status=" + $loginA.status)

$bad = Call 'POST' ($base + '/api/auth/login') $null (J @{ username = $uA; password = 'WrongPass123' })
Check ($bad.status -eq 401) 'wrong password -> 401 Unauthorized' ("status=" + $bad.status)

$anon = Call 'GET' ($base + '/api/accounts') $null $null
Check ($anon.status -eq 401 -or $anon.status -eq 403) 'no token -> 401/403' ("status=" + $anon.status)

if (-not $tokA) { Check $false 'PRE-REQ customer A token missing (skipped account/money flow)' 'see failures above' } else {
# ---------- 3. accounts ----------
$c1 = Call 'POST' ($base + '/api/accounts') $tokA (J @{ accountType = 'SAVINGS' })
$c2 = Call 'POST' ($base + '/api/accounts') $tokA (J @{ accountType = 'WADIAH' })
$ob1 = $null; $ob2 = $null
try { $ob1 = $c1.text | ConvertFrom-Json } catch {}
try { $ob2 = $c2.text | ConvertFrom-Json } catch {}
$acc1 = if ($ob1) { $ob1.data } else { $null }
$acc2 = if ($ob2) { $ob2.data } else { $null }
Check ($c1.status -eq 201) 'create SAVINGS account (AccountNumberGenerator + INSERT)' ("status=" + $c1.status + " body=" + (Short $c1.text 160))
Check ($c2.status -eq 201) 'create WADIAH account' ("status=" + $c2.status + " body=" + (Short $c2.text 160))
if ($acc1) {
  Check ($acc1.accountNumber -match '^AMN[0-9]{9}$') 'generated account number matches AMN+9 digits' ("accountNumber=" + $acc1.accountNumber)
  Check ($acc1.status -eq 'ACTIVE' -and [double]$acc1.balance -eq 0) 'new account ACTIVE with zero balance' ("status=" + $acc1.status + " balance=" + $acc1.balance)
}
$badType = Call 'POST' ($base + '/api/accounts') $tokA (J @{ accountType = 'MURABAHA' })
Check ($badType.status -eq 400) 'invalid account type -> 400' ("status=" + $badType.status + " body=" + (Short $badType.text 120))

# ---------- 4. money flow ----------
$dep = Call 'POST' ($base + '/api/accounts/' + $acc1.id + '/deposit') $tokA (J @{ amount = 1000.00; description = 'E2E initial deposit' })
$db1 = $null
try { $db1 = $dep.text | ConvertFrom-Json } catch {}
Check ($dep.status -eq 200) 'deposit 1000.00 (atomic SQL UPDATE + INSERT transaction)' ("status=" + $dep.status + " body=" + (Short $dep.text 200))
if ($db1 -and $db1.data) {
  $ref1 = $db1.data.referenceNumber
  Check ($ref1 -match '^TXN[0-9]{17}$') 'deposit reference = TXN + 17 digits (20 chars)' ("referenceNumber=" + $ref1 + " len=" + $ref1.Length)
  Check ([double]$db1.data.balanceAfter -eq 1000) 'deposit balanceAfter=1000' ("before=" + $db1.data.balanceBefore + " after=" + $db1.data.balanceAfter)
}

$wd = Call 'POST' ($base + '/api/accounts/' + $acc1.id + '/withdraw') $tokA (J @{ amount = 100.00; description = 'E2E withdrawal' })
$wd1 = $null
try { $wd1 = $wd.text | ConvertFrom-Json } catch {}
Check ($wd.status -eq 200) 'withdraw 100.00' ("status=" + $wd.status + " body=" + (Short $wd.text 200))
if ($wd1 -and $wd1.data) {
  Check ($wd1.data.referenceNumber -match '^TXN[0-9]{17}$') 'withdrawal reference = 20 chars' ("referenceNumber=" + $wd1.data.referenceNumber + " len=" + $wd1.data.referenceNumber.Length)
}

$bal1 = Call 'GET' ($base + '/api/accounts/' + $acc1.id + '/balance') $tokA $null
Check ($bal1.status -eq 200 -and ([double]($bal1.text | ConvertFrom-Json).data) -eq 900) 'balance after deposit+withdraw = 900.00' ("status=" + $bal1.status + " body=" + (Short $bal1.text 100))

$tr = Call 'POST' ($base + '/api/transfers') $tokA (J @{ sourceAccountNumber = $acc1.accountNumber; destinationAccountNumber = $acc2.accountNumber; amount = 250.50; description = 'E2E transfer' })
$tb = $null
try { $tb = $tr.text | ConvertFrom-Json } catch {}
Check ($tr.status -eq 200) 'transfer 250.50 A->B (single DB tx, 2 rows)' ("status=" + $tr.status + " body=" + (Short $tr.text 320))
if ($tb -and $tb.data) {
  Check ($tb.data.Count -eq 2) 'transfer returns TRANSFER_OUT + TRANSFER_IN' ("count=" + $tb.data.Count + " types=" + (($tb.data | ForEach-Object { $_.transactionType }) -join ','))
  $refsOk = $true
  foreach ($t in $tb.data) { if ($t.referenceNumber -notmatch '^TXN[0-9]{17}$') { $refsOk = $false } }
  Check $refsOk 'both transfer references are 20 chars' ("refs=" + (($tb.data | ForEach-Object { $_.referenceNumber + '(' + $_.referenceNumber.Length + ')' }) -join ' , '))
  Check (($tb.data | ForEach-Object { $_.balanceAfter }) -contains 649.5) 'source balanceAfter = 649.50' ("out.after=" + $tb.data[0].balanceAfter + " in.after=" + $tb.data[1].balanceAfter)
}

$b1 = ([double]((Call 'GET' ($base + '/api/accounts/' + $acc1.id + '/balance') $tokA $null).text | ConvertFrom-Json).data)
$b2 = ([double]((Call 'GET' ($base + '/api/accounts/' + $acc2.id + '/balance') $tokA $null).text | ConvertFrom-Json).data)
Check ($b1 -eq 649.5 -and $b2 -eq 250.5) 'post-transfer balances 649.50 / 250.50' ("acctA=" + $b1 + " acctB=" + $b2)

$hist = Call 'GET' ($base + '/api/accounts/' + $acc1.id + '/transactions') $tokA $null
$hb = $null
try { $hb = $hist.text | ConvertFrom-Json } catch {}
$types = ''
if ($hb -and $hb.data) { $types = (($hb.data | ForEach-Object { $_.transactionType }) -join ',') }
Check ($hist.status -eq 200 -and $hb.data.Count -eq 3) 'history has 3 rows (DEPOSIT, WITHDRAWAL, TRANSFER_OUT)' ("count=" + $(if ($hb) { $hb.data.Count } else { -1 }) + " types=" + $types)

# cross-customer isolation
$other = Call 'GET' ($base + '/api/accounts/' + $acc1.id) $tokB $null
Check ($other.status -eq 403) 'other customer reading A account -> 403 Forbidden' ("status=" + $other.status + " body=" + (Short $other.text 120))

# insufficient funds must roll back atomically
$over = Call 'POST' ($base + '/api/transfers') $tokA (J @{ sourceAccountNumber = $acc1.accountNumber; destinationAccountNumber = $acc2.accountNumber; amount = 999999.00 })
Check ($over.status -eq 400) 'transfer > balance -> 400 (no partial debit)' ("status=" + $over.status + " body=" + (Short $over.text 140))
$b1b = ([double]((Call 'GET' ($base + '/api/accounts/' + $acc1.id + '/balance') $tokA $null).text | ConvertFrom-Json).data)
Check ($b1b -eq 649.5) 'failed transfer rolled back (balance unchanged)' ("balance=" + $b1b)
$badTok = Call 'GET' ($base + '/api/accounts') 'not.a.jwt' $null
Check ($badTok.status -eq 401 -or $badTok.status -eq 403) 'malformed JWT rejected' ("status=" + $badTok.status)
}

# ---------- 5. database-level verification (real MySQL 8) ----------
Say '=== DB CHECKS ==='

$fly = Sql 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
Say '--- flyway_schema_history ---'
Say $fly
$flyLines = @($fly -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' })
$flyFailed = @($flyLines | Where-Object { $_ -notmatch '1$' })
Check (($flyLines.Count -ge 5) -and ($flyFailed.Count -eq 0)) 'Flyway applied V1..V5 with success=1' ("rows=" + $flyLines.Count + " failedRows=" + $flyFailed.Count)

$refOut = Sql 'SELECT COUNT(*), MIN(CHAR_LENGTH(reference_number)), MAX(CHAR_LENGTH(reference_number)), COUNT(DISTINCT reference_number) FROM transactions;'
Say ('--- transactions width summary (count, minLen, maxLen, distinctRefs): ' + $refOut)
$parts = @($refOut -split "`t")
Check ($parts.Count -ge 4 -and (Num $parts[1]) -eq 20 -and (Num $parts[2]) -eq 20) 'every persisted reference_number is exactly 20 chars (no truncation/oversize)' ("summary=" + $refOut)
Check ($parts.Count -ge 4 -and (Num $parts[0]) -eq (Num $parts[3])) 'unique reference_number holds in MySQL' ("summary=" + $refOut)
Check ($parts.Count -ge 4 -and (Num $parts[0]) -ge 4) 'this run persisted the expected transaction rows' ("rows=" + $(if ($parts.Count -ge 4) { $parts[0] } else { 'n/a' }))

$rows = Sql 'SELECT reference_number, transaction_type, amount, balance_before, balance_after FROM transactions ORDER BY id;'
Say '--- transactions rows (reference, type, amount, before, after) ---'
Say $rows
$rowLines = @($rows -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' })
$badRows = @($rowLines | Where-Object { $_ -notmatch '^TXN[0-9]{17}' })
Check ($rowLines.Count -gt 0 -and $badRows.Count -eq 0) 'all rows match ^TXN[0-9]{17} (20 chars) in the DB' ("rows=" + $rowLines.Count + " bad=" + $badRows.Count)

$balOut = Sql 'SELECT account_number, account_type, balance, status FROM accounts ORDER BY id DESC LIMIT 3;'
Say '--- newest accounts (number, type, balance, status) ---'
Say $balOut

$cnt = Sql 'SELECT (SELECT COUNT(*) FROM users) u, (SELECT COUNT(*) FROM customers) c, (SELECT COUNT(*) FROM accounts) a, (SELECT COUNT(*) FROM transactions) t;'
Say ('--- counts users/customers/accounts/transactions: ' + $cnt)

$nf = Sql 'SELECT COUNT(*) FROM accounts WHERE balance <> (SELECT COALESCE(SUM(CASE WHEN transaction_type IN (''DEPOSIT'',''TRANSFER_IN'') THEN amount ELSE -amount END),0) FROM transactions WHERE transactions.account_id = accounts.id);'
Say ('--- accounts whose balance disagrees with its transaction ledger: ' + $nf)
Check ((Num $nf) -eq 0) 'account balance == sum of its transaction ledger (audit integrity)' ("mismatches=" + $nf)

# ---------- 6. application log + report ----------
Say '=== APP LOG CHECKS ==='
$appLog = (& docker logs amanah_app 2>&1 | Out-String)
$errLines = @($appLog -split "`n" | Where-Object { $_ -match '\bERROR\b' })
Check ($errLines.Count -eq 0) 'no ERROR lines in app log' ("errors=" + $errLines.Count + " :: " + (@($errLines | Select-Object -First 3) -join ' | '))
Check ($appLog -match 'Started AmanahBankingApplication') 'Spring Boot startup completed' ''
Check ($appLog -match 'Successfully applied|Migrating schema') 'Flyway migrated schema on startup' ''
Check ($appLog -notmatch 'truncat') 'no data-truncation warnings in log' ''
Say ('--- flyway startup lines: ' + (@($appLog -split "`n" | Where-Object { $_ -match 'Successfully applied' }) -join ' | '))

$summary = New-Object System.Collections.ArrayList
[void]$summary.Add('AMANAH - end-to-end verification against live MySQL 8 (docker compose up --build)')
[void]$summary.Add('run at ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss') + '   target: ' + $base)
[void]$summary.Add('')
foreach ($line in $res) { [void]$summary.Add($line) }
[void]$summary.Add('')
[void]$summary.Add('-------------------------------------------------------------')
[void]$summary.Add('TOTAL: ' + ($pass + $fail) + '    PASS: ' + $pass + '    FAIL: ' + $fail)
[void]$summary.Add('-------------------------------------------------------------')
[void]$summary.Add('')
[void]$summary.Add('--- flyway_schema_history (version, description, success) ---')
[void]$summary.Add($fly)
[void]$summary.Add('')
[void]$summary.Add('--- transactions (reference_number, type, amount, balance_before, balance_after) ---')
[void]$summary.Add($rows)
[void]$summary.Add('')
[void]$summary.Add('--- newest accounts (number, type, balance, status) ---')
[void]$summary.Add($balOut)
[void]$summary.Add('')
[void]$summary.Add('--- row counts (users, customers, accounts, transactions) ---')
[void]$summary.Add($cnt)
[void]$summary.Add('')
[void]$summary.Add('--- test identities ---')
[void]$summary.Add('customers: ' + $uA + ' / ' + $uB + '    admin password that worked: ' + $adminPass)
Set-Content -Path $rep -Value $summary -Encoding utf8
Say ('=== SMOKE TEST DONE pass=' + $pass + ' fail=' + $fail + ' ===')



