# Car Build Log — Frontend

A small **React + Next.js** app for tracking project-car builds (vehicles, their
modifications, and dyno results). It talks to the Car Build Log REST API.

## Run it

1. Make sure the backend is running and note its base URL.
2. Copy `.env.local.example` to `.env.local` and set the API URL, e.g.:
   ```
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1
   ```
3. Install dependencies and start the dev server:
   ```bash
   npm install
   npm run dev
   ```
4. Open http://localhost:3000.

> The backend must allow this origin (CORS). When the backend runs in a GitHub
> Codespace, set `NEXT_PUBLIC_API_BASE_URL` to the Codespace's forwarded
> `…-8080.app.github.dev/api/v1` URL and restart `npm run dev`.

## How the code is organised

Every page is a **client component** (`'use client'`) that loads data with the
standard React pattern: `useState` + `useEffect` + `fetch`.

| File                              | What it does                                           |
| --------------------------------- | ------------------------------------------------------ |
| `lib/types.ts`                    | TypeScript types describing the data                   |
| `lib/api.ts`                      | One `request` helper + a function per backend endpoint |
| `app/layout.tsx`                  | The shell (header) shown on every page                 |
| `app/page.tsx`                    | The vehicle list                                       |
| `app/vehicles/new/page.tsx`       | Add a vehicle                                          |
| `app/vehicles/[id]/page.tsx`      | One vehicle: summary, modifications, dyno + add forms  |
| `app/vehicles/[id]/edit/page.tsx` | Edit a vehicle                                         |
| `components/VehicleForm.tsx`      | The form reused by "new" and "edit"                    |
