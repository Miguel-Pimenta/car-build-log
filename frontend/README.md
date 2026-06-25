# Car Build Log — Frontend, explained from zero

This README is written for someone who **knows the backend but has never done React**. It teaches the
concepts first, then shows exactly how this app uses them, file by file. Read it top to bottom.

> **One-sentence summary:** this is a **Next.js** app (Next.js is a framework built on the **React**
> UI library) that shows your cars in the browser and talks to the Spring Boot API over HTTP.

---

# Part 1 — React from zero

## 1.1 What React is
React is a JavaScript library for building user interfaces out of **components**. A component is just
a **function that returns the HTML-like markup to display**. You build a page by composing components.

```tsx
function Hello() {
  return <h1>Hello</h1>;   // looks like HTML, but it's inside JavaScript
}
```

## 1.2 JSX — HTML inside JavaScript
That `<h1>Hello</h1>` is **JSX**. It's not a string and not real HTML — it's JavaScript that compiles
to function calls. Rules that trip people up coming from plain HTML:
- Attributes are camelCase: `className` (not `class`), `onClick` (not `onclick`).
- You embed JavaScript with **curly braces**: `<p>{vehicle.make}</p>`.
- A component must return **one** top-level element (wrap siblings in a `<div>` or `<>…</>`).

## 1.3 Props — passing data *into* a component
**Props** are the arguments you pass to a component, like function parameters. They flow **down** from
parent to child.

```tsx
function Badge({ status }) {       // `status` is a prop
  return <span>{status}</span>;
}
// used as:  <Badge status="SOLD" />
```
In this project, `StatusBadge` takes a `status` prop; `VehicleForm` takes `initialValue`, `onSubmit`,
and `submitLabel` props.

## 1.4 State — data that changes over time
A component re-draws itself when its **state** changes. You create state with the **`useState`** hook:

```tsx
const [search, setSearch] = useState("");   // [current value, function to change it]
```
- `search` is the current value (starts as `""`).
- Calling `setSearch("golf")` updates it **and tells React to re-render** the component with the new
  value. You never assign `search = "golf"` directly — always go through the setter.

This is the single most important idea in React: **UI is a function of state. Change state → React
re-renders.**

## 1.5 Events
You respond to user actions with `on…` props that take a function:

```tsx
<input value={search} onChange={(e) => setSearch(e.target.value)} />
```
This is a **controlled input**: its `value` comes from state, and every keystroke calls `onChange`,
which updates the state, which re-renders the input with the new value. Round and round.

## 1.6 Lists and keys
To render an array, use `.map()` to turn each item into JSX. React needs a unique **`key`** on each
item so it can track them efficiently:

```tsx
{vehicles.map((v) => <li key={v.id}>{v.make}</li>)}
```

## 1.7 Conditional rendering
Show different things with normal JavaScript expressions inside `{ }`:

```tsx
{isLoading ? <p>Loading…</p> : <List items={data} />}
{error && <p>{error}</p>}        // "&&" shows the right side only if error is truthy
```

## 1.8 Hooks (and the rules)
A **hook** is a special function whose name starts with `use…` that lets a component "hook into" React
features (state, lifecycle, context). `useState` is one. Two rules, always:
1. Only call hooks at the **top level** of a component (never inside `if`, loops, or `.map()`).
2. Only call them **from components or other hooks**.

> This is *why*, on the detail page, the "delete" button for a modification lives in its own
> `ModificationRow` component — so the `useDeleteModification` hook can be called at the top level
> instead of inside a `.map()`.

- **`useEffect`** runs code *after* render — used for side effects like timers. Example here:
  `use-debounce.ts` uses it to start/cancel a timer. (We mostly avoid `useEffect` for data fetching
  now — see TanStack Query below.)
- **Custom hooks** are just functions that call other hooks, so you can reuse logic. All our
  `hooks/use-*.ts` files are custom hooks.

---

# Part 2 — Next.js from zero

React alone doesn't give you URLs/pages, a build system, or server rendering. **Next.js** is the
framework that adds those.

