# Data Import & Export Strategy

## Supported Formats

| Operation | Formats |
|-----------|---------|
| Import | CSV, Excel, JSON |
| Export | CSV, Excel, JSON, PDF |
| Backup | SQL dump (ops), Parquet (analytics) |

---

## Import Pipeline

```
Upload to S3 (Media Service, max 100MB)
    → Validate schema + field mapping
    → Async workflow (Workflow Service)
    → Batch insert (1000 rows/batch)
    → Duplicate detection
    → Summary notification
```

Owner: **Lead Service** / **Customer Service** for domain data.

---

## Export Pipeline

```
Filter query → Stream results → Write temp file → Upload S3 → Pre-signed URL (24h)
```

Never load full dataset into memory — use streaming.

---

## Related

- Media Service for file storage
- Workflow Service for async processing
