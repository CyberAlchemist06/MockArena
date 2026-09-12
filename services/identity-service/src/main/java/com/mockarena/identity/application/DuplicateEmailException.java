package com.mockarena.identity.application;
public class DuplicateEmailException extends RuntimeException { public DuplicateEmailException(){super("Email is already registered");} }
