package com.mockarena.challenge.application;
public class InsufficientQuestionsException extends RuntimeException {
    public InsufficientQuestionsException() { super("The question catalog did not contain enough matching questions"); }
    public InsufficientQuestionsException(int groupIndex, String type, int requested, int eligible) { super("selectionGroupIndex=" + groupIndex + ", questionTypeCode=" + type + ", requestedCount=" + requested + ", eligibleUniqueCount=" + eligible); }
}
