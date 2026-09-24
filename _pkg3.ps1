$root = 'c:\Users\MSU\Desktop\amanah'
$o = (& docker run --rm -v amanah-m2:/root/.m2 -v 'c:/Users/MSU/Desktop/amanah:/app' -w /app `
  maven:3.9.6-eclipse-temurin-21 mvn -B clean package 2>&1 | Out-String)
Set-Content -Path ($root + '\pkg3.log') -Value $o -Encoding utf8

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipPath = $root + '\target\banking-1.0.0.jar'
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
$entries = @($zip.Entries | Where-Object { $_.FullName -match 'db/migration' } | ForEach-Object { $_.FullName })
$zip.Dispose()

$L = @()
$L += '=== fat jar migration entries ==='
$L += ($entries -join "`n")
$L += '=== fat jar size (bytes) ==='
$L += (Get-Item $zipPath).Length
$L += '=== build result ==='
$L += (@($o -split "`r?`n" | Select-String -Pattern 'Tests run:.*Failures|BUILD SUCCESS|BUILD FAILURE|Building jar|Total time' | Select-Object -Last 14) | ForEach-Object { $_.ToString() }) -join "`n"
Set-Content -Path (Join-Path $env:TEMP 'amanah-pkg3.txt') -Value $L -Encoding utf8
