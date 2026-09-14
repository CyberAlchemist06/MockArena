package com.mockarena.evaluation.infrastructure;
public class RemoteDependencyException extends RuntimeException { private final String category; public RemoteDependencyException(String category,Throwable cause){super(category,cause);this.category=category;} public String category(){return category;} }
