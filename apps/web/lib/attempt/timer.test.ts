import { describe, expect, it } from "vitest";
import { formatRemaining, remainingSeconds } from "./timer";
describe("attempt timer", () => { it("never returns a negative display duration", () => expect(remainingSeconds("2026-01-01T00:00:00Z", Date.parse("2026-01-01T00:00:10Z"))).toBe(0)); it("formats a remaining duration", () => expect(formatRemaining(65)).toBe("01:05")); });
