package com.mockarena.challenge.application;
public class InsufficientQuestionsException extends RuntimeException { public InsufficientQuestionsException() { super("The question catalog did not contain enough matching questions"); } }
