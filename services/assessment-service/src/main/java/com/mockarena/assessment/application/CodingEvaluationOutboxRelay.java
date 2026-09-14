package com.mockarena.assessment.application;

import com.mockarena.assessment.domain.*;
import org.springframework.beans.factory.annotation.*; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.client.*;
import java.time.*; import java.util.*;

/** PostgreSQL-backed at-least-once relay. It ships references/fingerprints, never candidate source. */
@Service @ConditionalOnProperty(prefix="assessment.coding-evaluation-relay",name="enabled",havingValue="true")
public class CodingEvaluationOutboxRelay {
 private final CodingEvaluationOutboxRepository outbox; private final SubmittedCodingResponseSnapshotRepository snapshots; private final RestClient client; private final String token; private final String owner=UUID.randomUUID().toString(); private final Clock clock=Clock.systemUTC();
 public CodingEvaluationOutboxRelay(CodingEvaluationOutboxRepository outbox,SubmittedCodingResponseSnapshotRepository snapshots,@Value("${evaluation-service.base-url}")String base,@Value("${evaluation-service.workload-token:}")String token){if(token==null||token.isBlank())throw new IllegalStateException("Evaluation workload token is required when coding relay is enabled");this.outbox=outbox;this.snapshots=snapshots;this.client=RestClient.builder().baseUrl(base).build();this.token=token;}
 @Scheduled(fixedDelayString="${assessment.coding-evaluation-relay.fixed-delay-ms:5000}") public void poll(){for(CodingEvaluationOutbox e:claim(20)){try{SubmittedCodingResponseSnapshot s=snapshots.findById(new SubmittedCodingResponseSnapshotId(e.attemptId(),e.globalPosition())).orElseThrow();client.post().uri("/internal/v1/coding-evaluation-jobs").header("X-MockArena-Workload-Token",token).body(new Delivery(e.attemptId(),e.globalPosition(),s.questionId(),e.questionVersionId(),e.sourceFingerprint(),s.programmingLanguage())).retrieve().toBodilessEntity();delivered(e.outboxId());}catch(RuntimeException ignored){retry(e.outboxId());}}}
 @Transactional public List<CodingEvaluationOutbox> claim(int limit){Instant now=clock.instant();List<CodingEvaluationOutbox> rows=outbox.claimable(now,limit);rows.forEach(e->e.claim(owner,now.plusSeconds(30),now));return List.copyOf(rows);}
 @Transactional public void delivered(UUID id){outbox.findById(id).ifPresent(e->e.delivered(clock.instant()));}
 @Transactional public void retry(UUID id){outbox.findById(id).ifPresent(e->{int exponent=Math.min(6,e.deliveryAttempts());e.retry(clock.instant().plusSeconds(Math.min(300,1L<<exponent)),clock.instant());});}
 record Delivery(UUID attemptId,int globalPosition,UUID questionId,UUID questionVersionId,String submittedResponseFingerprint,String programmingLanguage){}
}
