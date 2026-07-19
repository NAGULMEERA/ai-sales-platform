# ADR-032: Business Analytics Facts

**Status:** Accepted  
**Date:** 2026-07-19  
**Owner:** analytics-service

## Context

Dashboards need lead funnel, opportunity pipeline, AI usage, and conversion trends without querying other services’ databases.

## Decision

1. **analytics-service** records tenant-scoped facts into `analytics_event` and daily rollups (`analytics_daily_rollup`).
2. Facts are produced by `AnalyticsEventConsumer` from integration events (leads, customers, opportunities, conversations, workflows, AI, catalog, RAG).
3. Query APIs under `/api/v1/analytics/**` (`AnalyticsController`) expose dashboard, funnel, sources, trends, pipeline, customer growth, conversion, and AI usage.
4. Metric names are centralized in `AnalyticsMetricNames`.

## Consequences

- Analytics is eventually consistent.
- Grouped count queries are used for dashboard assembly to avoid N+1 fact scans.
- Gateway rate-limits `/api/v1/analytics/**`.

## Related code

- `backend/services/analytics-service/.../AnalyticsRecordingService.java`
- `backend/services/analytics-service/.../AnalyticsQueryService.java`
