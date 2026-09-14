import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AttemptWorkspace } from "./attempt-workspace";
import { BffError, browserApi } from "@/lib/api/browser-client";

function renderWorkspace() { const client = new QueryClient({ defaultOptions: { queries: { retry: false } } }); return render(<QueryClientProvider client={client}><AttemptWorkspace attemptId="attempt-1" /></QueryClientProvider>); }
function terminalContent() { return vi.spyOn(browserApi, "content").mockRejectedValue(new BffError(409, "INVALID_STATE")); }
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

describe("AttemptWorkspace result recovery", () => {
  it("recovers a pending submitted Attempt through the same-origin result client", async () => {
    terminalContent(); const result = vi.spyOn(browserApi, "result").mockResolvedValue({ attemptId:"attempt-1", evaluationStatus:"PENDING", score:null, maxScore:null, percentage:null, items:[] });
    renderWorkspace(); await screen.findByText("Evaluation in progress.");
    expect(result).toHaveBeenCalledWith("attempt-1"); expect(screen.queryByText(/Score/)).not.toBeInTheDocument();
  });
  it("renders partial outcomes without a final total", async () => {
    terminalContent(); vi.spyOn(browserApi, "result").mockResolvedValue({ attemptId:"attempt-1", evaluationStatus:"PARTIALLY_EVALUATED", score:null, maxScore:null, percentage:null, items:[{globalPosition:1,questionTypeCode:"MCQ",evaluationStatus:"EVALUATED",outcome:"CORRECT",awardedScore:1,maxScore:1},{globalPosition:2,questionTypeCode:"CODING",evaluationStatus:"PENDING",outcome:null,awardedScore:null,maxScore:null}] });
    renderWorkspace(); await screen.findByText("Evaluation partially complete");
    expect(screen.getByText("Question 1 — Correct")).toBeInTheDocument(); expect(screen.getByText("Question 2 — Pending evaluation")).toBeInTheDocument(); expect(screen.queryByText("Score")).not.toBeInTheDocument();
  });
  it("renders the server-supplied final score and safe outcomes", async () => {
    terminalContent(); vi.spyOn(browserApi, "result").mockResolvedValue({ attemptId:"attempt-1", evaluationStatus:"EVALUATED", score:8, maxScore:10, percentage:80, items:[{globalPosition:1,questionTypeCode:"MCQ",evaluationStatus:"EVALUATED",outcome:"CORRECT",awardedScore:1,maxScore:1},{globalPosition:2,questionTypeCode:"MCQ",evaluationStatus:"EVALUATED",outcome:"INCORRECT",awardedScore:0,maxScore:1},{globalPosition:3,questionTypeCode:"MCQ",evaluationStatus:"EVALUATED",outcome:"UNANSWERED",awardedScore:0,maxScore:1}] });
    renderWorkspace(); await screen.findByText("Assessment result");
    expect(screen.getByText("8 / 10")).toBeInTheDocument(); expect(screen.getByText("80%")).toBeInTheDocument(); expect(screen.getByText("Question 2 — Incorrect")).toBeInTheDocument(); expect(screen.queryByText(/correctOptionId|explanation/i)).not.toBeInTheDocument();
  });
  it("keeps unreleased results opaque", async () => {
    terminalContent(); vi.spyOn(browserApi, "result").mockRejectedValue(new BffError(409, "RESULT_NOT_RELEASED"));
    renderWorkspace(); await screen.findByText("Results are not available yet.");
    expect(screen.queryByText("Score")).not.toBeInTheDocument(); expect(screen.queryByText("Question outcomes")).not.toBeInTheDocument();
  });
});
