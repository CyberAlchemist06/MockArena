package com.mockarena.challenge.application;

public class UncomposableChallengeVersionException extends RuntimeException {
    public UncomposableChallengeVersionException() { super("Challenge version is not published and composable"); }
}
