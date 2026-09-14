import { describe, expect, it } from "vitest";
import { monacoLanguage } from "./monaco-language";

describe("monaco language mapping", () => {
  it("maps supported candidate language codes", () => {
    expect(monacoLanguage("JAVA")).toBe("java");
    expect(monacoLanguage("python")).toBe("python");
    expect(monacoLanguage("CPP")).toBe("cpp");
  });
  it("uses safe plaintext for unsupported codes", () => expect(monacoLanguage("COBOL")).toBe("plaintext"));
});
