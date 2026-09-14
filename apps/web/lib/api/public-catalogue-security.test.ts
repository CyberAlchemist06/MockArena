import { existsSync, readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

describe("public catalogue client boundary", () => {
  it("contains no demo UUID, fixture, or public backend environment variable", () => {
    const root = process.cwd();
    expect(existsSync(`${root}/lib/fixtures/public-catalogue.ts`)).toBe(false);
    const source = readFileSync(`${root}/.env.example`, "utf8");
    expect(source).not.toContain("MOCKARENA_DEMO_ASSESSMENT");
    expect(source).not.toContain("NEXT_PUBLIC_");
    expect(readFileSync(`${root}/components/catalogue/catalogue-browser.tsx`, "utf8")).not.toContain("00000000-0000-0000-0000-000000000001");
  });
});
