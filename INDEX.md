# Twitter - INDEX

## 1. Purpose

Twitter is a Spring Boot API relay service that provides a unified interface for fetching Twitter data. It:
- Wraps Synoptic API (primary) and Twitterapi.io (fallback) into a single API
- Provides caching with configurable TTL and billing periods
- Tracks API usage and credits per API key
- Handles automatic failover between data sources based on health monitoring

**Role in Bark Stack:**
- Consumed by: tray (for Twitter data lookups)
- Independent service with its own API keys for clients (AXIOM, BARK, etc.)

## 2. Key Directories

```
src/main/java/com/bark/twitter/
├── TwitterApplication.java        # Main entry point
├── controller/
│   ├── TwitterController.java     # Main REST endpoints
│   ├── MetricsController.java     # Latency metrics endpoints
│   ├── CreditController.java      # Credit management
│   └── UsageController.java       # Usage query endpoints
├── service/
│   ├── TwitterService.java        # Core business logic, caching, billing
│   ├── LatencyTracker.java        # Cache-miss latency tracking
│   └── VideoCacheWarmingService.java  # Async video cache warming
├── provider/
│   ├── TwitterDataProvider.java   # Interface for data providers
│   ├── SmartTwitterDataProvider.java  # Orchestrates fallback/shadowing
│   ├── SynopticDataProvider.java  # Synoptic API wrapper
│   ├── TwitterApiDataProvider.java    # Twitterapi.io wrapper
│   ├── SourceHealthMonitor.java   # Health monitoring (3-min rolling windows)
│   └── EndpointSourceManager.java # Primary source config per endpoint
├── client/
│   ├── SynopticClient.java        # HTTP client for Synoptic (WebClient)
│   └── TwitterApiClient.java      # HTTP client for Twitterapi.io
├── mapper/
│   ├── SynopticToAxiomMapper.java # Synoptic JSON → Axion DTO
│   ├── TwitterApiToAxionMapper.java   # TwitterAPI JSON → Axion DTO
│   └── SynopticToTwitterApiMapper.java
├── cache/
│   ├── CachedData.java            # Wrapper with billing tracking
│   ├── UsernameCacheService.java  # In-memory username→userId cache
│   └── RequestCoalescer.java      # Deduplicates concurrent requests
├── credits/
│   ├── CreditService.java         # API credit tracking
│   └── CreditRepository.java      # DynamoDB access for credits
├── usage/
│   ├── UsageTrackingService.java  # Tracks API calls
│   ├── UsageRepository.java       # DynamoDB access for usage
│   └── UsageKey.java              # Key: apiKeyHash + endpoint + minute
├── config/
│   ├── WebMvcConfig.java          # CORS, interceptor registration
│   ├── WebClientConfig.java       # Synoptic WebClient configuration
│   ├── DynamoDbConfig.java        # DynamoDB async client
│   ├── CacheConfig.java           # Caffeine cache manager
│   ├── SecurityConfig.java        # API key validation
│   ├── ApiKeyInterceptor.java     # HTTP interceptor for auth
│   └── SynopticRateLimiterConfig.java  # Resilience4j (40 req/sec)
├── dto/
│   ├── axion/                     # Response DTOs (AxionTweetDto, etc.)
│   └── twitterapi/                # Source format DTOs
├── exception/
│   └── GlobalExceptionHandler.java    # @RestControllerAdvice
└── infra/
    └── PushoverClient.java        # Alert notifications
```

## 3. Important Files & Classes

### Entry Point
- `TwitterApplication.java` - Main class (Java 21, Spring Boot 3.2.0, port 5000)
- Annotations: `@EnableCaching`, `@EnableScheduling`, `@EnableAsync`

### REST Controllers
| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| `TwitterController` | `/tweet/{id}`, `/user/{idOrHandle}`, `/community/{id}`, `/follows`, `/communities`, `/health` | Main Twitter API relay |
| `MetricsController` | `/metrics`, `/metrics/all` | Latency and performance metrics |
| `CreditController` | Credit endpoints | Get/add/remove credits |
| `UsageController` | Usage endpoints | View API usage stats |

### Core Services
| Service | Purpose |
|---------|---------|
| `TwitterService` | Caching, billing, batch operations, request deduplication |
| `SmartTwitterDataProvider` | Routes to primary source, handles fallback on errors |
| `SynopticDataProvider` | Synoptic API wrapper with rate limiting |
| `TwitterApiDataProvider` | Twitterapi.io wrapper |
| `SourceHealthMonitor` | Monitors error rates (3-min rolling windows) |
| `CreditService` | In-memory credit tracking with periodic DynamoDB flush |
| `UsageTrackingService` | In-memory usage accumulation with periodic flush |
| `UsernameCacheService` | Username→userId mapping cache |

### Configuration
- `application.yml` - Main configuration (API keys, cache TTLs, DynamoDB tables)
- `application-local.yml` - Local development overrides

## 4. Data Flows

### Single Tweet Request
```
GET /tweet/{id} (x-api-key header)
    ↓
ApiKeyInterceptor validates API key
    ↓
TwitterController.getTweet()
    ↓
TwitterService.getTweet()
    ├→ Check Caffeine cache
    ├→ If HIT: check billing period, charge if expired
    └→ If MISS: call SmartTwitterDataProvider
    ↓
SmartTwitterDataProvider
    ├→ Route to primary source (SYNOPTIC by default)
    ├→ If error: record in SourceHealthMonitor
    └→ If health issues: fallback to TwitterApiDataProvider
    ↓
SynopticClient
    ├→ Wait for rate limit (40 req/sec)
    └→ WebClient GET /tweets/lookup?tweet_ids={id}
    ↓
SynopticToAxiomMapper converts response
    ↓
Cache result, deduct credit, track usage
    ↓
Return AxionTweetDto
```

