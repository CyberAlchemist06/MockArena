import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const router = vi.hoisted(() => ({ replace: vi.fn(), refresh: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => router }));

import PublicLayout from "@/app/(public)/layout";
import { CandidateNavigation } from "./candidate-navigation";

const user = { userId: "user-id", email: "sam@example.com", displayName: "Sam Candidate", roles: ["USER"] };

describe("candidate navigation", () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify(user), { status: 200 })));
  });

  it("provides all candidate navigation links and the display name from the me BFF", async () => {
    render(<CandidateNavigation />);

    expect(screen.getByRole("link", { name: "MockArena" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Assessments" })).toHaveAttribute("href", "/assessments");
    expect(screen.getByRole("link", { name: "Question Bank" })).toHaveAttribute("href", "/questions");
    expect(screen.getByRole("link", { name: "Companies" })).toHaveAttribute("href", "/companies");
    expect(screen.getByRole("link", { name: "Pricing" })).toHaveAttribute("href", "/pricing");
    expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/candidate");
    expect(await screen.findByText("Signed in as Sam Candidate")).toBeVisible();
  });

  it("uses the logout BFF route and returns to the public home page", async () => {
    render(<CandidateNavigation />);
    fireEvent.click(screen.getByRole("button", { name: "Logout" }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith("/api/auth/logout", { method: "POST" }));
    expect(router.replace).toHaveBeenCalledWith("/");
    expect(router.refresh).toHaveBeenCalledOnce();
  });

  it("keeps public and authenticated navigation separate", async () => {
    const { unmount } = render(<PublicLayout><p>Public content</p></PublicLayout>);
    expect(screen.getByRole("link", { name: "Log in" })).toBeVisible();
    expect(screen.queryByRole("link", { name: "Dashboard" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Logout" })).not.toBeInTheDocument();

    unmount();
    render(<CandidateNavigation />);
    expect(screen.getByRole("link", { name: "Dashboard" })).toBeVisible();
    expect(screen.getByRole("button", { name: "Logout" })).toBeVisible();
    expect(screen.queryByRole("link", { name: "Log in" })).not.toBeInTheDocument();
  });
});
