import { NextResponse } from "next/server";
import { publicAssessmentFetch, ServiceError } from "@/lib/api/server";
import { serviceErrorResponse } from "@/lib/api/route-response";

export async function GET(_: Request, { params }: { params: Promise<{ assessmentId: string }> }) {
  const { assessmentId } = await params;
  try {
    const upstream = await publicAssessmentFetch(`/api/v1/public/assessments/${encodeURIComponent(assessmentId)}`);
    return NextResponse.json(await upstream.json().catch(() => ({})), { status: upstream.status });
  } catch (error) { return error instanceof ServiceError ? serviceErrorResponse(error) : serviceErrorResponse(error); }
}
