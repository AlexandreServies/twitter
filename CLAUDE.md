# Twitter — Twitter API Relay Service

Spring Boot API relay that unifies Twitter/X data access into a single endpoint. Wraps **Synoptic** (primary) and **twitterapi.io** (fallback) with caching, credit-based billing per API key, and automatic failover. Deployed on AWS Elastic Beanstalk. Consumed by `tray` and other services for Twitter lookups. Public repo.

## Stack

- **Java 21** (Corretto)
- **Spring Boot 3.2.0**
- **Build**: Maven
- **Key libs**:
  - Spring WebFlux (`WebClient`)
  - Spring Data Redis (optional; falls back to Caffeine)
  - Resilience4j (rate limiting)
  - AWS SDK v2 2.25.0 (DynamoDB)
  - Springdoc OpenAPI (Swagger at `/swagger-ui.html`)

## Build / Run / Test

```bash
mvn clean install -DskipTests          # build
mvn spring-boot:run                    # run (port 5000)
mvn test                               # tests (empty test dir)
mvn package -DskipTests                # package
mv target/twitter-0.0.1-SNAPSHOT.jar twitter.jar   # for deploy
```

- **CI**: `.github/workflows/ci.yml` (temurin 21, tests on PR/push to main)
- **Deploy**: AWS CodeBuild → Elastic Beanstalk (`buildspec.yml`); process in `Procfile`
- **Tomcat**: 200 max threads, 300 max connections

## Directory Map

`src/main/java/com/bark/twitter/`

- **`TwitterApplication.java`** — main; enables `@Cacheable`, `@Scheduled`, `@Async`
- **`controller/`** — REST endpoints
  - `TwitterController` — `/tweet/{id}`, `/user/{idOrHandle}`, `/community/{id}`, `/follows`, `/communities`, `/health`, `/emergency-alert`
  - `MetricsController`, `CreditController`, `UsageController`
- **`service/`** — business logic
  - `TwitterService` — core: cache, billing, request coalescing
  - `VideoCacheWarmingService` — async video URL rewriting via twproxy
  - `LatencyTracker`
- **`provider/`** — data source abstraction + fallback
  - `SmartTwitterDataProvider` — primary orchestrator (Synoptic → fallback)
  - `SynopticDataProvider`, `TwitterApiDataProvider`
  - `SourceHealthMonitor` — 3-min rolling error windows
  - `EndpointSourceManager` — per-endpoint primary config
- **`client/`** — HTTP clients (`SynopticClient`, `TwitterApiClient`)
- **`mapper/`** — JSON → DTO transformations (Synoptic, TwitterApi → Axion DTOs)
- **`cache/`** — `CachedData`, `RequestCoalescer`, `UsernameCacheService`
- **`credits/`** — `CreditService` (batch-claim from DynamoDB, fast local decrements), `CreditRepository`
- **`usage/`** — `UsageTrackingService`, `DetailedUsageTrackingService`, `UsageRepository`
- **`config/`** — `WebMvcConfig` (CORS, interceptors), `WebClientConfig` (50 connections for Synoptic), `CacheConfig`, `DynamoDbConfig` (us-east-1), `SecurityConfig`, `ApiKeyInterceptor`, `SynopticRateLimiterConfig` (40 req/sec/endpoint), `CacheProperties`, `OpenApiConfig`
- **`dto/`** — `axion/` (response), `twitterapi/` (internal format)
- **`exception/`** — `GlobalExceptionHandler` + custom exceptions
- **`infra/`** — `PushoverClient` (emergency alerts)

## REST Endpoints

- `GET /tweet/{id}` — fetch tweet
- `GET /user/{idOrHandle}` — fetch user
- `GET /community/{id}` — fetch community
- `GET /follows?user_handles=...` — batch follower counts (1 credit/handle)
- `GET /communities?ids=...` — batch community member counts (1 credit/community)
- `GET /health` — liveness
- `POST /emergency-alert` — Pushover notification
- `/metrics/*`, `/credits/*`, `/usage/*` — admin

## Scheduled Tasks

Every 5 seconds:
- `UsageTrackingService.flushToDynamoDB()` — batch write usage
- `CreditService.flushDecrements()` — return local batches
- `UsernameCacheService.flushToDynamoDB()` — persist new username→userId mappings

## External Integrations

**Twitter data sources:**
1. **Synoptic API** (`https://twttr.api.synoptic.com`) — primary; auth via `x-api-key`; rate-limited 40 req/sec/endpoint
   - `/tweets/lookup`, `/user/lookup`, `/community/info`
   - Known bug: sometimes returns wrong tweet ID; validated in `SynopticClient.getTweet()`
2. **twitterapi.io** (`https://api.twitterapi.io/twitter`) — fallback; auth via `X-API-Key`
   - `/tweets`, `/user/info`, `/community/info`
   - Used when `SourceHealthMonitor` detects Synoptic errors over 3-min window

**Video URL proxying** — Twitter blocks direct `video.twimg.com` from servers. `VideoCacheWarmingService` rewrites via `https://twproxy.twproxy.workers.dev/?url={url}` (the `twproxy-cf` sibling repo).

**DynamoDB (us-east-1):**
- `twitter-relay-usage` — (apiKeyHash, endpoint#timeBucket)
- `twitter-relay-credits` — (apiKey)
- `twitter-username-cache` — (username)

**Other**: Redis optional; Pushover for alerts; Elastic Beanstalk runtime; CloudWatch logs.

## Conventions

- **Constructor injection** throughout (no `@Autowired`)
- **Caching**: Caffeine local + optional Redis distributed
- **Concurrency**: `ConcurrentHashMap` + `AtomicLong` for counters; `RequestCoalescer` dedup
- **Async**: `@Async` for video warming, DynamoDB flushes, health checks
- **Logging**: `System.out.println` with `[timestamp][keyPrefix][id][source][type][duration] message` format
- **Rate limiting**: Resilience4j per endpoint type
- **Testing**: empty — CI runs `mvn test` but no tests exist. Add tests when making non-trivial changes.

## Gotchas

- **Billing period ≠ cache TTL** — e.g. `ttl=15m, billing-period=4m` charges at 4/8/12m. `billing-period=0` charges every request.
- **In-memory state is ephemeral** — credits + usage accumulate in memory, flush every 5s. Ungraceful shutdown loses unflushed data.
- **Username cache loads full table on startup** — slow with large tables.
- **3-min health window** — `SourceHealthMonitor` needs 3 min of data before failover; brief startup outages won't trigger.
- **No per-key rate limiting** — only Synoptic is rate-limited globally; API keys share the same bucket beyond credits.
- **API keys validated by `list.contains()`** — keys stored as SHA-256 hashes in `application.yml`.
- **`RequestCoalescer`** — can throw `CoalescingTimeoutException` if one request stalls while others wait.
- **DynamoDB writes are async fire-and-forget** — expect brief in-memory vs DB inconsistency.
