"use client";

import React, { useEffect, useState } from "react";
import type { ComponentType } from "react";
import { monacoLanguage } from "./monaco-language";

const maxSourceLength = 500_000;
type EditorProps = { height: string; language: string; value: string; onChange: (value: string | undefined) => void; options: Record<string, unknown>; loading?: React.ReactNode };

export function CodingEditor({ languageCode, source, disabled, onChange }: { languageCode: string; source: string; disabled: boolean; onChange: (source: string) => void }) {
  const [Editor, setEditor] = useState<ComponentType<EditorProps> | null>(null);
  const [failed, setFailed] = useState(false);
  const [tooLong, setTooLong] = useState(false);

  useEffect(() => {
    let active = true;
    void import("@monaco-editor/react").then(module => { if (active) setEditor(() => module.default as ComponentType<EditorProps>); }).catch(() => { if (active) setFailed(true); });
    return () => { active = false; };
  }, []);

  const change = (next: string) => {
    if (next.length > maxSourceLength) { setTooLong(true); return; }
    setTooLong(false); onChange(next);
  };
  const fallback = <textarea aria-label="Source code" value={source} disabled={disabled} maxLength={maxSourceLength} onChange={event => change(event.target.value)} />;
  return <section aria-label="Coding editor"><label>Source code</label>{Editor ? <Editor height="360px" language={monacoLanguage(languageCode)} value={source} onChange={value => change(value ?? "")} options={{ readOnly: disabled, automaticLayout: true, lineNumbers: "on", tabSize: 4, insertSpaces: true, minimap: { enabled: false } }} loading={fallback} /> : fallback}{failed && <p className="muted">Editor enhancements are unavailable; use the source editor above.</p>}{tooLong && <p className="error" role="alert">Source code cannot exceed 500,000 characters.</p>}</section>;
}
