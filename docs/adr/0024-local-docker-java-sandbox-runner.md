# ADR 0024: Local Docker Java Sandbox Runner

## Context

Coding source is hostile input. Evaluation, Assessment, Question, and the web application must not execute it. MockArena needs one local Java assessment path without claiming production-grade arbitrary-code isolation.

## Decision

Use a separate Runner Service which alone controls Docker Desktop. It accepts workload-authenticated internal Java V1 requests and launches one ephemeral Linux container per job using the platform-controlled `javac Main.java` and `java Main` commands. Source reaches the container through `docker cp` from a random temporary runner-host directory; it is never a shell argument, bind mount, log field, or host process.

The candidate container uses `--network none`, no published ports, `--cap-drop ALL`, `no-new-privileges`, read-only root, tmpfs workspace, non-root UID 10001, CPU, memory, PID, timeout, and output ceilings. It has no Docker socket, secrets, application source, host mounts, or runtime-controlled commands. It is forcibly removed in all completion paths.

Evaluation retains only runner-derived facts and does not persist source or hidden tests. Infrastructure failures retry through its durable job model; candidate compile/runtime/timeout/output failures become ready for later result application and are never automatically treated as infrastructure scoring outcomes.

## Consequences

This is suitable only for local development and controlled demonstrations. Docker daemon access remains isolated to the private Runner controller and must never be granted to candidate containers or public services. A future production design must use stronger workload identity and isolation (for example gVisor, Kata, or microVMs), plus independent security review.
