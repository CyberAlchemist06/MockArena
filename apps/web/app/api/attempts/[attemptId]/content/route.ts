import { NextResponse } from "next/server";
import { assessmentFetch } from "@/lib/api/server";
import { serviceErrorResponse } from "@/lib/api/route-response";
import { cookies } from "next/headers";
import { attemptDeadlineCookie } from "@/lib/auth/session";

export async function GET(_: Request, { params }: { params: Promise<{ attemptId: string }> }) {
  try {
    const { attemptId } = await params;
    const upstream = await assessmentFetch(`/api/v1/attempts/${encodeURIComponent(attemptId)}/content`);
    const items = await upstream.json().catch(() => ({}));
    const deadlineAt = (await cookies()).get(attemptDeadlineCookie(attemptId))?.value ?? null;
    return NextResponse.json(upstream.ok ? { items, deadlineAt } : items, { status: upstream.status });
  } catch (error) { return serviceErrorResponse(error); }
}