### Billing Strategy
```
CachedData<T> contains:
- data: The cached value
- cachedAt: When fetched (for TTL)
- billedAt: When last charged (for billing period)

Example: tweets with ttl=15min, billing=4min
- 0:00 - Fetch & charge (cache miss)
- 0:02 - Cache hit, no charge (within billing period)
- 0:05 - Cache hit, charge (billing period expired)
- 0:16 - Cache expired, fetch fresh & charge
```

### Scheduled Tasks (every 5 seconds)
```
UsageTrackingService.flushToDynamoDB()  → Batch write usage
UsernameCacheService.flushToDynamoDB()  → Write new username mappings
CreditService.flushDecrements()         → Batch decrement credits
```

## 5. External Dependencies

### Third-Party APIs
| Service | URL | Auth | Rate Limit |
|---------|-----|------|------------|
| Synoptic | `https://twttr.api.synoptic.com` | x-api-key | 40 req/sec |
| Twitterapi.io | `https://api.twitterapi.io/twitter` | X-API-Key | N/A |
| Pushover | `https://api.pushover.net/1/messages.json` | API key | N/A |

### Video Proxy
- Twitter blocks direct video.twimg.com requests
- URLs proxied through: `https://twproxy.twproxy.workers.dev/?url={encoded-url}`

### Databases
- **DynamoDB** (us-east-1) - Usage, credits, username cache

### Environment Variables
```yaml
synoptic.api.api-key: <synoptic-key>
twitterapi.api-key: <twitterapi-key>
aws.region: us-east-1
security.api-keys: [<list of valid client API keys>]
pushover.api-key: <optional>
pushover.user-key: <optional>
```

## 6. AWS Resources & Deployment

### Region: us-east-1
- **Elastic Beanstalk** - Application hosting
- **CloudWatch Logs** - Application logs

### DynamoDB Tables (all in us-east-1)
| Table | Purpose | Keys |
|-------|---------|------|
| `twitter-relay-usage` | API call tracking | PK: apiKeyHash, SK: endpoint#minuteBucket |
| `twitter-relay-credits` | Credit balances | PK: apiKey |
| `twitter-username-cache` | Username→userId mapping | PK: username |

### CodePipeline
- Region: us-west-2
- Build: AWS CodeBuild (`buildspec.yml`)

### Deployment Configuration
- `.ebextensions/logs.config` - Log rotation
- `.platform/nginx/` - Nginx reverse proxy
- `Procfile` - Java process definition

### Build & Run
```bash
# Build
mvn compile

# Run locally
mvn spring-boot:run

# Port: 5000
```

## 7. Common Patterns

### Architecture
- **Layered design**: Controller → Service → Provider → Client
- **Smart fallback**: Primary source with automatic failover
- **In-memory + async flush**: Fast reads, eventual consistency

### In-Memory Cache + Async Flush
```java
// Fast concurrent read/write
ConcurrentHashMap + LongAdder

// Periodic background flush (every 5 sec)
@Scheduled → DynamoDB batch write
```

### Request Coalescing
```java
// Deduplicates concurrent requests for same resource
RequestCoalescer<T>
- First request wins, calls API
- Other requests wait and share result
```

### Health Monitoring
```java
SourceHealthMonitor
- 3-minute rolling window
- Tracks errors and latency
- Triggers fallback when issues detected
```

### Coding Conventions
- Constructor injection for dependencies
- `@Async` for non-blocking operations
- `ConcurrentHashMap` + `AtomicLong` for thread safety
- Caffeine for in-memory caching
- Resilience4j for rate limiting

### Where to Add New Features
- **New endpoint**: `controller/` - add to TwitterController
- **New data source**: `provider/` - implement TwitterDataProvider
- **New cache**: `config/CacheConfig.java` - add Caffeine cache
- **New mapper**: `mapper/` - create mapper class

## 8. Gotchas & Notes

### Billing vs Cache TTL
- **Independent timers**: billing-period can differ from cache TTL
- Setting billing-period=0 charges on every request (even cached)
- Example: ttl=15min, billing=4min means charges at 0, 4, 8, 12 min

### In-Memory State
- Credits and usage are in-memory with periodic DynamoDB flush
- Unsaved data may be lost on ungraceful shutdown
- Username cache loads entire DynamoDB table at startup

### Source Health Monitoring
- Requires 3 minutes of data before triggering fallback
- Brief outages within startup window won't failover
- Health window is rolling (old data drops off)

### Rate Limiting
- Synoptic has 40 req/sec per endpoint type (tweet, user-by-id, etc.)
- Rate limiters are independent per endpoint
- Total could be 160 req/sec across all endpoint types

### API Key Validation
- Simple list.contains() check
- Keys stored as SHA-256 hashes in application.yml
- No per-key rate limiting (only Synoptic rate limiting)

### Video Proxy
- Essential for Synoptic responses (Twitter blocks direct video URLs)
- If proxy fails, video URLs return 403
- Proxied through Cloudflare Worker

### DynamoDB Consistency
- All writes are async fire-and-forget
- Brief inconsistency between memory and database is expected
- Not suitable for real-time audit trails

### Logging
- Uses `System.out.println()` to stdout
- No structured logging
- Captured by Elastic Beanstalk → CloudWatch

### Performance Notes
- **Bottleneck**: Synoptic 40 req/sec rate limit
- **Optimizations**: Request coalescing, Caffeine cache, async DynamoDB
- **Connection pooling**: WebClient with 50 max connections
