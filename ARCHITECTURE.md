# Architecture & Design Decisions

This document explains the *why* behind the URL shortener's design — the requirements that drove each decision, the alternatives considered, and what each choice traded away. The central theme: **one requirement (per-visit billing at scale) cascades into nearly every interesting decision in the system.**

---

## 1. Requirements & scale assumptions

**Functional**
- Shorten URLs (generated codes + custom aliases).
- Redirect short → long.
- Count every visit, attributed to a client.
- Bill clients per visit, by plan tier; produce invoices.
- Authenticate and rate-limit API access.

**Non-functional (the ones that shape the design)**
- **Read-heavy.** Redirects vastly outnumber creates (~100:1 typical for link services).
- **High redirect throughput.** Design target ~115K redirects/sec at peak.
- **Billing must be correct.** A miscounted visit is lost or double-charged revenue.

**Anchored scale math** (the back-of-envelope that justifies the infrastructure):
- ~100M new URLs/day → ~1,157 writes/sec.
- At ~100:1 read:write → ~115K reads/sec.
- 5-year horizon → ~180B URLs → a **7-character Base62** keyspace (62⁷ ≈ 3.5 trillion) is sufficient.

These numbers are why a single Postgres instance can't serve the read path alone (→ cache + replicas), and why per-visit DB writes are infeasible (→ event pipeline).

---

## 2. The redirect must be observable: 302, not 301

**Decision:** redirects return **HTTP 302 (Found)**, not 301 (Moved Permanently).

**Why:** a 301 is cached aggressively by browsers and CDNs. After the first hit, subsequent clicks never reach the server — which means we **cannot count or bill the visit.** A 302 keeps every redirect observable.

**Tradeoff:** 302 forgoes the client-side caching benefit of 301 (slightly more load on our redirect path). For a system whose business model *is* counting clicks, observability wins decisively. This single status-code choice is what makes the entire billing pipeline possible.

---

## 3. Short-code generation

**Decision:** generated codes come from a **Postgres sequence** (`nextval`) encoded to **Base62**. Custom aliases use the client-supplied string directly.

**Why a sequence + Base62:**
- A monotonic sequence guarantees **uniqueness without coordination** — no collision checks, no retries on the generated path.
- Base62 (`0-9A-Za-z`) keeps codes short and URL-safe; a 7-char code covers the 5-year keyspace.

**Alternatives considered:**
- *Random codes* → require a uniqueness check on every create (a round-trip + retry loop). The sequence avoids this entirely.
- *Hashing the long URL* → identical URLs would collide into one code, breaking per-client attribution and custom analytics.