## 2.1 File-based routing (the App Router)
The URL structure mirrors the **`app/` folder**. A folder is a URL segment; a file named
**`page.tsx`** makes that segment a real page:

| File | URL |
|---|---|
| `app/page.tsx` | `/` |
| `app/vehicles/new/page.tsx` | `/vehicles/new` |
| `app/vehicles/[id]/page.tsx` | `/vehicles/123` (any id) |
| `app/vehicles/[id]/edit/page.tsx` | `/vehicles/123/edit` |

`[id]` is a **dynamic segment** — it matches any value, which you read with `useParams()`.

## 2.2 Layouts
**`app/layout.tsx`** wraps every page. It renders the shared shell (`<html>`, `<body>`, the header)
and drops the current page into `{children}`. It runs **before** the page.

## 2.3 Server Components vs Client Components — the key Next.js idea
By default, Next.js components render **on the server** (fast, no JavaScript shipped). But anything
interactive — `useState`, `useEffect`, events, browser APIs — must be a **Client Component**, marked
by putting **`"use client";`** as the first line of the file.

In this app: `layout.tsx` stays a Server Component (just a static shell), and every interactive page
starts with `"use client";`.

## 2.4 Navigation
- **`<Link href="/vehicles/new">`** — client-side navigation (no full page reload).
- **`useRouter()`** → `router.push("/…")` — navigate from code (e.g. after saving a form).
- **`useParams()`** — read the dynamic `[id]` from the URL.

---

# Part 3 — The other tools (and *why* each exists)

React/Next give you the skeleton. These libraries handle specific jobs. **They all stack together —
none replaces another.**

## 3.1 TypeScript
JavaScript with **types**. `interface VehicleResponse { make: string; year: number; … }` describes a
shape; the editor then catches mistakes (typos, wrong arguments) before you run anything. Types are
erased at build time — they don't exist in the running code. Our types live in `lib/types.ts`.

## 3.2 TanStack Query — managing "server state"
The data that lives in your **backend** (vehicles, modifications…) is "server state". Doing it by hand
means `useState` + `useEffect` + `fetch` + loading/error flags on every page. TanStack Query replaces
all that:

```tsx
const { data, isLoading, isError, error } = useQuery({
  queryKey: ["vehicles", search],          // identity of this query in the cache
  queryFn: () => getVehicles(search),       // how to fetch it
});
```
- **`queryKey`** is an array that *identifies and caches* the result. `["vehicles","bmw"]` and
  `["vehicles","golf"]` are stored separately. **Change the key → it refetches automatically.** That's
  how search works: the term is part of the key.
- **`useQuery`** = a **read**. It gives you `data`/`isLoading`/`isError` and caches the result so
  revisiting a page is instant.
- **`useMutation`** = a **write** (POST/PUT/DELETE). After it succeeds you call
  **`invalidateQueries(key)`** to mark cached data stale so it refetches — that's how the screen stays
  in sync after you add/delete something.

> **Server state vs form state:** TanStack Query is **only** for backend data. What you're currently
> *typing into a form* is local "form state" — a different job, handled by `useState` or React Hook
> Form (below).

## 3.3 React Hook Form + Zod — forms and validation
- **React Hook Form (RHF)** manages all of a form's fields without a `useState` per field, and tracks
  things like `isSubmitting`.
- **Zod** is a schema/validation library. You declare the rules once, RHF checks them, and invalid
  fields show inline errors **before** any API call.

```tsx
const schema = z.object({ make: z.string().min(1, "Make is required").max(100), /* … */ });
const form = useForm({ resolver: zodResolver(schema) });
```

## 3.4 Tailwind CSS — styling with utility classes
Instead of separate `.css` files, you put small single-purpose classes in `className`:
`className="border rounded p-4"` = a border, rounded corners, padding. It reads verbose but keeps
styles next to the markup and ships only the classes you use.

## 3.5 shadcn/ui — components you own
Pre-built, accessible components (`Button`, `Badge`, `Select`, `Form`, `Input`) that the shadcn tool
**copied into `components/ui/`**. They're *your* files (built on Tailwind + Radix), so you can read and
edit them — not a black-box package.

---

