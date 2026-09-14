"use client";

import React, { FormEvent, useEffect, useState } from "react";
import type { PublicCataloguePage } from "@/lib/api/contracts";
import { AssessmentCard } from "./assessment-card";

export function CatalogueBrowser() {
  const [page, setPage] = useState<PublicCataloguePage | null>(null); const [error, setError] = useState(""); const [loading, setLoading] = useState(true);
  const [q, setQ] = useState(""); const [type, setType] = useState(""); const [availability, setAvailability] = useState("OPEN");
  async function load(cursor?: string, append = false) { setLoading(true); setError(""); const params = new URLSearchParams({ availability, pageSize: "20" }); if (q.trim()) params.set("q", q.trim()); if (type.trim()) params.set("assessmentTypeCode", type.trim()); if (cursor) params.set("cursor", cursor); try { const response = await fetch(`/api/public/assessments?${params}`); if (!response.ok) throw new Error(); const next = await response.json() as PublicCataloguePage; setPage(append && page ? { items: [...page.items, ...next.items], nextCursor: next.nextCursor } : next); } catch { setError("The assessment catalogue is unavailable. Please try again."); } finally { setLoading(false); } }
  // The initial catalogue request intentionally uses the initial filter state; searches reload through form submission.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void load(); }, []);
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); void load(); }
  return <><form className="catalogue-filters" onSubmit={submit}><label>Search<input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search assessments" /></label><label>Assessment type<input value={type} onChange={(event) => setType(event.target.value)} placeholder="e.g. STANDARD" /></label><label>Availability<select value={availability} onChange={(event) => setAvailability(event.target.value)}><option value="OPEN">Open now</option><option value="UPCOMING">Upcoming</option><option value="ALL">All published</option></select></label><button className="button" disabled={loading}>Search</button></form>{loading && !page && <p className="notice" role="status">Loading assessments…</p>}{error && <section className="notice" role="alert"><p>{error}</p><button className="button secondary" onClick={() => void load()}>Try again</button></section>}{!loading && !error && page?.items.length === 0 && <section className="notice"><h2>No assessments found</h2><p>Try a different search or availability filter.</p></section>}{page?.items.length ? <div className="grid">{page.items.map((assessment) => <AssessmentCard key={assessment.assessmentId} assessment={assessment} />)}</div> : null}{page?.nextCursor && !error && <button className="button secondary more" disabled={loading} onClick={() => void load(page.nextCursor ?? undefined, true)}>{loading ? "Loading…" : "Load more"}</button>}</>;
}
