import React from "react";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import CandidateDashboard from "./page";

describe("CandidateDashboard", () => {
  it("offers browsing assessments as the primary and empty-state action", () => {
    render(<CandidateDashboard />);

    expect(screen.getByRole("heading", { name: "Candidate dashboard" })).toBeVisible();
    expect(screen.getByText(/Active-attempt listing is coming/i)).toBeVisible();
    const browseLinks = screen.getAllByRole("link", { name: "Browse assessments" });
    expect(browseLinks).toHaveLength(2);
    browseLinks.forEach((link) => expect(link).toHaveAttribute("href", "/assessments"));
  });
});
