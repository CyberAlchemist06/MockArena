# Local PostgreSQL backup and restore

MockArena's local PostgreSQL container is `mockarena-postgres`. Its Docker volume persists data across container restarts, but a Docker volume is not a portable, auditable database backup. The supplied scripts create logical PostgreSQL backups with `pg_dump` in custom format, which can be inspected and restored with `pg_restore`.

## Backup

Run from PowerShell:

```powershell
& D:\MockArena\scripts\Backup-MockArenaPostgres.ps1
```

The script requires Docker, the exact `mockarena-postgres` container, a running container, and a ready `mockarena` database. It writes a verified custom-format backup to:

```text
D:\MockArena\backups\mockarena_yyyyMMdd_HHmmss.dump
D:\MockArena\backups\mockarena_yyyyMMdd_HHmmss.dump.sha256
```

It verifies that the dump is non-empty and that `pg_restore --list` can read it before retaining the file. The checksum sidecar is a SHA-256 checksum of the dump.

To inspect a backup manually without changing the database:

```powershell
docker cp D:\MockArena\backups\mockarena_yyyyMMdd_HHmmss.dump mockarena-postgres:/tmp/verify.dump
docker exec mockarena-postgres pg_restore --list /tmp/verify.dump
docker exec mockarena-postgres rm -f -- /tmp/verify.dump
```

## Restore

Restoring is destructive to database objects in the existing `mockarena` database. Stop Question Service, Challenge Service, and any other local writer first. The script warns but does not stop applications itself.

```powershell
& D:\MockArena\scripts\Restore-MockArenaPostgres.ps1 `
  -BackupPath D:\MockArena\backups\mockarena_yyyyMMdd_HHmmss.dump `
  -ConfirmRestore
```

The script accepts only a backup under `D:\MockArena\backups`, validates it with `pg_restore --list`, requires the exact interactive phrase `RESTORE mockarena`, checks PostgreSQL readiness, and creates a new pre-restore logical backup before it starts restoration.

It restores into the existing database with `pg_restore --clean --if-exists --no-owner --no-privileges --exit-on-error`. It does not delete or recreate the database, container, or Docker volume. The selected backup and automatic pre-restore backup are retained if restoration fails.

## Git exclusion

Backups can contain all local application data, Flyway history, and development credentials or test content. They are excluded through the repository-root `/backups/` rule in `.gitignore` and must never be committed.

## Limitations

- This is a local-development recovery workflow, not production backup/disaster-recovery design.
- It creates a full logical dump of `mockarena`; it does not provide point-in-time recovery, encryption, off-machine replication, retention automation, or managed secret storage.
- Restoring while applications write to the database can create inconsistent application behavior; stop writers first.
- `--exit-on-error` stops on the first restore failure, but PostgreSQL restore is not a distributed transaction. Use the automatically created pre-restore backup to recover if needed.
- The current Docker volume is anonymous. The workflow intentionally does not create or migrate to a named volume.
