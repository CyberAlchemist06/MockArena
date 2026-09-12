[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$ContainerName = 'mockarena-postgres'
$DatabaseName = 'mockarena'
$DatabaseUser = 'mockarena'
$BackupDirectory = 'D:\MockArena\backups'
$Timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$BackupName = "mockarena_$Timestamp.dump"
$BackupPath = Join-Path $BackupDirectory $BackupName
$PartialPath = "$BackupPath.partial"
$ContainerTemporaryPath = "/tmp/mockarena-backup-$Timestamp.dump"

function Invoke-DockerChecked {
    param([string[]]$Arguments, [string]$FailureMessage, [switch]$SuppressOutput)
    if ($SuppressOutput) { & docker @Arguments | Out-Null } else { & docker @Arguments }
    if ($LASTEXITCODE -ne 0) { throw $FailureMessage }
}

function Assert-PostgresContainerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI is not available on PATH.'
    }

    $running = & docker container inspect --format '{{.State.Running}}' $ContainerName 2>$null
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL container '$ContainerName' does not exist." }
    if ($running.Trim() -ne 'true') { throw "PostgreSQL container '$ContainerName' is not running." }

    & docker exec $ContainerName pg_isready -U $DatabaseUser -d $DatabaseName | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL database '$DatabaseName' is not ready." }
}

Assert-PostgresContainerReady
New-Item -ItemType Directory -Force -Path $BackupDirectory | Out-Null
if (Test-Path -LiteralPath $BackupPath) { throw "Refusing to overwrite an existing backup: $BackupPath" }

try {
    Invoke-DockerChecked @('exec', $ContainerName, 'pg_dump', '--format=custom', '--no-owner', '--no-privileges', '-U', $DatabaseUser, '-d', $DatabaseName, '-f', $ContainerTemporaryPath) "pg_dump failed."
    Invoke-DockerChecked -Arguments @('exec', $ContainerName, 'pg_restore', '--list', $ContainerTemporaryPath) -FailureMessage 'pg_restore verification failed for the temporary backup.' -SuppressOutput
    Invoke-DockerChecked @('cp', "${ContainerName}:$ContainerTemporaryPath", $PartialPath) 'Docker could not copy the backup from the PostgreSQL container.'

    if (-not (Test-Path -LiteralPath $PartialPath) -or (Get-Item -LiteralPath $PartialPath).Length -le 0) {
        throw 'The copied backup is missing or empty.'
    }

    Move-Item -LiteralPath $PartialPath -Destination $BackupPath
    $Hash = (Get-FileHash -LiteralPath $BackupPath -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath "$BackupPath.sha256" -Value "$Hash  *$BackupName" -NoNewline
    Write-Host "Backup created and verified: $BackupPath"
    Write-Output $BackupPath
}
finally {
    if (Test-Path -LiteralPath $PartialPath) { Remove-Item -LiteralPath $PartialPath -Force }
    # This is the uniquely named temporary dump made by this script, not database data or a Docker volume.
    & docker exec $ContainerName rm -f -- $ContainerTemporaryPath 2>$null | Out-Null
}
