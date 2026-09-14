"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { browserApi } from "@/lib/api/browser-client";
export function AttemptStarter({ assessmentId, versionNumber }: { assessmentId: string; versionNumber: number }) {
  const router = useRouter(); const started = useRef(false); const [error, setError] = useState("");
  useEffect(() => { if (started.current) return; started.current = true; browserApi.startAttempt(assessmentId, versionNumber, crypto.randomUUID()).then((a) => router.replace(`/attempts/${a.attemptId}`)).catch(() => setError("Unable to start this assessment. It may not be available to your account.")); }, [assessmentId, router, versionNumber]);
  return <section className="panel"><h1>Starting assessment…</h1><p className="muted">Preparing your secure attempt route.</p>{error && <><p className="error" role="alert">{error}</p><button className="button secondary" onClick={() => router.push(`/assessments/${assessmentId}`)}>Back to assessment</button></>}</section>;
}
