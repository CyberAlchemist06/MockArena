import { describe, expect, it, vi } from "vitest";
vi.mock("@/lib/api/server", () => ({ publicAssessmentFetch: vi.fn(), ServiceError: class ServiceError extends Error { constructor(public status: number, public code?: string) { super(); } } }));
import { publicAssessmentFetch } from "@/lib/api/server";
import { GET } from "./route";

describe("public catalogue BFF", () => {
  it("forwards only supported anonymous query parameters without authorization", async () => {
    vi.mocked(publicAssessmentFetch).mockResolvedValue(new Response(JSON.stringify({ items: [], nextCursor: null }), { status: 200 }));
    const response = await GET(new Request("http://web/api/public/assessments?q=java&availability=OPEN&pageSize=2&ignored=secret"));
    expect(response.status).toBe(200);
    expect(publicAssessmentFetch).toHaveBeenCalledWith("/api/v1/public/assessments?q=java&availability=OPEN&pageSize=2");
    expect(JSON.stringify(vi.mocked(publicAssessmentFetch).mock.calls)).not.toContain("Authorization");
  });
});
