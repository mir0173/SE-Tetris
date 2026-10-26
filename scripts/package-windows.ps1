$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    throw 'JAVA_HOME must point to a JDK 21 or newer installation.'
}
$jpackagePath = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackagePath)) {
    throw "jpackage was not found: $jpackagePath"
}

Push-Location -LiteralPath $projectRoot
try {
    & .\mvnw.cmd clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }

    $packageArguments = @(
        '--type', 'app-image',
        '--name', 'SE-Tetris',
        '--app-version', '1.0.0',
        '--vendor', 'SE-Tetris Team',
        '--description', 'Text-based Tetris game',
        '--input', 'target\app',
        '--main-jar', 'SE-Tetris.jar',
        '--main-class', 'tetris.Launcher',
        '--dest', 'target\dist',
        '--icon', 'src\main\resources\icons\se-tetris.ico',
        '--add-modules', 'java.desktop,java.logging,java.xml,jdk.unsupported',
        '--java-options', '-Xmx256m'
    )
    & $jpackagePath @packageArguments
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }

    $appImagePath = Join-Path $projectRoot 'target\dist\SE-Tetris'
    $zipPath = Join-Path $projectRoot 'target\dist\SE-Tetris-windows.zip'
    Compress-Archive -LiteralPath $appImagePath -DestinationPath $zipPath
    Write-Output "Distribution: $zipPath"
} finally {
    Pop-Location
}
