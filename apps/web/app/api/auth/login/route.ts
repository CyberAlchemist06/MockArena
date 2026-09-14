import { NextResponse } from "next/server";
import { identityLogin } from "@/lib/api/server";
import { badRequest, serviceErrorResponse } from "@/lib/api/route-response";
import { ACCESS_COOKIE, accessCookieOptions } from "@/lib/auth/session";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    if (typeof body.email !== "string" || typeof body.password !== "string") return badRequest();
    const login = await identityLogin(body.email, body.password);
    const response = NextResponse.json({ ok: true });
    response.cookies.set(ACCESS_COOKIE, login.accessToken, accessCookieOptions(login.expiresInSeconds));
    return response;
  } catch (error) { return serviceErrorResponse(error); }
}
