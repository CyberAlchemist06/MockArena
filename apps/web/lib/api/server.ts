import "server-only";
import { accessToken } from "@/lib/auth/session";
import type { ApiErrorBody, LoginResponse, User } from "./contracts";

const identityBaseUrl = process.env.IDENTITY_SERVICE_BASE_URL ?? "http://localhost:8082";
const assessmentBaseUrl = process.env.ASSESSMENT_SERVICE_BASE_URL ?? "http://localhost:8083";
const challengeBaseUrl = process.env.CHALLENGE_SERVICE_BASE_URL ?? "http://localhost:8081";

export class ServiceError extends Error {
  constructor(public readonly status: number, public readonly code?: string) { super("Service request failed"); }
}

async function serviceFetch(baseUrl: string, path: string, init: RequestInit = {}, authenticated = false): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body) headers.set("Content-Type", "application/json");
  if (authenticated) {
    const token = await accessToken();
    if (!token) throw new ServiceError(401, "UNAUTHENTICATED");
    headers.set("Authorization", `Bearer ${token}`);
  }
  return fetch(`${baseUrl}${path}`, { ...init, headers, cache: "no-store" });
}

async function parseError(response: Response): Promise<ServiceError> {
  const body = await response.json().catch(() => ({} as ApiErrorBody)) as ApiErrorBody;
  return new ServiceError(response.status, body.code);
}

export async function identityLogin(email: string, password: string): Promise<LoginResponse> {
  const response = await serviceFetch(identityBaseUrl, "/api/v1/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<LoginResponse>;
}

export async function identityRegister(email: string, displayName: string, password: string): Promise<User> {
  const response = await serviceFetch(identityBaseUrl, "/api/v1/auth/register", { method: "POST", body: JSON.stringify({ email, displayName, password }) });
  if (!response.ok) throw await parseError(response);
  return response.json() as Promise<User>;
}

export async function currentUser(): Promise<User | null> {
  try {
    const response = await serviceFetch(identityBaseUrl, "/api/v1/users/me", {}, true);
    return response.ok ? response.json() as Promise<User> : null;
  } catch { return null; }
}

export async function assessmentFetch(path: string, init: RequestInit = {}): Promise<Response> {
  return serviceFetch(assessmentBaseUrl, path, init, true);
}

export async function challengeFetch(path: string, init: RequestInit = {}): Promise<Response> {
  return serviceFetch(challengeBaseUrl, path, init, true);
}

export async function publicAssessmentFetch(path: string): Promise<Response> {
  return serviceFetch(assessmentBaseUrl, path);
}

export async function requireAssessmentResponse(path: string, init: RequestInit = {}): Promise<Response> {
  const response = await assessmentFetch(path, init);
  if (!response.ok) throw await parseError(response);
  return response;
}
