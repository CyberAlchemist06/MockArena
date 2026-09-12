package com.mockarena.challenge.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ChallengeVersionRepository extends JpaRepository<ChallengeVersion, UUID> { }
