import { describe, expect, it, vi } from "vitest";
vi.mock("@/lib/api/server", () => ({ assessmentFetch: vi.fn() }));
import { assessmentFetch } from "@/lib/api/server";
import { GET } from "./route";

describe("attempt-result BFF", () => {
  it("forwards only the server-side Assessment request and preserves safe states", async () => {
    vi.mocked(assessmentFetch).mockResolvedValue(new Response(JSON.stringify({ attemptId:"a", evaluationStatus:"PENDING", items:[] }), { status: 200 }));
    const response = await GET(new Request("http://web/api/attempts/a/result"), { params: Promise.resolve({ attemptId:"a" }) });
    expect(response.status).toBe(200);
    expect(assessmentFetch).toHaveBeenCalledWith("/api/v1/attempts/a/result");
    expect(await response.json()).not.toHaveProperty("correctOptionId");
  });

  it("preserves the safe not-released response without contacting Question Service", async () => {
    vi.mocked(assessmentFetch).mockResolvedValue(new Response(JSON.stringify({ code:"RESULT_NOT_RELEASED", message:"Result is not available yet" }), { status: 409 }));
    const response = await GET(new Request("http://web/api/attempts/a/result"), { params: Promise.resolve({ attemptId:"a" }) });
    expect(response.status).toBe(409);
    expect((await response.json()).code).toBe("RESULT_NOT_RELEASED");
  });
});
