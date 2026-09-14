import { NextResponse } from "next/server";
import { assessmentFetch, ServiceError } from "@/lib/api/server";
import { badRequest, serviceErrorResponse } from "@/lib/api/route-response";
import { attemptDeadlineCookie, nonSensitiveCookieOptions } from "@/lib/auth/session";

export async function POST(request: Request, { params }: { params: Promise<{ assessmentId: string; versionNumber: string }> }) {
  const { assessmentId, versionNumber } = await params;
  const idempotencyKey = request.headers.get("Idempotency-Key");
  if (!idempotencyKey || !Number.isInteger(Number(versionNumber))) return badRequest("Idempotency-Key and a valid version are required");
  try {
    const upstream = await assessmentFetch(`/api/v1/assessments/${encodeURIComponent(assessmentId)}/versions/${versionNumber}/attempts`, {
      method: "POST", headers: { "Idempotency-Key": idempotencyKey }
    });
    const body = await upstream.json().catch(() => ({}));
    const response = NextResponse.json(body, { status: upstream.status });
    if (upstream.ok && typeof body.attemptId === "string" && typeof body.deadlineAt === "string") {
      const seconds = Math.max(1, Math.ceil((new Date(body.deadlineAt).getTime() - Date.now()) / 1000));
      response.cookies.set(attemptDeadlineCookie(body.attemptId), body.deadlineAt, nonSensitiveCookieOptions(seconds));
    }
    return response;
  } catch (error) {
    return error instanceof ServiceError ? serviceErrorResponse(error) : serviceErrorResponse(error);
  }
}
