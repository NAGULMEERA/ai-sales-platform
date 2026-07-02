# Platform cache (Redis)

Tenant-aware read-through caching for business services.

## Enable

```yaml
spring:
  config:
    import:
      - optional:classpath:platform/application-cache.yml

aisales:
  cache:
    enabled: true
```

Requires Redis (`spring.data.redis.*`).

## Usage

```java
@Service
@RequiredArgsConstructor
public class LeadQueryService {

    private static final String NS = "lead";

    private final PlatformCacheService cache;
    private final LeadRepository leadRepository;

    public LeadResponse getById(UUID id) {
        return cache.getOrLoad(NS, id.toString(), LeadResponse.class,
                () -> mapper.toResponse(leadRepository.findById(id).orElseThrow()));
    }

    public void onLeadUpdated(UUID id) {
        cache.evict(NS, id.toString());
    }
}
```

## Key format

| Context | Pattern |
|---------|---------|
| Tenant request | `{prefix}:tenant:{tenantId}:{namespace}:{key}` |
| Platform admin / no tenant | `{prefix}:platform:{namespace}:{key}` |

Default prefix: `aisales`.

## Operations

| Method | Purpose |
|--------|---------|
| `get` | Read cached value |
| `getOrLoad` | Read-through with loader |
| `put` | Write with TTL |
| `evict` | Invalidate single entry |
| `evictNamespace` | Invalidate all keys in namespace for current tenant |
