import { NextResponse } from "next/server";
import { assessmentFetch } from "@/lib/api/server";
import { serviceErrorResponse } from "@/lib/api/route-response";

export async function GET(_: Request, { params }: { params: Promise<{ attemptId: string }> }) {
  try {
    const { attemptId } = await params;
    const upstream = await assessmentFetch(`/api/v1/attempts/${encodeURIComponent(attemptId)}/result`);
    return NextResponse.json(await upstream.json().catch(() => ({})), { status: upstream.status });
  } catch (error) { return serviceErrorResponse(error); }
}
