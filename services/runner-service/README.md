# Local Java Sandbox Runner

This pet-project Runner Service is the only component that controls Docker Desktop for local Java execution. Build its image with:

```powershell
docker build -t mockarena-java-runner:21-v1 .\services\runner-service
```

Every candidate container is created without a network, ports, host mounts, Docker socket, capabilities, or privileges; it uses a non-root user, read-only root filesystem, tmpfs workspace, memory/CPU/PID limits, bounded output, and forced removal. Candidate source is copied through a random temporary directory with `docker cp`; it is never interpolated into shell commands or executed on Windows.

This is appropriate for local development and controlled demonstrations, not a hardened public arbitrary-code-execution platform. Production isolation remains deferred to the backlog.
