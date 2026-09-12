package com.mockarena.assessment.application;

import java.util.UUID;

/** Replaceable boundary for the future Commerce and Entitlements context. */
public interface AttemptStartEntitlementPort {
    UUID reserve(UUID subject, String capabilityCode, String idempotencyKey);
    void commit(UUID reservationId);
    void release(UUID reservationId);
}
