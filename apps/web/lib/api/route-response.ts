import { NextResponse } from "next/server";
import { ServiceError } from "@/lib/api/server";

export function serviceErrorResponse(error: unknown) {
  if (error instanceof ServiceError) {
    return NextResponse.json(
      { code: error.code ?? "SERVICE_UNAVAILABLE", message: "Request could not be completed" },
      { status: error.status }
    );
  }
  return NextResponse.json({ code: "SERVICE_UNAVAILABLE", message: "Request could not be completed" }, { status: 503 });
}

export function badRequest(message = "Request validation failed") {
  return NextResponse.json({ code: "VALIDATION_ERROR", message }, { status: 400 });
}
