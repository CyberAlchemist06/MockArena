import { describe, expect, it, vi } from "vitest";
vi.mock("@/lib/api/server", () => ({ assessmentFetch: vi.fn() }));
import { assessmentFetch } from "@/lib/api/server";
import { POST } from "./route";
describe("attempt-submit BFF", () => {
  it("uses the same-origin request key and never exposes a service URL", async () => {
    vi.mocked(assessmentFetch).mockResolvedValue(new Response(JSON.stringify({ attemptId:"a",status:"SUBMITTED",submittedAt:"2026-01-01T00:00:00Z",deadlineAt:null }),{status:200}));
    const response=await POST(new Request("http://web/api/attempts/a/submit",{method:"POST",headers:{"Idempotency-Key":"submit-key"}}),{params:Promise.resolve({attemptId:"a"})});
    expect(response.status).toBe(200);expect(assessmentFetch).toHaveBeenCalledWith("/api/v1/attempts/a/submit",expect.objectContaining({headers:{"Idempotency-Key":"submit-key"}}));expect(await response.json()).not.toHaveProperty("token");
  });
});
