# Concepts & Patterns in this Project

Reference notes for the Spring Boot / JPA patterns used in the Car Build Log API,
each tied to the actual file where it shows up. Read top to bottom or jump around.

- [1. The layered structure](#1-the-layered-structure)
- [2. Beans & dependency injection (and why it isn't the Singleton pattern)](#2-beans--dependency-injection)
- [3. DTOs and mappers](#3-dtos-and-mappers)
- [4. Optional](#4-optional)
- [5. @Transactional and readOnly](#5-transactional-and-readonly)
- [6. The persistence context: saving without calling save()](#6-the-persistence-context)
- [7. Entity lifecycle states](#7-entity-lifecycle-states)
- [8. Global exception handling](#8-global-exception-handling)
- [9. Authentication & authorization (Spring Security + JWT)](#9-authentication--authorization-spring-security--jwt)
- [Glossary](#glossary)

---

## 1. The layered structure

The app is organised **package-by-layer**: each technical layer is its own package.
A request flows straight down the layers and the response comes back up:

```
HTTP  ──▶  controller/  ──▶  service/  ──▶  repository/  ──▶  PostgreSQL
                │              │              │
              dto/          model/         model/
            (in & out)    (entities)     (entities)
            mapper/  converts between model and dto
            exception/  catches anything that goes wrong, app-wide
```

- **controller/** — receives HTTP, validates input, returns the right status code. No business logic.
- **service/** — the business logic and the transaction boundary.
- **repository/** — database access (Spring Data JPA).
- **model/** — JPA entities (the database shape).
- **dto/** — the API shape (what clients send and receive).
- **mapper/** — converts `model` ⇄ `dto`.
- **exception/** — one place that turns errors into clean JSON.

Rule of thumb: **controllers talk HTTP, services talk business rules, repositories talk SQL.**

---

## 2. Beans & dependency injection

### What a "bean" is
A **bean** is just an object that Spring creates and manages for you. Any class annotated
with `@RestController`, `@Service`, `@Component` (our mappers), `@Repository`, or
`@RestControllerAdvice` becomes a bean at startup.

Spring's container (the "IoC container" — Inversion of Control) builds these objects, figures
out which ones depend on which, and wires them together. You never write `new VehicleService(...)`
in application code — Spring does it.

### Constructor injection (how a bean gets its dependencies)
We ask for dependencies through the **constructor**:

```java
@RestController
public class VehicleController {

    private final VehicleService vehicleService;   // final = set once, never reassigned

    public VehicleController(VehicleService vehicleService) {  // Spring passes it in
        this.vehicleService = vehicleService;
    }
}
```

Spring sees the constructor needs a `VehicleService`, finds that bean, and passes it in.
(Since Spring 4.3, a single constructor doesn't even need an `@Autowired` annotation.)

**Why constructor injection instead of `@Autowired` on a field?**

| Constructor injection (used here)                    | Field injection (`@Autowired` on a field)    |
| ---------------------------------------------------- | -------------------------------------------- |
| Dependencies can be `final` (immutable)              | Cannot be `final`                            |
| Object is always fully built & valid                 | Can exist half-initialised                   |
| Trivial to unit test with plain `new`                | Needs reflection / a container to set fields |
| A huge constructor warns you the class does too much | Hides that smell                             |

You can see the testability payoff in `service/...Test` files, e.g.
`new VehicleService(mockRepository, new VehicleMapper())` — no Spring needed.

---

## 3. DTOs and mappers

- **Entity (model)** = the _database_ shape. Lives in `model/`, annotated with `@Entity`.
- **DTO** = the _API_ shape — what the client sends (`*Request`) and receives (`*Response`).
  Lives in `dto/`, written as Java `record`s.

**Why not just send the entity over the wire?** Because the database shape and the API contract
should be free to change independently. DTOs let you:

- hide internal fields (we expose `vehicleId`, not the whole nested `Vehicle`),
- put request **validation** (`@NotBlank`, `@Min`, …) on the DTO instead of the entity,
- evolve the DB without breaking clients (and vice-versa).

A **mapper** (in `mapper/`) converts between the two — **both directions**:

```java
// DTO  ->  entity   (incoming: build something to persist)
public Vehicle toEntity(VehicleRequest request) { ... }

// entity  ->  DTO   (outgoing: build what we send back)
public VehicleResponse toResponse(Vehicle vehicle) { ... }
```

Full round trip:
`Request DTO → (mapper) → entity → save → entity → (mapper) → Response DTO`.

---

## 4. Optional

`Optional<T>` is Java's way of saying _"this might be empty — you must handle that."_ It replaces
returning `null` and hoping the caller remembers to check (the usual cause of
`NullPointerException`).

In `repository/DynoResultRepository.java`:

```java
Optional<DynoResult> findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(UUID vehicleId);
```

A vehicle might have **no** dyno runs, so "find the latest" can legitimately find nothing.
You then decide what empty means at the call site:

```java
// A missing dyno is normal — fall back to null (service/VehicleSummaryService.java)
DynoResult latest = dynoResultRepository
        .findFirstByVehicleIdOrderByMeasuredAtDescCreatedAtDesc(vehicleId)
        .orElse(null);

// A missing vehicle is an error — throw 404 (service/VehicleService.java)
return vehicleRepository.findById(id)
        .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", id));
```

`findById` returns `Optional` for free — it's built into Spring Data's `JpaRepository`.

---

## 5. @Transactional and readOnly

### Transactions

A database **transaction** is an all-or-nothing unit of work: everything inside it **commits**
together, or **rolls back** together if something fails. `@Transactional` tells Spring to wrap a
method in one — it opens a transaction before the method, then:

- method returns normally → **commit**
- a `RuntimeException` is thrown → **roll back** (everything undone)

Spring does this with a proxy around the bean, so you never write begin/commit/rollback yourself.

### readOnly = true

A flag meaning _"this method only reads, never writes."_ It's an optimisation + a statement of intent:

- **skips dirty checking** (see §6) — no snapshots, no flush → less memory/CPU on read paths,
- signals the DB/driver it's read-only, which in larger setups lets queries hit a **read replica**.

### The class-default + method-override pattern

Look at any service (e.g. `service/DynoService.java`):

```java
@Service
@Transactional(readOnly = true)     // default for EVERY method in the class
public class DynoService {

    @Transactional                  // overrides → a read-WRITE transaction
    public DynoResponse addToVehicle(...) { ...writes... }

    public List<DynoResponse> listForVehicle(...) { ...reads... }  // inherits readOnly = true
}
```

A method-level annotation overrides the class-level one. Most methods read → `readOnly = true` is
the class default; the write methods (`create`, `update`, `delete`, `addToVehicle`) use a plain
`@Transactional` so they actually commit changes.

> This is the same pattern Spring Data uses internally (its `SimpleJpaRepository` is
> `@Transactional(readOnly = true)` with `@Transactional` on the write methods).

### Why annotate the class instead of just the methods that write?

You _can_ put `@Transactional` only on the write methods and leave reads bare — it works, and it's a
common style. But first, two clarifications:

- `@Transactional` on the class is **not** one shared transaction. Each method call still gets its
  **own** transaction; the class annotation just sets the **default settings** for every method.
- The real choice isn't _transaction vs. no transaction_ — reads benefit from a (read-only)
  transaction too. It's _read-only vs. read-write_.

What reads lose if you leave them un-annotated:

|                                                                   | Reads un-annotated                         | Class default `readOnly = true`    |
| ----------------------------------------------------------------- | ------------------------------------------ | ---------------------------------- |
| A read doing **two** repo calls (e.g. `listForVehicle`)           | two separate transactions                  | one consistent transaction         |
| `readOnly` optimisation (skip dirty checking)                     | no                                         | yes                                |
| Persistence context open for the whole method (lazy loading safe) | no → risk of `LazyInitializationException` | yes                                |
| Can you _forget_ to protect a method?                             | yes (a forgotten write is a bug)           | no — the safe default is automatic |

So the convention flips responsibility to the safe side: make the cheap, safe thing (a read-only
transaction) the **automatic default**, then explicitly mark the few **write** methods — the
dangerous case you _want_ to be deliberate about. And `readOnly = true` isn't a cost; it makes reads
_cheaper_, so there's no downside to "applying it to all."

**Rule of thumb:** `@Transactional(readOnly = true)` on the class, plain `@Transactional` on each
method that writes.

### Two gotchas

1. **Rollback is automatic only for unchecked exceptions** (`RuntimeException`/`Error`); checked
   exceptions commit unless you set `rollbackFor`.
2. **Self-invocation bypasses it** — because it's proxy-based, one method calling another
   `@Transactional` method on `this` skips the transaction logic. The call must come from _outside_
   the bean (controllers calling services cross the proxy correctly).

---

## 6. The persistence context

### Saving without calling save()

This surprises everyone at first. In `service/VehicleService.java`:

```java
@Transactional
public VehicleResponse update(UUID id, VehicleRequest request) {
    Vehicle vehicle = getEntity(id);        // 1. loaded from DB (SELECT)
    vehicleMapper.apply(request, vehicle);  // 2. setters change the in-memory object
    return vehicleMapper.toResponse(vehicle); // 3. read fields for the response
}                                            // 4. on commit → UPDATE is fired automatically
```

No `repository.save(...)` — yet the row updates. Here's why.

Inside a transaction, Hibernate keeps an in-memory workspace called the **persistence context**
(the JPA `EntityManager` / Hibernate `Session`). When it loads an entity, two things happen:
the object becomes **managed** (tracked), and Hibernate stores a **private snapshot** of its
original field values.

At commit it does **dirty checking**: it compares each managed entity's current values to that
snapshot and, for anything changed, generates the `UPDATE` (this is the "flush"). So:

1. `getEntity(id)` → `SELECT`; the `Vehicle` is now managed, snapshot = `{make:"VW", ...}`.
2. setters change the object in memory → `{make:"Volkswagen", ...}`. No SQL yet.
3. just reads fields.
4. commit → flush → Hibernate sees `make` changed → `UPDATE vehicles SET ... WHERE id = ?`.

### What "dirty checking" actually does (the snapshot)

"Dirty" just means _changed_ — an entity is dirty when its in-memory values no longer match the
database. Dirty checking is a **before-and-after comparison**:

1. **At load**, Hibernate stores a hidden **snapshot** of the entity's field values as they came from
   the DB (it keeps both the live object and the frozen snapshot).
2. **You mutate** the object with setters — only the object changes; the snapshot stays frozen.
3. **At flush** (on commit), Hibernate compares current values to the snapshot, field by field.
   Different → the entity is _dirty_ → it generates an `UPDATE`. Unchanged → no SQL.

With values:

```
findById(id)        → Vehicle{ make="VW", model="Golf" }   // snapshot: make="VW", model="Golf"
vehicle.setMake("Volkswagen")                              // object:   make="Volkswagen", model="Golf"

--- commit / flush: compare object vs snapshot ---
   make:  "VW"   -> "Volkswagen"   DIRTY
   model: "Golf" -> "Golf"          unchanged
=> UPDATE vehicles SET ... WHERE id = ?
```

Analogy: a text editor remembers a file's original contents when you open it; on **Save** it diffs
the current text against that original and writes because something changed — you never told it
_what_ changed. Hibernate diffs entity objects the same way at commit.

> By default the `UPDATE` writes _all_ of the row's mapped columns (Hibernate just knows the row is
> dirty). Annotating an entity with `@DynamicUpdate` makes it write only the changed columns — rarely
> needed, but good to know it exists.

### So when DO you need save()?

Compare with `service/ModificationService.java#addToVehicle`:

```java
Modification saved = modificationRepository.save(modificationMapper.toEntity(request, vehicle));
```

Here `toEntity(...)` does `new Modification()` — a **brand-new** object Hibernate has never seen.
It isn't tracked, so dirty checking can't help it; you call `save()` to introduce it (→ `INSERT`).

| Where did the entity come from?              | State             | Call `save()`?                               |
| -------------------------------------------- | ----------------- | -------------------------------------------- |
| `new Modification()` (you created it)        | new / untracked   | **Yes** → `INSERT`                           |
| `findById(...)` (loaded in this transaction) | managed / tracked | **No** → just mutate it → `UPDATE` on commit |

**Rule of thumb: new object → `save()`; loaded object → just change it.**

### Why it only works inside the transaction

Dirty checking works only while the entity is **managed** — i.e. while the persistence context is
open, which is exactly the span of the `@Transactional` method. After commit the context closes and
the entity becomes **detached**; setters on it then do nothing to the DB. (It's also why reads use
`readOnly = true`: the flush is skipped, so an accidental setter on a loaded entity can't surprise
you with an `UPDATE`.)

---

## 7. Entity lifecycle states

Every JPA entity is in one of four states:

| State                    | Meaning                                                | How you get there                              |
| ------------------------ | ------------------------------------------------------ | ---------------------------------------------- |
| **Transient / new**      | Plain object, no DB identity, untracked                | `new Vehicle()`                                |
| **Managed / persistent** | Tracked by the persistence context; changes auto-flush | `findById(...)` inside a tx, or after `save()` |
| **Detached**             | Was managed, but the context closed; no longer tracked | transaction ended                              |
| **Removed**              | Marked for deletion                                    | `delete(...)`                                  |

`save()`/`persist()` moves _transient → managed_. `merge()` re-attaches a _detached_ object.

---

## 8. Global exception handling

Instead of try/catch in every controller, one class handles errors for the whole app —
`exception/GlobalExceptionHandler.java`, annotated `@RestControllerAdvice`:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)   // our 404
    public ResponseEntity<ErrorResponse> handleNotFound(...) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class) // failed @Valid → 400
    public ResponseEntity<ErrorResponse> handleValidation(...) { ... }
    // ...
}
```

Any exception thrown by _any_ controller bubbles up to the matching `@ExceptionHandler`, which
turns it into the consistent JSON shape defined by `ErrorResponse`
(`{ timestamp, status, error, message }`). This is a **cross-cutting concern** handled in one place
— the services just throw `ResourceNotFoundException` and trust the advice to format the response.

---

## 9. Authentication & authorization (Spring Security + JWT)

Two different questions the app has to answer:

- **Authentication** = _who are you?_ (logging in)
- **Authorization** = _what are you allowed to do?_ (you may only touch your own vehicles)

### 9.1 Stateless auth with a JWT

HTTP has no memory — every request arrives as a stranger. Two ways to "stay logged in":

- **Sessions:** the server keeps a guest list in memory and hands you a cookie to look you up. Stateful.
- **JWT (what we use):** the server hands you a signed **token** and remembers nothing. The token _is_
  the proof; you send it on every request.

A **JWT** carries who you are (its `subject` = username) + an expiry, plus a **signature** made with a
secret only the server knows (`app.jwt.secret`). Anyone can _read_ a JWT, but nobody can forge or alter
one without the secret — change a byte and the signature no longer verifies. `security/JwtService.java`
builds and verifies them (HS256, via the JJWT library).

### 9.2 Passwords are hashed, never stored

`config/SecurityConfig` exposes a **`BCryptPasswordEncoder`** bean. On register we store
`encoder.encode(rawPassword)` — a one-way **BCrypt hash** (with a random salt) — never the password
itself. On login the encoder **re-hashes the attempt and compares hashes**, so even our own database
never knows anyone's real password.

Analogy: a one-way blender. You can turn fruit into a smoothie (hash) but never a smoothie back into
fruit; to check a password you blend the attempt the same way and compare smoothies.

### 9.3 The security filter chain (`config/SecurityConfig`)

Spring Security is a **chain of servlet filters** that runs _before_ your controllers. Our
`SecurityFilterChain` bean configures it:

- `sessionCreationPolicy(STATELESS)` — no server sessions; the token carries identity.
- `csrf.disable()` — safe here (see 9.7).
- `permitAll()` for `/api/v1/auth/**` and `/actuator/health`; `authenticated()` for everything else.
- `addFilterBefore(jwtAuthenticationFilter, …)` — slots our token filter into the chain.
- an `authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED))` so unauthenticated requests
  return **401** rather than Spring's default 403 (see 9.7).

### 9.4 The gatekeeper filter + the SecurityContext

`security/JwtAuthenticationFilter` (a `OncePerRequestFilter`) runs on **every** request: read the
`Authorization: Bearer <token>` header → verify it with `JwtService` → load the user → put an
`Authentication` into the **`SecurityContextHolder`**.

The **SecurityContext** is a per-request holder of "who is logged in." The filter _fills_ it; the rest
of the app _reads_ it. With no valid token the filter simply leaves the request unauthenticated, and
the chain's rules return 401.

### 9.5 Bridging your `User` to Spring: `UserDetailsService`

Spring Security doesn't know your `model/User` entity — it only speaks the **`UserDetails`** interface
(username + password hash + roles). `security/CustomUserDetailsService` is the adapter: given a
username, load your `User` and return a Spring `UserDetails`. It's used both at login (to check the
password) and in the filter (to load the caller).

### 9.6 Getting a token: `AuthController`

- `POST /api/v1/auth/register` — validates, rejects a duplicate username/email (409), hashes the
  password, saves the user, returns a token.
- `POST /api/v1/auth/login` — hands the credentials to the **`AuthenticationManager`** (which uses
  `UserDetailsService` + the `PasswordEncoder`); on success `JwtService` issues a token; bad
  credentials → **401**.

### 9.7 401 vs 403, and why CSRF is off

- **401 Unauthorized** = "I don't know who you are" (missing/invalid token). **403 Forbidden** = "I
  know you, but you can't do this."
- **CSRF disabled:** CSRF attacks rely on browsers _auto-sending cookies_. A Bearer token is not
  auto-sent — your code attaches it deliberately — so a token API isn't vulnerable. (You'd keep CSRF on
  for cookie/session auth.)

### 9.8 Authorization: ownership & IDOR

Each `Vehicle` has an `@ManyToOne` **`owner`** (a `User`). `security/CurrentUserService` reads the
logged-in user from the SecurityContext (username → `UserRepository.findByUsername`). Then in
`service/VehicleService`:

- **create** stamps `owner = currentUser` (taken from the token, never from the request body).
- **list** filters the query to `owner = currentUser` — you only ever see your own.
- **get / update / delete** load via `getEntity(id)`, which checks the owner and throws the **same
  404** if the vehicle isn't yours.

Why 404 and not 403 for "not yours"? A 403 would confirm the id _exists_, letting an attacker
enumerate other people's ids. Returning an identical 404 for both "missing" and "not yours" leaks
nothing. This closes the **IDOR** (Insecure Direct Object Reference) hole — a valid token alone must
never be enough to read someone else's row.

**The check protects a _path_, not an entity.** This is the part that's easy to get wrong, and this
project got it wrong for a while. `Modification` has no owner of its own — it inherits one through
its vehicle. The nested routes (`/vehicles/{vehicleId}/modifications`) were safe because they
resolve the vehicle first and so pass through the gate above. But the flat routes
(`GET`/`DELETE /modifications/{id}`) started from the modification and never looked at a vehicle at
all, so they walked straight around it: any logged-in user could read or delete someone else's
modification given its id. The fix is for `ModificationService.getEntity` to walk _up_ to the parent
vehicle and run it through `VehicleService.getEntity`.

Two follow-on rules that fell out of fixing it:

- **`existsById` can't authorise.** The old `delete` used `existsById` + `deleteById`, which never
  load the row — and you cannot check an owner on a row you didn't load. Go through the entity.
- **Keep the error message identical too.** Delegating to `VehicleService` initially let its own
  `"Vehicle not found: <vehicleId>"` escape, which disclosed the parent id and, by differing from
  the `"Modification not found"` you get for an unknown id, still let an attacker tell "exists but
  not yours" from "doesn't exist". The 404 has to be indistinguishable in **body** as well as status.

**Rule of thumb:** add a second route to the same data and you must re-run the ownership check. Ask
of every new endpoint: _what stops this returning someone else's row?_

### 9.9 The frontend side (token handling)

The browser mirrors this in `frontend/lib/api.ts`:

- After login/register, the returned token is saved in the browser's **`localStorage`**.
- Every request adds the `Authorization: Bearer <token>` header.
- A **401** clears the token and redirects to `/login`.

(Simplest approach; `localStorage` is readable by JavaScript, so it's exposed to XSS — an httpOnly
cookie is the more secure but heavier alternative.)

**Rule of thumb:** authentication proves _who_; authorization (ownership checks) proves _what you may
touch_. A token gets you in the door; ownership decides which rooms.

---

## Glossary

- **Bean** — an object created and managed by Spring's container.
- **IoC / Dependency Injection** — the container builds your objects and supplies their
  dependencies, instead of you `new`-ing them.
- **DTO** — Data Transfer Object; the API-facing shape (here, Java `record`s).
- **Entity** — a class mapped to a database table (`@Entity`).
- **Persistence context** — Hibernate's in-transaction workspace that tracks managed entities.
- **Dirty checking** — Hibernate detecting changed managed entities and auto-issuing `UPDATE`s.
- **Flush** — sending pending SQL to the DB (happens on commit, among other times).
- **Transaction** — an all-or-nothing unit of DB work (commit or roll back).
- **Proxy** — a wrapper Spring puts around a bean to add behaviour like transactions.
- **Authentication** — proving _who_ you are (here: logging in for a JWT).
- **Authorization** — deciding _what_ you may do (here: you can only touch your own vehicles).
- **JWT** — JSON Web Token; a signed, self-contained token carrying your identity + an expiry.
- **BCrypt** — a one-way, salted password-hashing algorithm; you compare hashes, never plaintext.
- **SecurityContext** — a per-request holder of "who is logged in," filled by the JWT filter.
- **Principal** — the authenticated identity in the SecurityContext (here, the username).
- **IDOR** — Insecure Direct Object Reference; accessing someone else's record by guessing its id. Prevented by the ownership check returning 404.
- **Stateless** — the server keeps no session; the token carries identity on every request.
