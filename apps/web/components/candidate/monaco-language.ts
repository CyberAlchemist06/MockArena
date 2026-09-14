const languages: Record<string, string> = {
  JAVA: "java",
  PYTHON: "python",
  JAVASCRIPT: "javascript",
  TYPESCRIPT: "typescript",
  C: "c",
  CPP: "cpp"
};

export function monacoLanguage(languageCode: string | null | undefined): string {
  return languageCode ? (languages[languageCode.trim().toUpperCase()] ?? "plaintext") : "plaintext";
}
