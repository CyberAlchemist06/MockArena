import { describe, expect, it } from "vitest";
import { safeReturnTo } from "./return-to";
describe("safeReturnTo", () => { it("permits local paths", () => expect(safeReturnTo("/assessments/a/take?version=1")).toBe("/assessments/a/take?version=1")); it("rejects external and protocol-relative targets", () => { expect(safeReturnTo("https://evil.example", "/candidate")).toBe("/candidate"); expect(safeReturnTo("//evil.example", "/candidate")).toBe("/candidate"); expect(safeReturnTo("\\evil", "/candidate")).toBe("/candidate"); }); });