# Part 4 — How this app is wired

## 4.1 The layered data flow (memorise this)
```
 Page (UI)              what to show          app/page.tsx, app/vehicles/...
    │ calls
 Custom hook            how to get data       hooks/use-*.ts   (useQuery / useMutation)
    │ calls
 API layer              the URLs              lib/api.ts       (request() → fetch())
    │ HTTP
 Spring Boot backend                          http://localhost:8080/api/v1
```
Each layer has **one job**. A component never calls `fetch` directly; it calls a hook.

## 4.2 Directory map
```
frontend/
├─ app/                       routes + global setup
│  ├─ layout.tsx              root shell, runs first, wraps every page (Server Component)
│  ├─ providers.tsx           sets up TanStack Query (Client Component)
│  ├─ globals.css             Tailwind import + theme variables
│  ├─ page.tsx                "/"                      → vehicle list + search
│  └─ vehicles/
│     ├─ new/page.tsx         "/vehicles/new"          → create form
│     └─ [id]/
│        ├─ page.tsx          "/vehicles/:id"          → detail (summary, mods, dyno)
│        └─ edit/page.tsx     "/vehicles/:id/edit"     → edit form
├─ components/
│  ├─ VehicleForm.tsx         reusable create/edit form (React Hook Form + Zod)
│  ├─ StatusBadge.tsx         colored PROJECT/DAILY/SOLD badge
│  └─ ui/                     shadcn components we own (badge, button, select, form, input, label)
├─ hooks/                     the data-access layer (TanStack Query wrappers)
│  ├─ use-vehicles.ts         vehicleKeys factory + list/single/create/update
│  ├─ use-vehicle.ts          summary/mods/dyno reads + delete-vehicle
│  ├─ use-modifications.ts    create/delete modification
│  ├─ use-dyno.ts             create dyno result
│  └─ use-debounce.ts         "wait until typing stops" helper
└─ lib/
   ├─ api.ts                  every backend call (fetch layer)
   ├─ types.ts                TypeScript types mirroring the backend
   └─ utils.ts                cn() class-name helper (used by shadcn)
```

## 4.3 What runs first — the boot order
Opening `http://localhost:3000/`:
1. **`app/layout.tsx`** renders the HTML shell + header (Server Component), placing the page in
   `{children}`.
2. **`app/providers.tsx`** wraps the children and creates the TanStack Query **cache**
   (`QueryClient`), shared via React context so any page can use query hooks.
3. **`app/page.tsx` (`HomePage`)** renders (Client Component).
4. It calls **`useVehicles(debouncedSearch)`**.
5. That hook's `useQuery` runs its `queryFn` → **`lib/api.ts` `getVehicles()`** → `request()` →
   `fetch()` to the backend.
6. While waiting: `isLoading` is true → shows `Loading…`. On success: data is cached under the query
   key, the component re-renders with it. On failure: `isError`/`error`.
7. From there, **user actions** drive everything (typing changes a key → refetch; submitting a form
   runs a mutation → invalidates caches → affected views refetch).

---

# Part 5 — Every file, explained

