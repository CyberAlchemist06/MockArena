import { afterEach, describe, expect, it, vi } from "vitest";
vi.mock("@/lib/api/server", () => ({ challengeFetch: vi.fn(), assessmentFetch: vi.fn(), ServiceError: class ServiceError extends Error { constructor(public status: number, public code?: string) { super(); } } }));
import { assessmentFetch, challengeFetch } from "@/lib/api/server";
import { POST } from "./route";

describe("creator assessment BFF", () => {
  afterEach(() => vi.clearAllMocks());
  it("orchestrates authenticated Challenge and Assessment publication server-side", async () => {
    vi.mocked(challengeFetch).mockResolvedValueOnce(new Response(JSON.stringify({ challengeId: "challenge", challengeVersionId: "challenge-version", versionNumber: 1 }), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ challengeId: "challenge", challengeVersionId: "challenge-version", versionNumber: 1 }), { status: 200 }));
    vi.mocked(assessmentFetch).mockResolvedValueOnce(new Response(JSON.stringify({ assessmentId: "assessment", versionNumber: 1 }), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ assessmentId: "assessment", versionNumber: 1, title: "Builder", attemptDurationSeconds: 3600 }), { status: 200 }));
    const result = await POST(new Request("http://web/api/creator/assessments", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ title: "Builder", durationMinutes: 60, selectionGroups: [{ questionTypeCodes: ["MCQ"], requestedQuestionCount: 10, taxonomyAll: [], difficultyProfiles: [], contentLocales: ["en"], programmingLanguages: [] }] }) }));
    expect(result.status).toBe(201); expect(challengeFetch).toHaveBeenCalledWith("/api/v1/challenges", expect.any(Object)); expect(challengeFetch).toHaveBeenCalledWith("/api/v1/challenges/challenge/versions/1/publish", expect.any(Object)); expect(assessmentFetch).toHaveBeenCalledWith("/api/v1/assessments", expect.any(Object)); expect(assessmentFetch).toHaveBeenCalledWith("/api/v1/assessments/assessment/versions/1/publish", expect.any(Object));
    expect(await result.json()).toMatchObject({ assessmentId: "assessment", questionCount: 10, questionTypeCounts: { MCQ: 10 } });
  });
  it("does not duplicate a published Challenge when Assessment creation fails", async () => {
    vi.mocked(challengeFetch).mockResolvedValueOnce(new Response(JSON.stringify({ challengeId: "challenge", challengeVersionId: "challenge-version", versionNumber: 1 }), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ challengeId: "challenge", challengeVersionId: "challenge-version", versionNumber: 1 }), { status: 200 }));
    vi.mocked(assessmentFetch).mockResolvedValueOnce(new Response(JSON.stringify({ code: "INVALID_STATE", message: "internal detail" }), { status: 409 }));
    const result = await POST(new Request("http://web/api/creator/assessments", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ title: "Builder", durationMinutes: 60, selectionGroups: [{ questionTypeCodes: ["MCQ"], requestedQuestionCount: 10, taxonomyAll: [], difficultyProfiles: [], contentLocales: ["en"], programmingLanguages: [] }] }) }));
    expect(result.status).toBe(409); expect(challengeFetch).toHaveBeenCalledTimes(2); expect(assessmentFetch).toHaveBeenCalledTimes(1);
    expect(await result.json()).toEqual({ code: "INVALID_STATE" });
  });
});
