package com.mockarena.assessment.domain;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="attempt_submit_requests", schema="assessment") public class AttemptSubmitRequest {
 @Id private UUID id; @Column(nullable=false) private UUID attemptId; @Column(nullable=false) private UUID candidateUserId; @Column(nullable=false,length=200) private String idempotencyKey; @Column(nullable=false) private Instant submittedAt;
 protected AttemptSubmitRequest(){} public AttemptSubmitRequest(UUID id,UUID attemptId,UUID candidateUserId,String key,Instant at){this.id=id;this.attemptId=attemptId;this.candidateUserId=candidateUserId;idempotencyKey=key;submittedAt=at;}
 public UUID attemptId(){return attemptId;} public UUID candidateUserId(){return candidateUserId;} public String idempotencyKey(){return idempotencyKey;} public Instant submittedAt(){return submittedAt;}
}
