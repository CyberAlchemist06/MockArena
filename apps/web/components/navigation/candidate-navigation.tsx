"use client";

import Link from "next/link";
import React from "react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

type CurrentUser = { displayName?: string };

const links = [
  ["Assessments", "/assessments"],
  ["Question Bank", "/questions"],
  ["Companies", "/companies"],
  ["Pricing", "/pricing"],
  ["Create Assessment", "/create/assessment"],
  ["Dashboard", "/candidate"]
] as const;

export function CandidateNavigation() {
  const router = useRouter();
  const [user, setUser] = useState<CurrentUser | null>(null);

  useEffect(() => {
    let active = true;
    fetch("/api/auth/me")
      .then(async (response) => response.ok ? response.json() as Promise<CurrentUser> : null)
      .then((currentUser) => { if (active) setUser(currentUser); })
      .catch(() => { if (active) setUser(null); });
    return () => { active = false; };
  }, []);

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST" });
    router.replace("/");
    router.refresh();
  }

  return <header className="nav"><div className="shell nav-inner">
    <Link className="brand" href="/">MockArena</Link>
    <nav className="nav-links" aria-label="Candidate navigation">
      {links.map(([label, href]) => <Link key={href} href={href}>{label}</Link>)}
    </nav>
    <div className="account-actions">
      {user?.displayName && <span className="signed-in">Signed in as {user.displayName}</span>}
      <button className="logout" type="button" onClick={logout}>Logout</button>
    </div>
  </div></header>;
}
