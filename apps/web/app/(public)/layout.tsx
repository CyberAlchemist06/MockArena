import Link from "next/link";
import React from "react";
export default function PublicLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <><header className="nav"><div className="shell nav-inner"><Link className="brand" href="/">MockArena</Link><nav className="nav-links"><Link href="/assessments">Assessments</Link><Link href="/questions">Question bank</Link><Link href="/companies">Companies</Link><Link href="/pricing">Pricing</Link><Link href="/login">Log in</Link></nav></div></header><main className="shell">{children}</main></>; }
