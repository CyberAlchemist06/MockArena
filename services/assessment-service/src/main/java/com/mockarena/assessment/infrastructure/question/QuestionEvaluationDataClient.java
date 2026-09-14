package com.mockarena.assessment.infrastructure.question; import java.util.*; public interface QuestionEvaluationDataClient{List<McqEvaluationData> resolve(List<UUID> questionVersionIds);}
