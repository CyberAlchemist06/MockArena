import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CatalogueBrowser } from "./catalogue-browser";

const assessment = { assessmentId: "6e69a92e-75dd-4530-92a1-d092f3e2ed30", versionNumber: 1, title: "Real assessment", description: "Real catalogue data", assessmentTypeCode: "STANDARD", visibility: "PUBLIC", timing: { policyCode: "FIXED_DURATION", attemptDurationSeconds: 3600 }, availability: { availableFrom: null, availableUntil: null }, questionCount: 3, questionTypeCounts: { MCQ: 2, CODING: 1 } };
describe("CatalogueBrowser", () => {
  afterEach(cleanup);
  it("renders real catalogue data, forwards search, and follows the cursor", async () => {
    const nextAssessment = { ...assessment, assessmentId: "7e69a92e-75dd-4530-92a1-d092f3e2ed30", title: "Second assessment" }; const fetchMock = vi.fn().mockResolvedValueOnce(new Response(JSON.stringify({ items: [assessment], nextCursor: "next" }))).mockResolvedValueOnce(new Response(JSON.stringify({ items: [nextAssessment], nextCursor: null })));
    vi.stubGlobal("fetch", fetchMock); render(<CatalogueBrowser />);
    expect(await screen.findByText("Real assessment")).toBeVisible(); expect(screen.getByRole("link", { name: "Take Assessment" })).toHaveAttribute("href", "/assessments/6e69a92e-75dd-4530-92a1-d092f3e2ed30/take?version=1");
    fireEvent.click(screen.getByRole("button", { name: "Load more" })); await waitFor(() => expect(fetchMock.mock.calls[1][0]).toContain("cursor=next"));
  });
  it("shows empty and unavailable states", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ items: [], nextCursor: null })))); const { unmount } = render(<CatalogueBrowser />); expect(await screen.findByText("No assessments found")).toBeVisible(); unmount(); vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 503 }))); render(<CatalogueBrowser />); expect(await screen.findByText(/catalogue is unavailable/i)).toBeVisible();
  });
});
