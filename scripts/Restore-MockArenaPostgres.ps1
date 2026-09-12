[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath,
    [switch]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'

$ContainerName = 'mockarena-postgres'
$DatabaseName = 'mockarena'
$DatabaseUser = 'mockarena'
$BackupDirectory = 'D:\MockArena\backups'
$BackupScript = 'D:\MockArena\scripts\Backup-MockArenaPostgres.ps1'

function Invoke-DockerChecked {
    param([string[]]$Arguments, [string]$FailureMessage, [switch]$SuppressOutput)
    if ($SuppressOutput) { & docker @Arguments | Out-Null } else { & docker @Arguments }
    if ($LASTEXITCODE -ne 0) { throw $FailureMessage }
}

function Assert-PostgresContainerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'Docker CLI is not available on PATH.' }
    $running = & docker container inspect --format '{{.State.Running}}' $ContainerName 2>$null
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL container '$ContainerName' does not exist." }
    if ($running.Trim() -ne 'true') { throw "PostgreSQL container '$ContainerName' is not running." }
    & docker exec $ContainerName pg_isready -U $DatabaseUser -d $DatabaseName | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL database '$DatabaseName' is not ready." }
}

if (-not $ConfirmRestore) {
    throw 'Restore requires the explicit -ConfirmRestore switch. No database changes were made.'
}

$ResolvedBackup = [System.IO.Path]::GetFullPath((Resolve-Path -LiteralPath $BackupPath -ErrorAction Stop).Path)
$ResolvedBackupRoot = [System.IO.Path]::GetFullPath($BackupDirectory).TrimEnd('\')
if (-not $ResolvedBackup.StartsWith("$ResolvedBackupRoot\", [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Backup must be located under $BackupDirectory. No database changes were made."
}
if ((Get-Item -LiteralPath $ResolvedBackup).Length -le 0) { throw 'Backup file is empty. No database changes were made.' }

Write-Warning 'Stop Question Service, Challenge Service, and any other application that can write to PostgreSQL before restoring. This script will not stop applications automatically.'
$Confirmation = Read-Host "Type exactly 'RESTORE mockarena' to replace database objects from '$ResolvedBackup'"
if ($Confirmation -cne 'RESTORE mockarena') { throw 'Restore confirmation did not match. No database changes were made.' }

Assert-PostgresContainerReady
$TemporaryPath = "/tmp/mockarena-restore-$([guid]::NewGuid().ToString('N')).dump"

try {
    Invoke-DockerChecked @('cp', $ResolvedBackup, "${ContainerName}:$TemporaryPath") 'Docker could not copy the selected backup to the PostgreSQL container.'
    Invoke-DockerChecked -Arguments @('exec', $ContainerName, 'pg_restore', '--list', $TemporaryPath) -FailureMessage 'pg_restore verification failed. No database changes were made.' -SuppressOutput

    Write-Host 'Creating an automatic pre-restore backup...'
    $PreRestoreBackup = & $BackupScript
    if ($LASTEXITCODE -ne 0 -or -not $PreRestoreBackup) { throw 'Pre-restore backup failed. Restore was not started.' }
    Write-Host "Pre-restore backup retained at: $PreRestoreBackup"

    Invoke-DockerChecked @('exec', $ContainerName, 'pg_restore', '--clean', '--if-exists', '--no-owner', '--no-privileges', '--exit-on-error', '-U', $DatabaseUser, '-d', $DatabaseName, $TemporaryPath) 'Restore failed. pg_restore stopped at the first error; the selected and pre-restore backups were retained.'
    Write-Host 'Restore completed successfully.'
}
finally {
    # This is the uniquely named temporary copy made by this script, not database data or a Docker volume.
    & docker exec $ContainerName rm -f -- $TemporaryPath 2>$null | Out-Null
}
