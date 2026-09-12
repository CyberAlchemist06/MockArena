package com.mockarena.challenge.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
public interface ChallengeVersionRepository extends JpaRepository<ChallengeVersion, UUID> { Optional<ChallengeVersion> findByChallengeIdAndVersionNumber(UUID challengeId, int versionNumber); }
