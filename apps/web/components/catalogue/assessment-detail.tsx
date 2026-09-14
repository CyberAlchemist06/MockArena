"use client";

import Link from "next/link";
import React, { useEffect, useState } from "react";
import type { PublicAssessmentDetail } from "@/lib/api/contracts";

export function AssessmentDetail({ assessmentId }: { assessmentId: string }) {
  const [assessment, setAssessment] = useState<PublicAssessmentDetail | null>(null); const [error, setError] = useState<"missing" | "unavailable" | "">("");
  async function load() { setError(""); try { const response = await fetch(`/api/public/assessments/${encodeURIComponent(assessmentId)}`); if (response.status === 404) { setError("missing"); return; } if (!response.ok) throw new Error(); setAssessment(await response.json() as PublicAssessmentDetail); } catch { setError("unavailable"); } }
  // Reload when the routed public identifier changes; load is recreated with this component render.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void load(); }, [assessmentId]);
  if (error === "missing") return <section className="panel"><h1>Assessment unavailable</h1><p>This assessment is not public or no longer available.</p><Link className="button secondary" href="/assessments">Browse assessments</Link></section>;
  if (error === "unavailable") return <section className="panel" role="alert"><h1>Catalogue unavailable</h1><p>We could not load this assessment.</p><button className="button secondary" onClick={() => void load()}>Try again</button></section>;
  if (!assessment) return <p className="notice" role="status">Loading assessment…</p>;
  const availability = assessment.availability.availableFrom || assessment.availability.availableUntil ? `${assessment.availability.availableFrom ? `From ${new Date(assessment.availability.availableFrom).toLocaleString()}` : "Available now"}${assessment.availability.availableUntil ? ` until ${new Date(assessment.availability.availableUntil).toLocaleString()}` : ""}` : "Available now";
  return <><p className="muted">{assessment.assessmentTypeCode}</p><h1>{assessment.title}</h1>{assessment.description && <p className="lead">{assessment.description}</p>}<div className="panel"><p>{assessment.instructionsSummary}</p><p><strong>Timing:</strong> {assessment.timing.policyCode === "FIXED_DURATION" ? `${Math.ceil((assessment.timing.attemptDurationSeconds ?? 0) / 60)} minutes` : "Untimed"}</p><p><strong>Availability:</strong> {availability}</p><p><strong>Attempts:</strong> {assessment.attemptPolicy.maxAttempts}</p><p><strong>Results:</strong> {assessment.resultRelease.policyCode}</p><p><strong>{assessment.questionCount}</strong> questions · MCQ {assessment.questionTypeCounts.MCQ ?? 0} · Coding {assessment.questionTypeCounts.CODING ?? 0}</p><Link className="button" href={`/assessments/${assessment.assessmentId}/take?version=${assessment.versionNumber}`}>Take Assessment</Link></div></>;
}
