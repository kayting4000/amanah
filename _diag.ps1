$o = Join-Path $env:TEMP 'amanah-diag.txt'
$root = 'c:\Users\MSU\Desktop\amanah'
$L = @()
$L += 'now ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

$L += '--- local toolchain ---'
foreach ($c in @('java', 'javac', 'mvn', 'mvnw')) {
  $p = (Get-Command $c -ErrorAction SilentlyContinue | Select-Object -First 1).Source
  if ($p) { $L += ($c + ' -> ' + $p) } else { $L += ($c + ' -> NOT FOUND ON PATH') }
}
$L += 'JAVA_HOME=' + $env:JAVA_HOME
$L += 'JAVA_HOME exists=' + (Test-Path $env:JAVA_HOME)

$L += '--- local maven repo ---'
$m2 = Join-Path $env:USERPROFILE '.m2'
$L += ('~/.m2 exists=' + (Test-Path $m2))
if (Test-Path $m2) {
  $L += ((Get-ChildItem $m2 -Force | Select-Object Name | Out-String -Width 120)).Trim()
  $rep = Join-Path $m2 'repository'
  $n = @(Get-ChildItem $rep -Recurse -Filter *.jar -ErrorAction SilentlyContinue).Count
  $L += ('jars in ~/.m2/repository = ' + $n)
} else {
  $L += 'no local repository at all'
}

$L += '--- project import helpers ---'
$L += ('mvnw present=' + (Test-Path ($root + '\mvnw')))
$L += ('.mvn dir present=' + (Test-Path ($root + '\.mvn')))
$L += ('.vscode present=' + (Test-Path ($root + '\.vscode')))
if (Test-Path ($root + '\.vscode')) {
  $L += ((Get-ChildItem ($root + '\.vscode') -Force | Select-Object Name | Out-String -Width 120)).Trim()
  $sf = $root + '\.vscode\settings.json'
  if (Test-Path $sf) { $L += '--- settings.json ---'; $L += (Get-Content $sf -Raw) }
}
$L += ('target/classes present=' + (Test-Path ($root + '\target\classes')))

$L += '--- VS Code log sessions (newest 3) ---'
$logs = Join-Path $env:APPDATA 'Code\logs'
if (Test-Path $logs) {
  $L += ((Get-ChildItem $logs -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 3 Name, LastWriteTime | Format-Table -AutoSize | Out-String -Width 140)).Trim()
  $L += '--- jdt.ls logs ---'
  $jl = @(Get-ChildItem $logs -Recurse -Filter '*jdt.ls*' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 6)
  foreach ($f in $jl) { $L += ($f.FullName + '  (' + $f.Length + ' B, ' + $f.LastWriteTime + ')') }
  foreach ($f in ($jl | Where-Object { $_.Name -like '*.log' } | Select-Object -First 1)) {
    $L += '--- tail of ' + $f.Name + ' ---'
    $L += ((Get-Content $f.FullName -Tail 60 -ErrorAction SilentlyContinue) -join "`n")
  }
} else {
  $L += 'no VS Code logs dir'
}

$L += '--- java workspace storage caches ---'
$ws = Join-Path $env:APPDATA 'Code\User\workspaceStorage'
if (Test-Path $ws) {
  $cps = @(Get-ChildItem $ws -Recurse -Filter '.classpath' -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 4)
  $L += ('classpath files found=' + $cps.Count)
  foreach ($f in $cps) {
    $L += 'FILE ' + $f.FullName + '  (' + $f.LastWriteTime + ')'
    $L += ((Get-Content $f.FullName -Raw))
  }
  $L += '--- workspaceStorage dirs ---'
  $L += ((Get-ChildItem $ws -Directory -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 5 Name, LastWriteTime | Format-Table -AutoSize | Out-String -Width 140)).Trim()
} else {
  $L += 'no workspaceStorage'
}
Set-Content -Path $o -Value $L -Encoding utf8
