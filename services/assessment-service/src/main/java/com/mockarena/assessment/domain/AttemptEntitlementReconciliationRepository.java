package com.mockarena.assessment.domain;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AttemptEntitlementReconciliationRepository extends JpaRepository<AttemptEntitlementReconciliation, UUID> { Optional<AttemptEntitlementReconciliation> findByReservationId(UUID reservationId); Optional<AttemptEntitlementReconciliation> findByAttemptId(UUID attemptId); }
