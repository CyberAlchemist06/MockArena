import React from "react";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AssessmentBuilder } from "./assessment-builder";

describe("AssessmentBuilder", () => {
  afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
  it("omits zero-count groups and converts duration to the BFF request", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ assessmentId: "a", versionNumber: 1, questionCount: 10, questionTypeCounts: { MCQ: 10 } }), { status: 201 })); vi.stubGlobal("fetch", fetchMock);
    render(<AssessmentBuilder />); fireEvent.change(screen.getByLabelText("Assessment title"), { target: { value: "Ten MCQ" } });
    for (let index = 0; index < 20; index++) fireEvent.click(screen.getByLabelText("Decrease coding"));
    for (let index = 0; index < 20; index++) fireEvent.click(screen.getByLabelText("Decrease mcq"));
    await waitFor(() => expect(screen.getByText("Total questions: 10")).toBeVisible());
    fireEvent.submit(screen.getByRole("button", { name: "Create Assessment" }).closest("form")!);
    await waitFor(() => expect(fetchMock).toHaveBeenCalled()); const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body.durationMinutes).toBe(60); expect(body.selectionGroups).toEqual([expect.objectContaining({ questionTypeCodes: ["MCQ"], requestedQuestionCount: 10, programmingLanguages: [] })]);
  });
  it("shows an insufficient-selection message without exposing upstream details", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: "QUESTION_SELECTION_INSUFFICIENT", trace: "secret" }), { status: 422 })));
    render(<AssessmentBuilder />); fireEvent.change(screen.getByLabelText("Assessment title"), { target: { value: "Insufficient" } }); fireEvent.click(screen.getByRole("button", { name: "Create Assessment" }));
    expect(await screen.findByText(/does not have enough matching questions/i)).toBeVisible(); expect(screen.queryByText("secret")).toBeNull();
  });
  it.each([[0, 10], [5, 5], [100, 10]])("accepts %i MCQ / %i CODING and sends only valid type groups", async (mcq, coding) => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ assessmentId: "a", versionNumber: 1, questionCount: mcq + coding, questionTypeCounts: {} }), { status: 201 })); vi.stubGlobal("fetch", fetchMock);
    render(<AssessmentBuilder />); fireEvent.change(screen.getByLabelText("Assessment title"), { target: { value: "Valid mix" } }); setCounts(mcq, coding);
    fireEvent.submit(screen.getByRole("button", { name: "Create Assessment" }).closest("form")!); await waitFor(() => expect(fetchMock).toHaveBeenCalled());
    const groups = JSON.parse(fetchMock.mock.calls[0][1].body).selectionGroups;
    expect(groups.reduce((total: number, group: { requestedQuestionCount: number }) => total + group.requestedQuestionCount, 0)).toBe(mcq + coding);
    expect(groups.every((group: { questionTypeCodes: string[]; programmingLanguages: string[] }) => group.questionTypeCodes[0] === "CODING" || group.programmingLanguages.length === 0)).toBe(true);
    expect(groups.some((group: { questionTypeCodes: string[] }) => group.questionTypeCodes[0] === "MCQ")).toBe(mcq > 0);
    expect(groups.some((group: { questionTypeCodes: string[] }) => group.questionTypeCodes[0] === "CODING")).toBe(coding > 0);
  });
  it("enforces client caps and rejects a total below ten", () => {
    render(<AssessmentBuilder />); fireEvent.change(screen.getByLabelText("Assessment title"), { target: { value: "Bounds" } });
    for (let index = 0; index < 100; index++) fireEvent.click(screen.getByLabelText("Increase mcq"));
    for (let index = 0; index < 20; index++) fireEvent.click(screen.getByLabelText("Increase coding"));
    expect(screen.getByText("Total questions: 110")).toBeVisible(); // 101 MCQ / 11 Coding cannot be selected.
    for (let index = 0; index < 101; index++) fireEvent.click(screen.getByLabelText("Decrease mcq"));
    for (let index = 0; index < 11; index++) fireEvent.click(screen.getByLabelText("Decrease coding"));
    for (let index = 0; index < 9; index++) fireEvent.click(screen.getByLabelText("Increase mcq"));
    expect(screen.getByText("Total questions: 9")).toBeVisible(); expect(screen.getByRole("button", { name: "Create Assessment" })).toBeDisabled();
  });
  function setCounts(mcq: number, coding: number) {
    for (let index = 0; index < 30; index++) fireEvent.click(screen.getByLabelText("Decrease mcq"));
    for (let index = 0; index < 3; index++) fireEvent.click(screen.getByLabelText("Decrease coding"));
    for (let index = 0; index < mcq; index++) fireEvent.click(screen.getByLabelText("Increase mcq"));
    for (let index = 0; index < coding; index++) fireEvent.click(screen.getByLabelText("Increase coding"));
  }
});
