# MockArena web

The browser speaks only to same-origin `/api/*` BFF handlers. Those handlers hold the Identity access token in an HttpOnly, `SameSite=Lax` cookie and call Identity/Assessment from the server. The JWT is never returned to client JavaScript or stored in browser storage.

## Local development

Copy `.env.example` to `.env.local` and start the Identity and Assessment services. Then run `npm run dev` and open `http://localhost:3000`.

Public catalogue, question-bank, company, and pricing views are explicitly static demo/coming-soon presentation only. They do not use internal service APIs. The real backend journey is assessment start, candidate content retrieval, and response autosave.

The deadline shown after an attempt starts comes from the backend start response. It is retained only in a short-lived HttpOnly BFF cookie so refreshes can render the display-only timer; Assessment Service remains authoritative for expiry.
