import { AttemptWorkspace } from "@/components/candidate/attempt-workspace";
export default async function AttemptPage({ params }: { params: Promise<{ attemptId: string }> }) { const { attemptId } = await params; return <AttemptWorkspace attemptId={attemptId} />; }
