package com.mockarena.runner.application;

public final class NormalizedWhitespaceComparator {
  private NormalizedWhitespaceComparator() {}
  public static boolean matches(String actual, String expected) {
    return normalize(actual).equals(normalize(expected));
  }
  private static String normalize(String value) { return value.trim().replaceAll("\\s+", " "); }
}
