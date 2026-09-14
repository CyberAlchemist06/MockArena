"use client";
import React, { useRef, useState } from "react";
import type { CandidateQuestion } from "@/lib/api/contracts";
function languages(value: unknown): string[] { return Array.isArray(value) ? value.filter((v): v is string => typeof v === "string") : []; }
export function CandidateQuestionRenderer({ question, disabled = false, onMcq, onCoding }: { question: CandidateQuestion; disabled?: boolean; onMcq: (option:string) => void; onCoding: (language:string, source:string) => void }) {
  const [source, setSource] = useState(""); const [language, setLanguage] = useState(languages(question.programmingLanguages)[0] ?? ""); const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  if (question.questionTypeCode === "MCQ") return <div>{question.options.map((option) => <label className="option" key={option.id}><input aria-label={option.text} type="radio" name={`q-${question.position}`} disabled={disabled} onChange={() => onMcq(option.id)} />{option.text}</label>)}</div>;
  const schedule = (nextLanguage: string, nextSource: string) => { if (timer.current) clearTimeout(timer.current); timer.current = setTimeout(() => onCoding(nextLanguage, nextSource), 900); };
  return <div><h2>Constraints</h2><p>{question.constraints}</p><pre>{JSON.stringify(question.examples, null, 2)}</pre><label>Programming language<select value={language} disabled={disabled} onChange={(e) => { setLanguage(e.target.value); schedule(e.target.value, source); }}>{languages(question.programmingLanguages).map((item) => <option key={item}>{item}</option>)}</select></label><label>Source code<textarea value={source} disabled={disabled} onChange={(e) => { setSource(e.target.value); schedule(language, e.target.value); }} /></label></div>;
}
