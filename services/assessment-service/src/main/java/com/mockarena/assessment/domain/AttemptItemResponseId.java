package com.mockarena.assessment.domain;

import java.io.Serializable;
import java.util.UUID;

public record AttemptItemResponseId(UUID attemptId, int globalPosition) implements Serializable { }
