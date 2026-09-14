import { NextResponse } from "next/server";
import { assessmentFetch, challengeFetch, ServiceError } from "@/lib/api/server";
import { serviceErrorResponse } from "@/lib/api/route-response";

type Group = { questionTypeCodes: ["MCQ"] | ["CODING"]; requestedQuestionCount: number; taxonomyAll: unknown[]; difficultyProfiles: unknown[]; contentLocales: string[]; programmingLanguages: string[] };
type BuilderInput = { title: string; description?: string; durationMinutes: number; selectionGroups: Group[] };

function validation(message: string) { return NextResponse.json({ code: "VALIDATION_ERROR", message }, { status: 400 }); }

export async function POST(request: Request) {
  const input = await request.json().catch(() => null) as BuilderInput | null;
  if (!input || !input.title?.trim() || !Number.isInteger(input.durationMinutes) || input.durationMinutes < 1 || !Array.isArray(input.selectionGroups) || input.selectionGroups.length === 0) return validation("Invalid assessment builder request.");
  try {
    const challenge = await challengeFetch("/api/v1/challenges", { method: "POST", body: JSON.stringify({ title: input.title.trim(), visibility: "PUBLIC", selectionGroups: input.selectionGroups }) });
    const challengeBody = await challenge.json().catch(() => ({}));
    if (!challenge.ok) return NextResponse.json(challengeBody, { status: challenge.status });
    const publishedChallenge = await challengeFetch(`/api/v1/challenges/${encodeURIComponent(challengeBody.challengeId)}/versions/${challengeBody.versionNumber}/publish`, { method: "POST", body: JSON.stringify({ expectedChallengeVersion: 0, expectedVersion: 0 }) });
    const publishedChallengeBody = await publishedChallenge.json().catch(() => ({}));
    if (!publishedChallenge.ok) return NextResponse.json(publishedChallengeBody, { status: publishedChallenge.status });
    const assessment = await assessmentFetch("/api/v1/assessments", { method: "POST", body: JSON.stringify({ visibility: "PUBLIC", content: {
      title: input.title.trim(), description: input.description?.trim() || null, instructions: "Complete all questions before the assessment deadline.", assessmentTypeCode: "STANDARD",
      timingPolicy: { policyCode: "FIXED_DURATION", parameters: {} }, attemptDurationSeconds: input.durationMinutes * 60,
      attemptPolicy: { policyCode: "MAX_ATTEMPTS", parameters: { maxAttempts: 1 } }, resultReleasePolicy: { policyCode: "IMMEDIATE", parameters: {} },
      challengeVersionIds: [publishedChallengeBody.challengeVersionId]
    } }) });
    const assessmentBody = await assessment.json().catch(() => ({}));
    if (!assessment.ok) return NextResponse.json({ code: assessmentBody.code ?? "ASSESSMENT_CREATION_FAILED" }, { status: assessment.status });
    const publishedAssessment = await assessmentFetch(`/api/v1/assessments/${encodeURIComponent(assessmentBody.assessmentId)}/versions/${assessmentBody.versionNumber}/publish`, { method: "POST", body: JSON.stringify({ expectedAssessmentVersion: 0, expectedVersion: 0 }) });
    const publishedAssessmentBody = await publishedAssessment.json().catch(() => ({}));
    if (!publishedAssessment.ok) return NextResponse.json({ code: publishedAssessmentBody.code ?? "ASSESSMENT_PUBLICATION_FAILED" }, { status: publishedAssessment.status });
    return NextResponse.json({ assessmentId: publishedAssessmentBody.assessmentId, versionNumber: publishedAssessmentBody.versionNumber, title: publishedAssessmentBody.title, durationSeconds: publishedAssessmentBody.attemptDurationSeconds, questionCount: input.selectionGroups.reduce((total, group) => total + group.requestedQuestionCount, 0), questionTypeCounts: Object.fromEntries(input.selectionGroups.map(group => [group.questionTypeCodes[0], group.requestedQuestionCount])) }, { status: 201 });
  } catch (error) { return error instanceof ServiceError ? serviceErrorResponse(error) : serviceErrorResponse(error); }
}
