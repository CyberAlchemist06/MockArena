import React from "react";
import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AssessmentDetail } from "./assessment-detail";
describe("AssessmentDetail", () => { afterEach(cleanup); it("shows an inaccessible public detail safely", async () => { vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 404 }))); render(<AssessmentDetail assessmentId="missing" />); expect(await screen.findByText("Assessment unavailable")).toBeVisible(); }); });
