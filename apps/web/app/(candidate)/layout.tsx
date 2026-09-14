import { redirect } from "next/navigation";
import { CandidateNavigation } from "@/components/navigation/candidate-navigation";
import { hasSession } from "@/lib/auth/session";

export default async function CandidateLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  if (!await hasSession()) redirect("/login?returnTo=%2Fcandidate");
  return <><CandidateNavigation /><main className="shell">{children}</main></>;
}
