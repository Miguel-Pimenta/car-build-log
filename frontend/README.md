# Car Build Log — Frontend (study guide)

A walkthrough of **how this frontend works**, written to be read top-to-bottom. It explains the
tech, the **order things run in**, and **what every file does**.

---

## 1. The big picture

- **React** is a *library* for building UIs out of components. It does not decide routing, data
  fetching, or build setup — you add those.
- **Next.js** is the *framework* on top of React that makes those decisions: file-based routing,
  server/client rendering, the build, etc.
- **TypeScript** adds types so the editor catches mistakes before you run anything.
- **Tailwind CSS** is how we style: small utility classes in `className` (`px-4`, `text-sm`) instead
  of separate `.css` files.
- **shadcn/ui** = pre-built, accessible components (`Badge`, `Button`, `Select`) **copied into our
  repo** under `components/ui/` so we own and can edit them.
- **TanStack Query** manages "server state" — fetching, caching, and refetching backend data — so we
  don't hand-write `useState`/`useEffect`/`fetch` on every page.

The frontend talks to the Spring Boot backend over HTTP (REST + JSON). It never touches the database
directly.

### The layered data flow (memorise this)

```
 Page component (UI)            e.g. app/page.tsx
        │ calls
        ▼
 Custom hook (data access)      e.g. hooks/use-vehicles.ts  → useQuery / useMutation
        │ calls
        ▼
 API layer (raw fetch)          lib/api.ts  → request() → fetch()
        │ HTTP
        ▼
 Spring Boot backend            http://localhost:8080/api/v1
```

**Each layer has one job.** A component decides *what to show*; a hook decides *how to get the data*;
`lib/api.ts` knows *the URLs*. That separation is what keeps the code readable.

---

## 2. What runs first — the boot order

When you open, say, `http://localhost:3000/` :

1. **`app/layout.tsx` (`RootLayout`)** runs first. It's a **Server Component** that renders the HTML
   shell: `<html>`, `<body>`, the header, and the page container. It places the current page into
   `{children}`.
2. Inside the layout, **`app/providers.tsx` (`Providers`)** wraps `{children}`. This is a **client
   component** that creates the **TanStack Query `QueryClient`** (the cache) and shares it via React
   context, so every page can use query hooks.
3. **The page for the URL** renders — for `/` that's **`app/page.tsx` (`HomePage`)**, a client
   component.
4. The page **calls a hook**, e.g. `useVehicles(search)`.
5. The hook's **`useQuery`** runs its **`queryFn`**, which calls **`lib/api.ts` → `getVehicles()` →
   `request()` → `fetch()`** to the backend.
6. While the request is in flight, `isLoading` is `true` → the page shows `Loading…`.
   When it resolves, the data is **stored in the cache under its query key**, `isLoading` becomes
   `false`, and the component **re-renders** with the data. On failure, `isError`/`error` are set.
7. After that, **user actions** drive everything: typing updates state (which changes a query key and
   refetches), submitting a form runs a **mutation** (which then *invalidates* caches so the screen
   updates).

> **Server vs Client Components:** by default Next components render on the server. Anything that uses
> hooks/state/events must start with `"use client"`. Here, the layout stays server-rendered (fast,
> static shell) and the interactive pages opt into the client with `"use client"`.

---

## 3. Directory map

```
frontend/
├─ app/                         ← routes (file-based routing) + global setup
│  ├─ layout.tsx                ← root layout, runs first, wraps every page
│  ├─ providers.tsx             ← sets up TanStack Query (client component)
│  ├─ globals.css               ← Tailwind import + shadcn theme variables
│  ├─ page.tsx                  ← "/"  → vehicle list + search
│  └─ vehicles/
│     ├─ new/page.tsx           ← "/vehicles/new"        → create form
│     └─ [id]/
│        ├─ page.tsx            ← "/vehicles/:id"        → detail (mods, dyno, summary)
│        └─ edit/page.tsx       ← "/vehicles/:id/edit"   → edit form
│
├─ components/
│  ├─ VehicleForm.tsx           ← reusable create/edit form
│  ├─ StatusBadge.tsx           ← colored PROJECT/DAILY/SOLD badge
│  └─ ui/                       ← shadcn components we own (badge, button, select)
│
├─ hooks/                       ← the data-access layer (TanStack Query wrappers)
│  ├─ use-vehicles.ts           ← vehicleKeys factory + list/single/create/update
│  ├─ use-vehicle.ts            ← summary/mods/dyno reads + delete-vehicle
│  ├─ use-modifications.ts      ← create/delete modification
│  ├─ use-dyno.ts               ← create dyno result
│  └─ use-debounce.ts           ← generic "wait until typing stops" helper
│
└─ lib/
   ├─ api.ts                    ← every backend call lives here (fetch layer)
   ├─ types.ts                  ← TypeScript types mirroring the backend DTOs
   └─ utils.ts                  ← cn() class-name helper (used by shadcn)
```

**Routing rule:** a folder under `app/` is a URL segment; a `page.tsx` makes that segment a real
page. `[id]` is a **dynamic segment** — it matches any id and is read with `useParams()`.

