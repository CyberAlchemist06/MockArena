import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("@monaco-editor/react", () => ({
  default: ({ value, language, onChange, options }: { value: string; language: string; onChange: (value: string) => void; options: { readOnly: boolean } }) => <textarea aria-label="Monaco source code" data-language={language} readOnly={options.readOnly} value={value} onChange={event => onChange(event.target.value)} />
}));

import { CodingEditor } from "./coding-editor";

afterEach(cleanup);

describe("CodingEditor", () => {
  it("loads the Monaco integration with restored source and mapped language", async () => {
    render(<CodingEditor languageCode="JAVA" source="class Solution {}" disabled={false} onChange={vi.fn()} />);
    const editor = await screen.findByLabelText("Monaco source code");
    expect(editor).toHaveValue("class Solution {}");
    expect(editor).toHaveAttribute("data-language", "java");
  });
  it("passes editor changes through without executing candidate source", async () => {
    const changed = vi.fn(); render(<CodingEditor languageCode="COBOL" source="old" disabled={false} onChange={changed} />);
    const editor = await screen.findByLabelText("Monaco source code");
    expect(editor).toHaveAttribute("data-language", "plaintext");
    fireEvent.change(editor, { target: { value: "new source" } });
    expect(changed).toHaveBeenCalledWith("new source");
  });
  it("uses a read-only Monaco model after terminal submission", async () => {
    render(<CodingEditor languageCode="JAVA" source="saved" disabled onChange={vi.fn()} />);
    expect(await screen.findByLabelText("Monaco source code")).toHaveAttribute("readonly");
  });
});
