package com.mockarena.assessment.application;
public class IdempotencyKeyReusedException extends RuntimeException { public IdempotencyKeyReusedException() { super("Idempotency-Key was already used for another request"); } }
