package com.mockarena.assessment.application;
import com.mockarena.assessment.domain.*; import org.junit.jupiter.api.Test; import java.time.Instant; import java.util.*; import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
class AttemptSubmissionApplicationServiceTest {
 @Test void serializes_submit_replay_and_later_response_locking_without_remote_clients(){
  AttemptRepository attempts=mock(AttemptRepository.class);AttemptSubmitRequestRepository requests=mock(AttemptSubmitRequestRepository.class);AttemptSubmissionApplicationService service=new AttemptSubmissionApplicationService(attempts,requests);UUID id=UUID.randomUUID(),user=UUID.randomUUID();Attempt attempt=new Attempt(id,UUID.randomUUID(),UUID.randomUUID(),1,user,Instant.now(),Instant.now().plusSeconds(300),null);
  when(attempts.findByIdForUpdate(id)).thenReturn(Optional.of(attempt));when(requests.findByAttemptIdAndIdempotencyKey(id,"key")).thenReturn(Optional.empty());when(attempts.saveAndFlush(any())).thenAnswer(x->x.getArgument(0));
  var first=service.submit(id,user,"key");assertThat(first.status()).isEqualTo("SUBMITTED");assertThat(first.submittedAt()).isNotNull();verify(requests).saveAndFlush(any(AttemptSubmitRequest.class));
  when(requests.findByAttemptIdAndIdempotencyKey(id,"key")).thenReturn(Optional.of(new AttemptSubmitRequest(UUID.randomUUID(),id,user,"key",first.submittedAt())));var replay=service.submit(id,user,"key");assertThat(replay.submittedAt()).isEqualTo(first.submittedAt());verify(attempts,times(1)).saveAndFlush(any());
  assertThatThrownBy(()->service.submit(id,user,"other")).isInstanceOf(IllegalStateException.class);assertThat(attempt.status()).isEqualTo(AttemptStatus.SUBMITTED);
 }
}
