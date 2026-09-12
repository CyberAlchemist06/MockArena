package com.mockarena.assessment.application;

import org.springframework.stereotype.Component;
import java.util.*;

/** Local-only quota adapter. It deliberately contains no pricing or payment logic. */
@Component
public class DevelopmentAttemptStartEntitlementAdapter implements AttemptStartEntitlementPort {
    private static final int COMPLIMENTARY_STARTS = 3;
    private final Map<UUID, Integer> committed = new HashMap<>();
    private final Map<UUID, UUID> reservations = new HashMap<>();
    @Override public synchronized UUID reserve(UUID subject, String capabilityCode, String idempotencyKey) {
        if (!"assessment.attempt.start".equals(capabilityCode) || committed.getOrDefault(subject, 0) + reservations.values().stream().filter(subject::equals).count() >= COMPLIMENTARY_STARTS) throw new AttemptStartEntitlementDeniedException();
        UUID reservation = UUID.randomUUID(); reservations.put(reservation, subject); return reservation;
    }
    @Override public synchronized void commit(UUID reservationId) { UUID subject = reservations.remove(reservationId); if (subject == null) return; committed.merge(subject, 1, Integer::sum); }
    @Override public synchronized void release(UUID reservationId) { reservations.remove(reservationId); }
}
