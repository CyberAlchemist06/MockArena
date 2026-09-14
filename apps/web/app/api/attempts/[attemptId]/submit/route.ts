import { NextResponse } from "next/server";
import { assessmentFetch } from "@/lib/api/server";
import { badRequest, serviceErrorResponse } from "@/lib/api/route-response";
export async function POST(request: Request,{params}:{params:Promise<{attemptId:string}>}){const key=request.headers.get("Idempotency-Key");if(!key)return badRequest("Idempotency-Key is required");try{const {attemptId}=await params;const upstream=await assessmentFetch(`/api/v1/attempts/${encodeURIComponent(attemptId)}/submit`,{method:"POST",headers:{"Idempotency-Key":key}});return NextResponse.json(await upstream.json().catch(()=>({})),{status:upstream.status});}catch(error){return serviceErrorResponse(error);}}
