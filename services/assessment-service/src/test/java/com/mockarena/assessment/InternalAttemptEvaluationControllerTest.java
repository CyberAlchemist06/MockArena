package com.mockarena.assessment;

import com.mockarena.assessment.api.InternalAttemptEvaluationController;
import com.mockarena.assessment.application.AttemptEvaluationService;
import com.mockarena.assessment.domain.AttemptResult;
import com.mockarena.assessment.domain.EvaluationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalAttemptEvaluationController.class, properties = "assessment.security.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
class InternalAttemptEvaluationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AttemptEvaluationService evaluation;

    @Test void retriesOnlyByAttemptIdAndReturnsSafeOperationalMetadata() throws Exception {
        UUID attemptId = UUID.randomUUID();
        AttemptResult result = mock(AttemptResult.class);
        when(result.evaluationStatus()).thenReturn(EvaluationStatus.PARTIALLY_EVALUATED);
        when(evaluation.evaluate(attemptId)).thenReturn(result);

        mvc.perform(post("/internal/v1/attempts/{attemptId}/evaluate", attemptId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
            .andExpect(jsonPath("$.evaluationStatus").value("PARTIALLY_EVALUATED"))
            .andExpect(jsonPath("$.correctOptionId").doesNotExist())
            .andExpect(jsonPath("$.sourceCode").doesNotExist())
            .andExpect(jsonPath("$.scoringPolicy").doesNotExist());
        verify(evaluation).evaluate(attemptId);
    }
}
