import { describe, expect, it, vi } from "vitest";
vi.mock("@/lib/api/server", () => ({ assessmentFetch: vi.fn(), ServiceError: class ServiceError extends Error { constructor(public status: number, public code?: string) { super(); } } }));
vi.mock("@/lib/auth/session", () => ({ attemptDeadlineCookie:(id:string) => `deadline_${id}`, nonSensitiveCookieOptions:() => ({httpOnly:true, sameSite:"lax", path:"/"}) }));
import { assessmentFetch } from "@/lib/api/server";
import { POST } from "./route";
describe("attempt-start BFF", () => { it("forwards only the idempotency header to Assessment Service", async () => { vi.mocked(assessmentFetch).mockResolvedValue(new Response(JSON.stringify({attemptId:"a", deadlineAt:null}), {status:201,headers:{"Content-Type":"application/json"}})); const result = await POST(new Request("http://web", {method:"POST",headers:{"Idempotency-Key":"key"}}), {params:Promise.resolve({assessmentId:"assessment",versionNumber:"1"})}); expect(result.status).toBe(201); expect(assessmentFetch).toHaveBeenCalledWith("/api/v1/assessments/assessment/versions/1/attempts", expect.objectContaining({headers:{"Idempotency-Key":"key"}})); }); });
