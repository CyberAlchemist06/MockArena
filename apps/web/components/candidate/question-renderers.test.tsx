import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CandidateQuestionRenderer } from "./question-renderers";
const mcq = { position:1, challengeId:"c", challengeVersionId:"cv", questionId:"q", questionVersionId:"qv", questionTypeCode:"MCQ" as const, title:"Tree", stem:"Pick one", options:[{id:"A",text:"A"},{id:"B",text:"B"}], constraints:null, examples:null, programmingLanguages:null };
const coding = { ...mcq, position:2, questionTypeCode:"CODING" as const, options:[], constraints:"n <= 10", examples:["x"], programmingLanguages:["JAVA"] };
describe("candidate renderers", () => { it("renders MCQ options and saves selection", () => { const save=vi.fn(); render(<CandidateQuestionRenderer question={mcq} onMcq={save} onCoding={vi.fn()} />); fireEvent.click(screen.getByLabelText("B")); expect(save).toHaveBeenCalledWith("B"); expect(screen.queryByText(/correct/i)).toBeNull(); }); it("renders coding textarea and language without grading UI", () => { render(<CandidateQuestionRenderer question={coding} onMcq={vi.fn()} onCoding={vi.fn()} />); expect(screen.getByLabelText("Source code")).toBeTruthy(); expect(screen.getByText("Constraints")).toBeTruthy(); expect(screen.queryByText("Run")).toBeNull(); }); });
