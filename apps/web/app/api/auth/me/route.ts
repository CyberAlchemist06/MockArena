import { NextResponse } from "next/server";
import { currentUser } from "@/lib/api/server";

export async function GET() {
  const user = await currentUser();
  return user ? NextResponse.json(user) : NextResponse.json({ code: "UNAUTHENTICATED", message: "Authentication is required" }, { status: 401 });
}
