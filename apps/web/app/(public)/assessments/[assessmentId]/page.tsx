import { AssessmentDetail } from "@/components/catalogue/assessment-detail";
export default async function AssessmentDetailPage({ params }: { params: Promise<{ assessmentId: string }> }) { const { assessmentId } = await params; return <AssessmentDetail assessmentId={assessmentId} />; }
