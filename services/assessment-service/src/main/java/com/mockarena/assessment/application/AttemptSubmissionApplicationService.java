package com.mockarena.assessment.application;

import com.mockarena.assessment.domain.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service public class AttemptSubmissionApplicationService {
    private final AttemptRepository attempts; private final AttemptSubmitRequestRepository requests; private final AttemptItemRepository items;
    private final AttemptItemResponseRepository responses; private final SubmittedCodingResponseSnapshotRepository snapshots;
    private final CodingEvaluationOutboxRepository outbox; private final ApplicationEventPublisher events; private final Clock clock=Clock.systemUTC();
    public AttemptSubmissionApplicationService(AttemptRepository attempts, AttemptSubmitRequestRepository requests, AttemptItemRepository items, AttemptItemResponseRepository responses, SubmittedCodingResponseSnapshotRepository snapshots, CodingEvaluationOutboxRepository outbox, ApplicationEventPublisher events) { this.attempts=attempts; this.requests=requests; this.items=items; this.responses=responses; this.snapshots=snapshots; this.outbox=outbox; this.events=events; }
    @Transactional public Submission submit(UUID id,UUID candidate,String key){
        if(key==null||key.isBlank()||key.length()>200)throw new IllegalArgumentException("Idempotency-Key is required");
        Attempt attempt=attempts.findByIdForUpdate(id).orElseThrow(AttemptNotFoundException::new); if(!attempt.candidateUserId().equals(candidate))throw new SecurityException("Attempt is not owned by current user");
        AttemptSubmitRequest replay=requests.findByAttemptIdAndIdempotencyKey(id,key.trim()).orElse(null); if(replay!=null){if(!replay.candidateUserId().equals(candidate))throw new IdempotencyKeyReusedException();return new Submission(attempt.id(),attempt.status().name(),replay.submittedAt(),attempt.deadlineAt());}
        Instant now=clock.instant(); if(attempt.expireIfDue(now)){attempts.saveAndFlush(attempt);throw new IllegalStateException("Attempt is not in progress");}
        attempt.submit(now); attempts.saveAndFlush(attempt); requests.saveAndFlush(new AttemptSubmitRequest(UUID.randomUUID(),id,candidate,key.trim(),now)); snapshotCodingResponsesAndQueue(id,now); events.publishEvent(new AttemptSubmittedEvent(id)); return new Submission(attempt.id(),attempt.status().name(),attempt.submittedAt(),attempt.deadlineAt());
    }
    private void snapshotCodingResponsesAndQueue(UUID attemptId,Instant submittedAt){Map<Integer,AttemptItemResponse> saved=new HashMap<>();responses.findByAttemptIdOrderByGlobalPositionAsc(attemptId).forEach(response->saved.put(response.globalPosition(),response));for(AttemptItem item:items.findByAttemptIdOrderByGlobalPositionAsc(attemptId)){if(!"CODING".equals(item.questionTypeCode()))continue;AttemptItemResponse response=saved.get(item.globalPosition());if(response!=null&&!"CODING".equals(response.questionTypeCode()))throw new IllegalStateException("Response type does not match attempt item");SubmittedCodingResponseSnapshot snapshot=response==null?SubmittedCodingResponseSnapshot.unanswered(item,submittedAt):SubmittedCodingResponseSnapshot.answered(item,response,sourceFingerprint(response.responsePayload().path("sourceCode").asText()),submittedAt);snapshots.save(snapshot);outbox.save(new CodingEvaluationOutbox(UUID.randomUUID(),snapshot,submittedAt));}}
    private static String sourceFingerprint(String source){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw new IllegalStateException("Unable to fingerprint submitted coding source",exception);}}
    public record Submission(UUID attemptId,String status,Instant submittedAt,Instant deadlineAt){}
}
