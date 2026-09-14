# Evaluation Service

Evaluation Service accepts source-free coding evaluation jobs from Assessment and stores idempotent durable job state in its own `evaluation` schema. It retrieves immutable submitted source snapshots from Assessment and protected historical coding specifications from Question only with workload credentials. It verifies identity and SHA-256 source fingerprints, then leaves answered Java V1 jobs at `READY_FOR_RUNNER`; no candidate source is executed in this service.

Internal routes require `X-MockArena-Workload-Token`. With security enabled, Evaluation fails closed unless incoming, Assessment snapshot, and Question coding workload tokens are configured outside source control. Production must add workload identity/mTLS before any runner is enabled.
