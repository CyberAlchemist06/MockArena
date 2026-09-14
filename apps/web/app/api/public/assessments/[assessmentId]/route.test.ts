import { describe, expect, it, vi } from "vitest";
vi.mock("@/lib/api/server", () => ({ publicAssessmentFetch: vi.fn(), ServiceError: class ServiceError extends Error { constructor(public status: number, public code?: string) { super(); } } }));
import { publicAssessmentFetch } from "@/lib/api/server";
import { GET } from "./route";

describe("public assessment detail BFF", () => {
  it("forwards a safe encoded identifier and preserves a not-found response", async () => {
    vi.mocked(publicAssessmentFetch).mockResolvedValue(new Response(JSON.stringify({ code: "ASSESSMENT_NOT_FOUND" }), { status: 404 }));
    const response = await GET(new Request("http://web"), { params: Promise.resolve({ assessmentId: "missing/id" }) });
    expect(response.status).toBe(404);
    expect(publicAssessmentFetch).toHaveBeenCalledWith("/api/v1/public/assessments/missing%2Fid");
  });
});
