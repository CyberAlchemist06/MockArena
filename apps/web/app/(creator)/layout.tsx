import { redirect } from "next/navigation";
import { CandidateNavigation } from "@/components/navigation/candidate-navigation";
import { hasSession } from "@/lib/auth/session";

export default async function CreatorLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  if (!await hasSession()) redirect("/login?returnTo=%2Fcreate%2Fassessment");
  return <><CandidateNavigation /><main className="shell">{children}</main></>;
}
