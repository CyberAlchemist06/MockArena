import { redirect } from "next/navigation";
import { hasSession } from "@/lib/auth/session";
import { safeReturnTo } from "@/lib/auth/return-to";
import { AttemptStarter } from "@/components/candidate/attempt-starter";
export default async function TakeAssessment({ params, searchParams }: { params: Promise<{ assessmentId: string }>; searchParams: Promise<{ version?: string }> }) {
  const { assessmentId } = await params; const { version } = await searchParams; const versionNumber = Number(version ?? "1");
  if (!Number.isInteger(versionNumber) || versionNumber < 1) redirect("/assessments");
  const returnTo = safeReturnTo(`/assessments/${assessmentId}/take?version=${versionNumber}`, "/assessments");
  if (!await hasSession()) redirect(`/login?returnTo=${encodeURIComponent(returnTo)}`);
  return <AttemptStarter assessmentId={assessmentId} versionNumber={versionNumber} />;
}