### `lib/api.ts` — the only place that talks to the backend
- `BASE` = `process.env.NEXT_PUBLIC_API_BASE_URL` (from `.env.local`, read at startup).
- `request<T>(path, options)` — shared helper: does the `fetch`, throws an `Error` (using the
  backend's `{ "message": … }`) when the response isn't OK, returns nothing for `204` (DELETE), else
  parses JSON.
- One small function per endpoint: `getVehicles(search?)`, `getVehicle`, `createVehicle`,
  `updateVehicle`, `deleteVehicle`, `getModifications`, `createModification`, `deleteModification`,
  `getDynoResults`, `createDynoResult`, `getVehicleSummary`.

### `lib/types.ts` — the shapes
TypeScript interfaces mirroring the backend DTOs (`VehicleRequest`/`Response`, etc.), the string
enums `VehicleStatus` / `ModificationCategory`, the arrays used to build dropdowns
(`VEHICLE_STATUSES`, `MODIFICATION_CATEGORIES`), and `PageResponse<T>` (the backend returns lists as a
page with a `content` array).

### `lib/utils.ts`
`cn(...)` merges Tailwind class names safely (used by the shadcn components).

### `app/providers.tsx` (Client Component)
Creates the `QueryClient` inside `useState(() => new QueryClient(...))` so there's exactly **one stable
cache** per browser session (never create it at module scope). Sets `staleTime: 30s` (data is "fresh"
for 30s before a background refetch). Wraps children in `<QueryClientProvider>`.

### `app/layout.tsx` (Server Component)
The page shell + header; sets the browser-tab title; renders `<Providers>{children}</Providers>`.

### `hooks/*` — the data-access layer
- **`use-vehicles.ts`** — the **`vehicleKeys`** query-key factory (`all`, `list(search)`,
  `detail(id)`, `summary(id)`, `modifications(id)`, `dyno(id)`) so keys are consistent in one place.
  Hooks: `useVehicles(search)` (list), `useVehicle(id)` (one), `useCreateVehicle`, `useUpdateVehicle`
  (mutations that invalidate the list/detail on success).
- **`use-vehicle.ts`** — `useVehicleSummary`, `useModifications`, `useDynoResults` (reads), and
  `useDeleteVehicle` (removes the vehicle's cache entries + invalidates the list).
- **`use-modifications.ts`** — `useCreateModification` / `useDeleteModification`; both invalidate
  `modifications` **and** `summary` (the summary's totals change).
- **`use-dyno.ts`** — `useCreateDynoResult`; invalidates `dyno` **and** `summary`.
- **`use-debounce.ts`** — `useDebounce(value, delay)`; returns a copy of `value` that only updates
  after `delay` ms of no change (so search fires when you *stop* typing).

### The pages
- **`app/page.tsx` (list)** — holds `search` state, derives `debouncedSearch = useDebounce(search,300)`,
  calls `useVehicles(debouncedSearch)`. Renders error / loading / empty (distinguishing "no match" vs
  "no vehicles yet") / a list of cards, each linking to the detail page and showing a `<StatusBadge>`.
- **`app/vehicles/new/page.tsx` (create)** — `useCreateVehicle()`, renders `<VehicleForm>`, and on
  submit does `await createVehicle.mutateAsync(data)` then navigates to the new vehicle.
- **`app/vehicles/[id]/page.tsx` (detail)** — reads `id` via `useParams()`, fires **four** queries
  (`useVehicle`, `useVehicleSummary`, `useModifications`, `useDynoResults`). The `ModificationRow`,
  `AddModificationForm`, and `AddDynoForm` are small components defined in the same file; the add-forms
  use the create-mutation hooks and rely on invalidation (no manual reload). *(Those two add-forms
  still use `useState` per field — they're simple enough that they weren't moved to React Hook Form.)*
- **`app/vehicles/[id]/edit/page.tsx` (edit)** — `useVehicle(id)` to pre-fill, `useUpdateVehicle(id)`
  to save, then navigates back to the detail page.

### `components/VehicleForm.tsx` — the form (React Hook Form + Zod)
One reusable form for **both** create and edit. How it works:
- `vehicleSchema` (Zod) declares the rules, mirroring the backend (`make` required ≤100, `year`
  1900–2100, `status` enum, etc.). `z.coerce.number()` turns the text input into a number; empty
  `notes` becomes `undefined`.
- `useForm({ resolver: zodResolver(vehicleSchema), defaultValues })` manages all fields. `defaultValues`
  come from `initialValue` (or sensible blanks; `status` defaults to `PROJECT`).
- Each field uses shadcn's `<FormField>`/`<FormItem>`/`<FormLabel>`/`<FormControl>`/`<FormMessage>` —
  `<FormMessage>` shows that field's validation error automatically.
- The status `<Select>` (a Radix component, not a native input) is wired with
  `value={field.value} onValueChange={field.onChange}` — the "controller" pattern for non-native inputs.
- On submit, RHF validates; if valid it calls `onSubmit(values)` (the prop the parent passed, which runs
  the mutation). API errors are caught into `submitError`; the button uses `form.formState.isSubmitting`.
- **Important:** the form takes an `onSubmit` prop and knows nothing about TanStack Query — that's what
  lets the same form serve both the create and edit pages.

### `components/StatusBadge.tsx`
Wraps shadcn `<Badge>` and maps each `VehicleStatus` to Tailwind colours in one place
(PROJECT = blue, DAILY = green, SOLD = red).

### `components/ui/*`
Generated shadcn primitives — edit freely; they're yours.

---

# Part 6 — Two end-to-end traces

**A) You type "golf" in the search box**
1. `onChange` → `setSearch("golf")` → re-render.
2. `useDebounce` waits 300ms (resets if you keep typing) → `debouncedSearch` becomes `"golf"`.
3. That changes `useVehicles`' key to `["vehicles","golf"]` → TanStack Query runs `queryFn` →
   `getVehicles("golf")` → `GET /vehicles?…&search=golf`.
4. Result cached under that key; the filtered list renders. Clearing the box → key `["vehicles",""]` →
   full list (and "golf" stays cached ~30s).

**B) You add a modification on the detail page**
1. `AddModificationForm` submit → `createMod.mutateAsync(data)` (`useCreateModification`).
2. `mutationFn` → `createModification(vehicleId, data)` → `POST /vehicles/{id}/modifications`.
3. `onSuccess` → `invalidateQueries(modifications(id))` **and** `invalidateQueries(summary(id))`.
4. Only those two refetch; the modifications list and summary update — the header and dyno list are
   untouched.

---

# Part 7 — Running it

```bash
npm install        # first time / after pulling new dependencies
npm run dev        # starts the dev server at http://localhost:3000
```
- Set the backend URL in **`frontend/.env.local`**:
  `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api/v1` (or your Codespace's public `:8080` URL).
  **Restart `npm run dev` after editing it** — env vars are read at startup.
- The backend must be running and allow `http://localhost:3000` via CORS (it does by default).

```bash
npx tsc --noEmit   # type-check only — the reliable "is my code correct?" check
npm run build      # production build (also type-checks)
npm run lint       # eslint
```
> **Build gotcha:** because this project lives under **OneDrive** and the dev server holds the `.next`
> folder open, `npm run build` can throw `EPERM … unlink … .next…`. It's not a code error — stop
> `npm run dev`, optionally delete `.next`, and rebuild. For checking your code, prefer `npx tsc
> --noEmit` (it never touches `.next`).

---

# Part 8 — Glossary

| Term | Meaning |
|---|---|
| **Component** | A function that returns JSX (the UI). |
| **JSX** | HTML-like syntax inside JavaScript; `{ }` embeds JS. |
| **Prop** | An input passed into a component (like a function argument). |
| **State** | Data that, when changed via its setter, re-renders the component. |
| **Hook** | A `use…` function that taps into React features; call at the top level only. |
| **`useState`** | Holds local state: `[value, setValue]`. |
| **`useEffect`** | Runs a side effect after render (timers, subscriptions). |
| **Custom hook** | A function that calls other hooks to reuse logic (our `use-*` files). |
| **Server Component** | Renders on the server; no state/events. Default in Next. |
| **Client Component** | Starts with `"use client"`; can use hooks/state/events. |
| **`useQuery`** | TanStack Query read: fetch + cache + loading/error, keyed by `queryKey`. |
| **`queryKey`** | Array identifying/caching a query; changing it refetches. |
| **`useMutation`** | TanStack Query write; `.mutate()/.mutateAsync()`, `isPending`, `onSuccess`. |
| **`invalidateQueries`** | Marks cached queries stale so they refetch — keeps the UI in sync after writes. |
| **`staleTime`** | How long cached data counts as "fresh" before a background refetch. |
| **Controlled input** | An input whose `value` is driven by state (`value` + `onChange`). |
| **React Hook Form** | Manages form fields/submission without a `useState` per field. |
| **Zod** | Schema/validation library; defines the rules a form checks. |
| **Tailwind** | Utility-class CSS (`px-4`, `text-sm`) in `className`. |
| **shadcn/ui** | Prebuilt components copied into `components/ui/` that you own. |
