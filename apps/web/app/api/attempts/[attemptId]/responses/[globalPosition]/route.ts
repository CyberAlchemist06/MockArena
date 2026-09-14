import { NextResponse } from "next/server";
import { assessmentFetch } from "@/lib/api/server";
import { badRequest, serviceErrorResponse } from "@/lib/api/route-response";

export async function PUT(request: Request, { params }: { params: Promise<{ attemptId: string; globalPosition: string }> }) {
  const idempotencyKey = request.headers.get("Idempotency-Key");
  if (!idempotencyKey) return badRequest("Idempotency-Key is required");
  try {
    const { attemptId, globalPosition } = await params;
    if (!Number.isInteger(Number(globalPosition))) return badRequest("A valid question position is required");
    const body = await request.text();
    const upstream = await assessmentFetch(`/api/v1/attempts/${encodeURIComponent(attemptId)}/responses/${globalPosition}`, {
      method: "PUT", headers: { "Idempotency-Key": idempotencyKey }, body
    });
    return NextResponse.json(await upstream.json().catch(() => ({})), { status: upstream.status });
  } catch (error) { return serviceErrorResponse(error); }
}
