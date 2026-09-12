package com.mockarena.assessment.infrastructure.challenge;
public class ChallengeServiceUnavailableException extends RuntimeException { public ChallengeServiceUnavailableException() { super("Challenge service is unavailable"); } public ChallengeServiceUnavailableException(Throwable cause) { super("Challenge service is unavailable", cause); } }
