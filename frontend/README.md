# Car Build Log — Frontend

The web UI for the Car Build Log API: a **Next.js** app for tracking project-car builds
(vehicles → modifications → dyno results) with per-user accounts. It talks to the Spring Boot
backend over HTTP.

## Tech stack

- **Next.js** (App Router) + **React** + **TypeScript**
- **Tailwind CSS** + **shadcn/ui** — styling and components
- **TanStack Query** — server-state / data fetching + caching
- **React Hook Form** + **Zod** — forms and validation
- **JWT auth** against the backend (token kept in `localStorage`)

## Features

- List, create, edit, and delete vehicles — with make/model **search** and **status** filtering
- Per-vehicle **modifications**, **dyno results**, and a **build summary**
- **Register / log in / log out** (JWT); each user sees and manages only **their own** vehicles

## Getting started

```bash
npm install
npm run dev        # http://localhost:3000
```

Point the app at your backend in `.env.local`:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
```

(Restart `npm run dev` after editing it — env vars are read at startup.) The backend must be running
and allow `http://localhost:3000` via CORS (it does by default). Then log in as the seeded
**`demo` / `password123`**, or register a new account.

## Scripts

| Command | What it does |
| --- | --- |
| `npm run dev` | Start the dev server |
| `npm run build` | Production build (also type-checks) |
| `npx tsc --noEmit` | Type-check only — the reliable "is my code correct?" check |
| `npm run lint` | ESLint |

> **Build note:** this repo lives under OneDrive, and the dev server holds the `.next` folder open, so
> `npm run build` can fail with `EPERM … unlink … .next…`. It's not a code error — stop `npm run dev`
> (and optionally delete `.next`) first. For a quick code check, prefer `npx tsc --noEmit`.

## Project structure

```
app/          routes (pages) + root layout + TanStack Query provider
  ├─ page.tsx              vehicle list + search/filter
  ├─ login/, register/     auth pages
  └─ vehicles/…            new / detail / edit pages
components/   shared components + shadcn ui/
hooks/        TanStack Query hooks (the data-access layer)
lib/          api.ts (all backend calls), types.ts, utils.ts
```

## How it works / learning notes

Explanations of the code and the concepts behind it — the layered architecture, TanStack Query,
forms, and how **authentication & authorization** work end to end — live in
**[`../docs/CONCEPTS.md`](../docs/CONCEPTS.md)**.
