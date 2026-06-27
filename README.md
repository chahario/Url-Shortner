# URL Shortener with Per-Visit Billing

A production-shaped URL shortener built to explore the distributed-systems decisions that a real, billable link service requires — not just `create → redirect`, but **per-visit billing at scale**, with the caching, idempotency, and consistency tradeoffs that requirement forces.

> Built as a deep, hands-on study of backend system design. Every component was reasoned about from first principles — the [Architecture writeup](./ARCHITECTURE.md) explains the *why* behind each decision and what was traded away.

---

## What it does

- **Shorten URLs** — auto-generated codes (Base62 over a DB sequence) or custom aliases.
- **Redirect** — cache-first, public, designed for the read-heavy hot path.
- **Count every visit** — asynchronously, via Kafka, without writing a DB row per click.
- **Bill clients** — per-visit pricing, plan tiers, immutable invoices.
- **Protect the API** — API-key auth + per-client rate limiting.
- **Expose usage** — per-day/week/month stats, cursor-paginated URL listing.

## Tech stack

| Concern | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot |
| Primary datastore | PostgreSQL |
| Cache | Redis |
| Event streaming | Apache Kafka |
| Local infra | Docker Compose |

---

## Architecture at a glance

```
                        ┌──────────────┐
   client ──API key──▶  │  API (Spring │
                        │   Boot app)  │
                        └──────┬───────┘
              ┌────────────────┼─────────────────┐
              ▼                ▼                 ▼
        ┌──────────┐    ┌──────────┐      ┌──────────┐
        │ Postgres │    │  Redis   │      │  Kafka   │
        │ (source  │    │ (cache + │      │ (visit   │
        │ of truth)│    │  limits) │      │  events) │
        └──────────┘    └──────────┘      └────┬─────┘
                                               │ consume
                                               ▼
                                        ┌────────────────┐
                                        │  aggregator    │
                                        │ (idempotent →  │
                                        │  usage_daily)  │
                                        └────────────────┘
```

A redirect emits a **visit event** to Kafka and returns immediately. A background consumer aggregates those events into per-day counters (`usage_daily`), deduplicated so each visit is counted **exactly once**. Billing later sums those counters. Full detail in [ARCHITECTURE.md](./ARCHITECTURE.md).

---

## Running locally

### Prerequisites
- JDK 21+
- Docker + Docker Compose

### 1. Start the infrastructure
```bash
docker compose up -d
```
This brings up Postgres, Redis, and Kafka. Confirm all three are healthy:
```bash
docker compose ps
```

### 2. Run the application
```bash
./mvnw spring-boot:run
```
The app starts on `http://localhost:8080`. On first boot it seeds two pricing plans (`free`, `pro`) and a demo client with API key `demo-api-key-12345` for testing.

---

## API

All `/api/**` endpoints require an API key:
```
Authorization: Bearer demo-api-key-12345
```
The redirect endpoint (`GET /{shortCode}`) is **public** — no key required.

### Create a short URL
```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer demo-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"long_url": "https://example.com/some/long/path", "custom_alias": "promo"}'
```
`custom_alias` is optional. Returns `201` with the short code and full short URL.

### Redirect (public)
```bash
curl -i http://localhost:8080/promo
# 302 Found, Location: https://example.com/some/long/path
```

### List your URLs (cursor-paginated)
```bash
curl "http://localhost:8080/api/v1/urls?limit=50" \
  -H "Authorization: Bearer demo-api-key-12345"
# returns { "data": [...], "next_cursor": "<opaque>" }
# pass next_cursor back as ?cursor=<opaque> for the next page
```

### Visit stats
```bash
curl "http://localhost:8080/api/v1/urls/promo/stats?from=2026-06-01&to=2026-06-30" \
  -H "Authorization: Bearer demo-api-key-12345"
# { "total_visits": N, "series": [ { "day": "...", "visits": N }, ... ] }
```

### Generate an invoice
```bash
curl -X POST "http://localhost:8080/api/v1/billing/invoices?month=2026-06" \
  -H "Authorization: Bearer demo-api-key-12345"
```

### List invoices
```bash
curl http://localhost:8080/api/v1/billing/invoices \
  -H "Authorization: Bearer demo-api-key-12345"
```

---

## Key endpoints summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/urls` | ✅ | Create a short URL |
| `GET` | `/{shortCode}` | ❌ (public) | Redirect + count the visit |
| `GET` | `/api/v1/urls` | ✅ | List URLs (cursor pagination) |
| `GET` | `/api/v1/urls/{code}/stats` | ✅ | Per-day/week/month visit stats |
| `POST` | `/api/v1/billing/invoices` | ✅ | Generate a monthly invoice |
| `GET` | `/api/v1/billing/invoices` | ✅ | List invoices |

---

## Design highlights

A few of the decisions explained in detail in [ARCHITECTURE.md](./ARCHITECTURE.md):

- **302, not 301** for redirects — so visits stay observable and billable (301 gets cached by browsers/CDNs).
- **Kafka-based visit counting** — redirects emit events; a consumer aggregates. Can't write a DB row per visit at scale.
- **Exactly-once processing** — dedupe on visit ID + atomic increment-and-record, on top of Kafka's at-least-once delivery. No double-billing.
- **Cache-aside with graceful degradation** — a Redis outage degrades to a slower DB read, never an error.
- **Fail-open rate limiting, fail-closed auth** — the right failure mode per concern.
- **`BigDecimal` money + rate-snapshotted invoices** — exact currency math; issued invoices are immutable.
- **DB-enforced uniqueness** — custom-alias collisions caught via the primary-key constraint, not a racy check-then-insert.
- **Cursor (keyset) pagination** — O(1)-ish at any depth, stable under inserts, unlike OFFSET.

---

## Project status & roadmap

This is a learning-focused build of the **API + billing engine**. Deliberately out of scope (and noted as separate concerns): public sign-up / payments integration, and a web dashboard — provisioning is handled via seeded/admin clients.

Planned hardening before any real deployment:
- [ ] Flyway migrations (replace `ddl-auto`)
- [ ] Externalized config + secrets via environment variables
- [ ] Dockerfile + health checks
- [ ] Test suite (unit + slice + idempotency integration test)
- [ ] Deploy on managed services (RDS / ElastiCache / MSK)

---

## License

MIT