import { NextResponse } from "next/server";
import { publicAssessmentFetch, ServiceError } from "@/lib/api/server";
import { serviceErrorResponse } from "@/lib/api/route-response";

const allowed = ["q", "assessmentTypeCode", "availability", "pageSize", "cursor"] as const;

export async function GET(request: Request) {
  const source = new URL(request.url).searchParams; const query = new URLSearchParams();
  allowed.forEach((key) => { const value = source.get(key); if (value !== null) query.set(key, value); });
  try {
    const upstream = await publicAssessmentFetch(`/api/v1/public/assessments${query.size ? `?${query}` : ""}`);
    return NextResponse.json(await upstream.json().catch(() => ({})), { status: upstream.status });
  } catch (error) { return error instanceof ServiceError ? serviceErrorResponse(error) : serviceErrorResponse(error); }
}