**Tradeoff:** sequential codes are mildly guessable/enumerable. For a link shortener this is acceptable (links aren't secrets); if it mattered, the sequence could be passed through a reversible permutation before encoding.

---

## 4. Caching the read path (cache-aside + graceful degradation)

**Decision:** Redis fronts Postgres for redirect resolution using **cache-aside**, with a 24h TTL and **write-around** on create.

**Flow:** check Redis → on miss, read Postgres → back-fill Redis → return. Creates do *not* warm the cache (write-around) — links that may never be clicked shouldn't occupy cache memory; reads warm it naturally.

**Graceful degradation:** every Redis operation is wrapped so a failure is treated as a **cache miss**. A Redis outage makes redirects *slower* (they fall through to Postgres), never *broken*. The cache is an accelerator over the durable source of truth, never a hard dependency.

**Redis topology:** 1 primary + 2 replicas for availability and read-scale — **replication, not sharding.** The ~20GB hot set fits comfortably on one node, so sharding would add complexity with no benefit.

**Tradeoff:** a cold cache after a Redis restart causes a brief burst of DB reads (acceptable; mitigable later with warming). TTL-based expiry means a tiny window of staleness on URL changes — fine for this domain.

---

## 5. Counting visits without killing the database

**The problem:** at ~115K redirects/sec, writing one DB row per visit is impossible — the write load alone would dwarf everything else, and we'd accumulate billions of rows we only ever read in aggregate.

**Decision:** redirects emit a lightweight **visit event** to **Kafka** and return immediately. A background consumer aggregates events into **per-day counters** (`usage_daily`), not raw visit rows.

**Why Kafka specifically:**
- Absorbs the high event rate as cheap append-only writes.
- **Durable and replayable** — a Redis counter would lose revenue data on restart; Kafka persists.
- Decouples the hot redirect path from the slower aggregation work (the redirect never waits on counting).

**Fire-and-forget on the redirect path:** publishing is wrapped in try/catch — if Kafka is unavailable, the redirect still succeeds (we accept a lost count over a failed user request). Counting is best-effort *at emission*; correctness is enforced downstream.

**Tradeoff:** eventual consistency on stats — counts appear after the consumer processes events (near-real-time, not instant). Acceptable for billing/analytics.

---

## 6. Exactly-once processing (the correctness centerpiece)

**The problem:** Kafka guarantees **at-least-once** delivery — on consumer retry or partition rebalance, the *same* event can be delivered twice. For billing, a duplicate visit is a **double-charge.**

**Decision:** make processing **idempotent** via a dedupe ledger + atomic transaction.

For each event, in **one database transaction**:
1. If `visitId` already exists in `processed_event` → skip (it's a redelivery).
2. Find-or-create the `usage_daily` row for `(short_code, day)` and increment.
3. Insert `visitId` into `processed_event`.

Because steps 2 and 3 commit atomically, and step 1 dedupes, **every visit is counted exactly once regardless of where a crash or redelivery lands:**
- Crash after increment, before recording the ID → transaction rolls back both → reprocessed cleanly.
- Crash after commit, before Kafka offset commit → event redelivered → step 1 skips it.

**Result:** delivery is at-least-once; **processing is exactly-once.** No double-billing.

**Tradeoff:** the `processed_event` ledger grows over time; it's pruned by a time-based archival policy (rows older than Kafka's retention window are safe to drop).

---

## 7. Data model & sharding strategy

**Tables:** `plan`, `client`, `url`, `usage_daily`, `invoice`, `processed_event`.

**Per-table sharding (production design):** each table is sharded on the key its dominant query uses:
- `url` → sharded by **`short_code`** (the redirect knows only the code).
- `usage_daily` / `invoice` → sharded by **`client_id`** (billing queries are per-client).

**Denormalization for billing:** `client_id` is copied onto `usage_daily`. This lets billing **sum a client's usage from a single table** without joining back to `url` (which lives on different shards). A deliberate denormalization that turns a cross-shard join into a single-shard scan.

**Why not a single shard key for everything:** a redirect knows the code but not the client; billing knows the client but not the codes. No single key serves both access patterns — so each table is sharded on its own dominant query. (Range queries on a hash-sharded key are pathological; range queries on a non-shard column *within* one shard are fine.)

---

## 8. Custom aliases & concurrency-safe uniqueness

**Decision:** alias uniqueness is enforced by the **database primary-key constraint**, not an application-level check.

**Why not check-then-insert:** `if (exists) throw; else insert;` has a **race condition** — two concurrent requests for the same alias can both pass the check before either inserts. Instead, we `saveAndFlush` (forcing the INSERT immediately) and catch the resulting `DataIntegrityViolationException`, translating it to a `409 Conflict`. The database enforces uniqueness as one atomic operation — no gap to race through.

This same mechanism covers both custom-alias clashes and the (astronomically rare) generated-code collision.

**Reserved words** (`api`, `admin`, …) are rejected in-memory before the DB round-trip.

**Tradeoff:** relying on exception-driven control flow for the conflict case; justified because it's the only concurrency-safe option without explicit locking.

---

## 9. Authentication & rate limiting

**Auth:** API-key based, enforced by a **servlet filter** (runs before Spring MVC). The filter resolves the key to a `Client`, rejects unknown keys with `401`, and stores the authenticated client in a request-scoped `ThreadLocal` (cleared in `finally`, since pool threads are reused). The public redirect bypasses the filter; only `/api/**` is gated.

**Rate limiting:** per-client, per-minute **fixed-window counter** in Redis (atomic `INCR`), with the limit driven by the client's plan tier. Exceeding it returns `429 + Retry-After`.

**Failure modes — chosen per concern:**
- **Rate limiter fails open.** If Redis is down, requests are allowed. A protective optimization should not become a single point of failure for the whole API. Crucially, **plan entitlement lives in Postgres** — a Redis outage temporarily stops *enforcing* throughput limits but never *grants* premium access.
- **Auth fails closed.** A failed security check must never let a request through.

**Tradeoff:** fixed-window allows a boundary burst (up to 2× the limit across a window edge). A sliding window or token bucket fixes this at higher complexity; fixed-window was chosen for clarity. The redirect is not app-rate-limited (would be handled at a gateway/CDN by IP in production).

---

## 10. Billing & invoices

**Decision:** invoices are **immutable** and **snapshot the rate** that applied at issue time.

**Flow:** sum `usage_daily` for the client+month → read the plan's *current* `rate_per_visit` → compute `amount = visits × rate` (`BigDecimal`, `HALF_UP`, 2 decimal places) → persist an invoice that **stores the rate as a value**, not a live reference to the plan.

**Why snapshot the rate:** if the plan's price changes next month, last month's invoice must **not** silently change. Money records are immutable history; the plan table is *current* pricing. The invoice has no foreign key to the plan's rate — it froze a copy.

**Why `BigDecimal`:** floating-point (`double`) cannot represent decimal currency values exactly; arithmetic drifts over millions of operations. `BigDecimal` is exact, with explicit rounding.

`UNIQUE(client_id, period)` prevents duplicate invoices for the same client-month.

---

## 11. Cursor pagination for listing

**Decision:** the URL listing endpoint uses **keyset (cursor) pagination**, not `OFFSET`.

**Why not OFFSET:** `LIMIT 50 OFFSET 100000` forces the DB to read and discard 100k rows (degrading on deep pages) and is unstable under concurrent inserts (rows shift across page boundaries). A cursor — `WHERE created_at < :cursor ORDER BY created_at DESC` — jumps straight to position via the index (O(1)-ish at any depth) and is stable under inserts.

The cursor is an **opaque** Base64 token (encoding the last row's timestamp), so clients treat it as a blob and the internal representation can evolve. The page size is **clamped to 1–100** to prevent a malicious `?limit=huge` from dumping the table.

**Tradeoff:** cursors only support forward/backward sequential navigation, not jumping to an arbitrary page number — acceptable, and usually preferable, for API consumers.

---

## 12. What was deliberately left out (and why)

Drawing clear boundaries is itself a design decision:

- **Microservices** — *rejected.* This is a cohesive system built by one team; a well-structured **modular monolith** is the correct architecture. Microservices solve organizational and selective-scaling problems this system doesn't have, at a large complexity cost.
- **Databases on Kubernetes** — *rejected.* Running stateful Postgres/Redis/Kafka yourself means owning replication, failover, and backups. Production would use **managed services** (RDS, ElastiCache, MSK). K8s is for the stateless app, not the data.
- **Public sign-up / payments / dashboard** — *out of scope.* The human-facing account layer (registration, Stripe integration, a web UI) is a separate concern from the high-scale API + billing engine that this project demonstrates. Clients are provisioned via seed/admin.

---

## 13. Summary of key tradeoffs

| Decision | Chose | Over | Because |
|---|---|---|---|
| Redirect status | 302 | 301 | visits stay observable/billable |
| Code generation | sequence + Base62 | random / hash | unique without coordination |
| Read path | Redis cache-aside | DB only | 115K reads/sec needs a cache |
| Cache failure | degrade to DB | error | cache is accelerator, not dependency |
| Visit counting | Kafka → aggregate | per-visit DB write | can't write 115K rows/sec |
| Processing | exactly-once (dedupe) | at-least-once | no double-billing |
| Alias uniqueness | DB constraint | check-then-insert | concurrency-safe (no race) |
| Rate limiter failure | fail open | fail closed | not a single point of failure |
| Auth failure | fail closed | fail open | security must not be bypassed |
| Money type | BigDecimal | double | exact currency arithmetic |
| Invoice rate | snapshot | live FK | immutable billing history |
| Pagination | cursor | OFFSET | fast + stable at any depth |
| Architecture | modular monolith | microservices | no org/scaling need for split |

---
