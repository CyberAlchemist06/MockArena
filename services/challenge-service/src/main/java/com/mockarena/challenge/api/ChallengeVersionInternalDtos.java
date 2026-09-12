package com.mockarena.challenge.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class ChallengeVersionInternalDtos {
    private ChallengeVersionInternalDtos() { }
    public record ResolveRequest(@NotEmpty List<@NotNull UUID> challengeVersionIds) { }
    public record Entry(UUID challengeId, UUID challengeVersionId, int versionNumber, String status) { }
    public record ResolveResponse(List<Entry> entries) { }
}
