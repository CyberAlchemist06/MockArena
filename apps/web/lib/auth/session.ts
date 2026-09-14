import "server-only";
import { cookies } from "next/headers";

export const ACCESS_COOKIE = "mockarena_access";
const ATTEMPT_DEADLINE_PREFIX = "mockarena_attempt_deadline_";

export const accessCookieOptions = (maxAge: number) => ({
  httpOnly: true,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
  path: "/",
  maxAge
});

export async function accessToken(): Promise<string | null> {
  return (await cookies()).get(ACCESS_COOKIE)?.value ?? null;
}

export async function hasSession(): Promise<boolean> {
  return Boolean(await accessToken());
}
export function attemptDeadlineCookie(attemptId: string) { return `${ATTEMPT_DEADLINE_PREFIX}${attemptId}`; }
export const nonSensitiveCookieOptions = (maxAge: number) => ({ httpOnly: true, sameSite: "lax" as const, secure: process.env.NODE_ENV === "production", path: "/", maxAge });
