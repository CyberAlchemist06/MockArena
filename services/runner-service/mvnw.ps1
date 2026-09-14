$ErrorActionPreference = 'Stop'
$wrapperRoot = Join-Path $PSScriptRoot '.mvn\wrapper'
$mavenVersion = '3.9.9'; $archive = Join-Path $wrapperRoot "apache-maven-$mavenVersion-bin.zip"; $mavenHome = Join-Path $wrapperRoot "apache-maven-$mavenVersion"
if (-not (Test-Path $mavenHome)) { New-Item -ItemType Directory -Force -Path $wrapperRoot | Out-Null; if (-not (Test-Path $archive)) { Invoke-WebRequest "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip" -OutFile $archive }; Expand-Archive -Path $archive -DestinationPath $wrapperRoot -Force }
& (Join-Path $mavenHome 'bin\mvn.cmd') @args
exit $LASTEXITCODE
