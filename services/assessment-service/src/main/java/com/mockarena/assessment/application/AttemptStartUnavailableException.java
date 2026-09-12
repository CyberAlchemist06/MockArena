package com.mockarena.assessment.application;
public class AttemptStartUnavailableException extends RuntimeException { public AttemptStartUnavailableException() { super("Attempt start is unavailable"); } public AttemptStartUnavailableException(Throwable cause) { super("Attempt start is unavailable", cause); } }
