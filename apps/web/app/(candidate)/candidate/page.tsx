import Link from "next/link";
import React from "react";

export default function CandidateDashboard() {
  return <section className="dashboard">
    <p className="muted">Your practice space</p>
    <h1>Candidate dashboard</h1>
    <p className="lead">Start a new practice session or return to an assessment from the catalogue.</p>
    <Link className="button" href="/assessments">Browse assessments</Link>
    <section className="panel empty-state" aria-labelledby="active-attempts-heading">
      <h2 id="active-attempts-heading">Active attempts</h2>
      <p>Active-attempt listing is coming when the Assessment Service endpoint is available. Until then, browse assessments to start or resume an attempt.</p>
      <Link className="button secondary" href="/assessments">Browse assessments</Link>
    </section>
  </section>;
}
