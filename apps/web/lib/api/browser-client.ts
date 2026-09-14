import type { ApiErrorBody, Attempt, CandidateContent, SaveResponseInput, SavedResponse, User } from "./contracts";

export class BffError extends Error {
  constructor(public readonly status: number, public readonly code?: string) { super("MockArena request failed"); }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, { ...init, headers: { "Content-Type": "application/json", ...init.headers }, credentials: "same-origin" });
  if (!response.ok) {
    const body = await response.json().catch(() => ({} as ApiErrorBody)) as ApiErrorBody;
    throw new BffError(response.status, body.code);
  }
  return response.json() as Promise<T>;
}

export const browserApi = {
  me: () => request<User>("/api/auth/me"),
  startAttempt: (assessmentId: string, versionNumber: number, idempotencyKey: string) => request<Attempt>(`/api/assessments/${assessmentId}/versions/${versionNumber}/attempts`, { method: "POST", headers: { "Idempotency-Key": idempotencyKey } }),
  content: (attemptId: string) => request<CandidateContent>(`/api/attempts/${attemptId}/content`),
  responses: (attemptId: string) => request<SavedResponse[]>(`/api/attempts/${attemptId}/responses`),
  saveResponse: (attemptId: string, position: number, input: SaveResponseInput, key: string) => request<SavedResponse>(`/api/attempts/${attemptId}/responses/${position}`, { method: "PUT", headers: { "Idempotency-Key": key }, body: JSON.stringify(input) })
};