---

## 4. The layers in detail

### 4a. `lib/api.ts` — the fetch layer
The single place that knows how to talk to the backend.

- `const BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"` — the backend
  URL. `NEXT_PUBLIC_*` vars are read from `.env.local` **at startup** (restart `npm run dev` after
  editing them).
- `request<T>(path, options)` — the shared helper used by every call: it `fetch`es, throws a normal
  `Error` (using the backend's `{ "message": ... }`) when the response isn't OK, returns `undefined`
  for empty `204` responses (DELETE), and otherwise parses JSON. `<T>` is "the type this call returns".
- The exported functions are thin one-liners per endpoint: `getVehicles(search?)`, `getVehicle(id)`,
  `createVehicle`, `updateVehicle`, `deleteVehicle`, `getModifications`, `createModification`,
  `deleteModification`, `getDynoResults`, `createDynoResult`, `getVehicleSummary`.
  `getVehicles` appends `&search=...` (URL-encoded) only when a search term is given.

### 4b. `lib/types.ts` — the shapes
TypeScript interfaces that **mirror the backend's DTOs**: `VehicleRequest`/`VehicleResponse`,
`ModificationRequest`/`Response`, `DynoRequest`/`Response`, `VehicleSummaryResponse`, and
`PageResponse<T>` (the backend returns lists as a "page" with a `content` array). It also defines the
string-union enums `VehicleStatus` (`PROJECT | DAILY | SOLD`) and `ModificationCategory`, plus the
`VEHICLE_STATUSES` / `MODIFICATION_CATEGORIES` arrays used to build dropdowns. These types are
compile-time only — they vanish at runtime.

### 4c. `hooks/*` — the data-access layer
Custom hooks that wrap TanStack Query so pages never call `fetch` directly.

- **Query keys** (`vehicleKeys` in `use-vehicles.ts`) are the heart of the cache. A key is an array,
  e.g. `["vehicles", "civic"]` or `["vehicles", id, "summary"]`. TanStack Query stores each query's
  result under its key, so different searches/vehicles don't overwrite each other. The factory keeps
  these keys consistent in one place:
  - `vehicleKeys.all` → `["vehicles"]` (invalidating this refetches *all* vehicle queries)
  - `.list(search)`, `.detail(id)`, `.summary(id)`, `.modifications(id)`, `.dyno(id)`
- **Read hooks** use `useQuery({ queryKey, queryFn })` and return `{ data, isLoading, isError, error }`.
  `useQuery` runs `queryFn` on mount and **re-runs automatically whenever the key changes** (that's how
  search works: the key includes the term). Examples: `useVehicles`, `useVehicle`,
  `useVehicleSummary`, `useModifications`, `useDynoResults`.
- **Mutation hooks** use `useMutation({ mutationFn, onSuccess })` for writes (POST/PUT/DELETE). You
  call `.mutate(data)` or `await .mutateAsync(data)`, and read `.isPending` / `.error` for free.
  In `onSuccess` they call `queryClient.invalidateQueries({ queryKey })` to mark the affected caches
  stale so they refetch. Examples and *what they invalidate*:
  - `useCreateVehicle` / `useUpdateVehicle` → the list (and the detail, for update)
  - `useDeleteVehicle` → removes that vehicle's cached entries, invalidates the list
  - `useCreateModification` / `useDeleteModification` → `modifications` **and** `summary`
    (because the summary's `totalModifications`/`totalSpend` change)
  - `useCreateDynoResult` → `dyno` **and** `summary` (the summary's power/torque change)
- **`use-debounce.ts`** (`useDebounce(value, delay)`) returns a copy of `value` that only updates after
  the value has been stable for `delay` ms. Used so search fires after you *stop* typing, not on every
  keystroke.

> **Why invalidation beats the old way:** previously the detail page kept a `refreshKey` counter and
> re-ran one big `useEffect` that fetched everything. Now a mutation invalidates only the keys it
> affected, so only that slice refetches (in the background) and the UI updates from cache.

### 4d. `app/providers.tsx` + `app/layout.tsx`
- `providers.tsx` (`"use client"`) creates the `QueryClient` **inside `useState(() => new QueryClient())`**
  — never at module scope — so there's exactly one stable cache per session. It sets `staleTime: 30s`
  (data is considered "fresh" for 30s, avoiding refetch-on-every-navigation). It renders
  `<QueryClientProvider>` around `children`.
- `layout.tsx` is the **Server Component** that renders the page shell + header and drops
  `<Providers>{children}</Providers>` in the middle. It also sets the tab `metadata` (title).

### 4e. The pages (`app/**/page.tsx`)
All are `"use client"` because they use hooks/state/events.

- **`page.tsx` (list, `/`)** — keeps `search` state for the input, derives `debouncedSearch =
  useDebounce(search, 300)`, and calls `useVehicles(debouncedSearch)`. Renders one of four states:
  error / loading / empty (distinguishing "no match" vs "no vehicles yet") / the list of cards. Each
  card links to the detail page and shows a `<StatusBadge>`.
- **`vehicles/new/page.tsx` (create)** — gets `useCreateVehicle()`, renders `<VehicleForm>`, and on
  submit does `await createVehicle.mutateAsync(data)` then `router.push("/vehicles/{newId}")`. The
  hook's `onSuccess` already invalidated the list.
- **`vehicles/[id]/page.tsx` (detail)** — reads the id with `useParams()`, then fires **four
  independent queries** (`useVehicle`, `useVehicleSummary`, `useModifications`, `useDynoResults`).
  Renders the header (+ badge + Edit/Delete), the summary stats, the modifications list, and the dyno
  list. `ModificationRow` is its own component **so it can call `useDeleteModification` at the top
  level** (hooks can't be called inside a `.map()` callback). The inline `AddModificationForm` /
  `AddDynoForm` use the create-mutation hooks and rely on invalidation to refresh — no manual reload.
- **`vehicles/[id]/edit/page.tsx` (edit)** — `useVehicle(id)` loads the current values to pre-fill,
  builds an `initialValue`, and `useUpdateVehicle(id)` saves; on success it navigates back to detail.

### 4f. The components
- **`VehicleForm.tsx`** — one reusable form for **both** create and edit. Each field is a *controlled
  input*: its value lives in `useState`, the input shows that state, and `onChange` updates it. The
  status field is a shadcn `<Select>` (note it uses `onValueChange={(v) => ...}` — Radix gives you the
  value directly, unlike a native `<select onChange={e => e.target.value}>`). On submit it builds a
  `VehicleRequest` and calls the `onSubmit` prop the parent passed in.
- **`StatusBadge.tsx`** — wraps the shadcn `<Badge>` and maps each `VehicleStatus` to Tailwind colour
  classes (`PROJECT`=blue, `DAILY`=green, `SOLD`=red) in one place, so the "business meaning" of the
  colours lives in a single file.
- **`components/ui/*`** — shadcn primitives (`badge`, `button`, `select`) generated into the repo;
  edit them freely. `lib/utils.ts`'s `cn()` merges Tailwind classes safely.

---

## 5. Two end-to-end traces

**A) You type "golf" in the search box**
1. `onChange` → `setSearch("golf")` re-renders `HomePage`.
2. `useDebounce` waits 300ms; if you keep typing it resets. Once you pause, `debouncedSearch` becomes
   `"golf"`.
3. That changes `useVehicles`' query key to `["vehicles", "golf"]`. TanStack Query sees a new key,
   runs `queryFn` → `getVehicles("golf")` → `GET /api/v1/vehicles?...&search=golf`.
4. Response is cached under that key; the list re-renders filtered. Clear the box → key becomes
   `["vehicles", ""]` → full list (and "golf" stays cached for 30s if you retype it).

**B) You add a modification on the detail page**
1. `AddModificationForm` submit → `createMod.mutate(data)` (`useCreateModification`).
2. `mutationFn` → `createModification(vehicleId, data)` → `POST /vehicles/{id}/modifications`.
3. `onSuccess` → `invalidateQueries(modifications(id))` **and** `invalidateQueries(summary(id))`.
4. TanStack Query refetches just those two; the modifications list and the summary stats update — the
   vehicle header and dyno list are untouched.

---

## 6. Running it

```bash
npm install          # first time / after pulling new deps
npm run dev          # http://localhost:3000
```

- Point the frontend at your backend in **`frontend/.env.local`**:
  `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1` (or your Codespace's public `:8080` URL).
  **Restart `npm run dev` after changing it** — env vars are read at startup.
- The backend must be running and reachable, with CORS allowing `http://localhost:3000` (it does by
  default).

```bash
npm run build        # production build (also full type-check)
npx tsc --noEmit     # type-check only
npm run lint         # eslint
```

---

## 7. Mini-glossary

| Term | Meaning |
|------|---------|
| **Server Component** | Renders on the server; no hooks/state. Default in Next App Router. |
| **Client Component** | Starts with `"use client"`; can use hooks, state, events, browser APIs. |
| **`useState`** | Holds a value that, when changed, re-renders the component. |
| **`useEffect`** | Runs side-effects after render (timers, subscriptions). We mostly avoid it for fetching now. |
| **`useQuery`** | TanStack Query read: fetch + cache + loading/error, keyed by `queryKey`. |
| **`queryKey`** | The array that identifies/caches a query; changing it refetches. |
| **`staleTime`** | How long cached data is "fresh" before a background refetch is allowed. |
| **`useMutation`** | TanStack Query write (POST/PUT/DELETE); `.mutate()`, `.isPending`, `onSuccess`. |
| **`invalidateQueries`** | Marks cached queries stale so they refetch — how the UI stays in sync after writes. |
| **Controlled input** | An input whose value is driven by React state (`value` + `onChange`). |
| **Debounce** | Wait until a value stops changing before acting on it. |
| **Query-key factory** | The `vehicleKeys` object that builds consistent keys in one place. |
