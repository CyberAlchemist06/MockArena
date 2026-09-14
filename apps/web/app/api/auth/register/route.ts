import { NextResponse } from "next/server";
import { identityLogin, identityRegister } from "@/lib/api/server";
import { badRequest, serviceErrorResponse } from "@/lib/api/route-response";
import { ACCESS_COOKIE, accessCookieOptions } from "@/lib/auth/session";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    if (typeof body.email !== "string" || typeof body.displayName !== "string" || typeof body.password !== "string") return badRequest();
    const user = await identityRegister(body.email, body.displayName, body.password);
    const login = await identityLogin(body.email, body.password);
    const response = NextResponse.json({ user });
    response.cookies.set(ACCESS_COOKIE, login.accessToken, accessCookieOptions(login.expiresInSeconds));
    return response;
  } catch (error) { return serviceErrorResponse(error); }
}
