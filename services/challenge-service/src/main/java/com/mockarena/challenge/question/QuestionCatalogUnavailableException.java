package com.mockarena.challenge.question;
public class QuestionCatalogUnavailableException extends RuntimeException {
    public QuestionCatalogUnavailableException() { super("Question catalog is unavailable"); }
    public QuestionCatalogUnavailableException(Throwable cause) { super("Question catalog is unavailable", cause); }
}
