package com.mockarena.assessment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class AttemptEvaluationAfterCommitListener {
    private static final Logger log = LoggerFactory.getLogger(AttemptEvaluationAfterCommitListener.class);
    private final AttemptEvaluationService evaluation;

    public AttemptEvaluationAfterCommitListener(AttemptEvaluationService evaluation) { this.evaluation = evaluation; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AttemptSubmittedEvent event) {
        try {
            evaluation.evaluate(event.attemptId());
        } catch (RuntimeException exception) {
            // Submission has committed. Keep only an operational identifier and failure category in logs.
            log.warn("Post-submit evaluation deferred for attemptId={} failure={}", event.attemptId(), exception.getClass().getSimpleName());
        }
    }
}
