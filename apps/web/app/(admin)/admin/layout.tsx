import { notFound, redirect } from "next/navigation"; import { currentUser } from "@/lib/api/server";
export default async function AdminLayout({ children }: Readonly<{ children: React.ReactNode }>) { const user = await currentUser(); if (!user) redirect("/login?returnTo=%2Fadmin"); if (!user.roles.includes("ADMIN")) notFound(); return <main className="shell">{children}</main>; }
