import Link from "next/link";
import React from "react";
import type { PublicAssessment } from "@/lib/api/contracts";

function timing(value: PublicAssessment) { return value.timing.policyCode === "FIXED_DURATION" && value.timing.attemptDurationSeconds ? `${Math.ceil(value.timing.attemptDurationSeconds / 60)} minutes` : "Untimed"; }
function availability(value: PublicAssessment) { return value.availability.availableFrom || value.availability.availableUntil ? `${value.availability.availableFrom ? `From ${new Date(value.availability.availableFrom).toLocaleString()}` : "Available now"}${value.availability.availableUntil ? ` until ${new Date(value.availability.availableUntil).toLocaleString()}` : ""}` : "Available now"; }

export function AssessmentCard({ assessment }: { assessment: PublicAssessment }) { return <article className="card"><p className="muted">{assessment.assessmentTypeCode} · {timing(assessment)}</p><h2>{assessment.title}</h2>{assessment.description && <p>{assessment.description}</p>}<p className="muted">{availability(assessment)}</p><p><strong>{assessment.questionCount}</strong> questions · MCQ {assessment.questionTypeCounts.MCQ ?? 0} · Coding {assessment.questionTypeCounts.CODING ?? 0}</p><div className="card-actions"><Link className="button secondary" href={`/assessments/${assessment.assessmentId}`}>View assessment</Link><Link className="button" href={`/assessments/${assessment.assessmentId}/take?version=${assessment.versionNumber}`}>Take Assessment</Link></div></article>; }
