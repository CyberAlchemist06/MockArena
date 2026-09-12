package com.mockarena.assessment.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name = "attempt_entitlement_reconciliations", schema = "assessment")
public class AttemptEntitlementReconciliation {
    @Id private UUID id; @Column(nullable = false) private UUID attemptId; @Column(nullable = false) private UUID reservationId; @Column(nullable = false) private String capabilityCode; @Column(nullable = false) private String state; @Column(nullable = false) private Instant createdAt; @Column(nullable = false) private Instant updatedAt;
    protected AttemptEntitlementReconciliation() { }
    public AttemptEntitlementReconciliation(UUID id, UUID attemptId, UUID reservationId, String capabilityCode, Instant now) { this.id=id; this.attemptId=attemptId; this.reservationId=reservationId; this.capabilityCode=capabilityCode; state="PENDING_COMMIT"; createdAt=now; updatedAt=now; }
    public UUID attemptId() { return attemptId; } public UUID reservationId() { return reservationId; } public String state() { return state; } public void committed(Instant now) { state="COMMITTED"; updatedAt=now; }
}
