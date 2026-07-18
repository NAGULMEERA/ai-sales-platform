# lead-service — platform adoption

## Enabled

- Observability, cache config import
- Outbox event publishing + inbox tables
- PostgreSQL RLS on tenant-owned tables
- Full Lead lifecycle API and domain state machine

## Implemented

1. Aggregate `Lead` with invariants + `LeadStateMachine`
2. Child entities: assignments, status history, activities, notes, scores, followups, duplicates
3. REST `/api/v1/leads/**` lifecycle + notes/followups/history/duplicates
4. Events: Created, Validated, Qualified, Assigned, Contacted, Scored, Converted, Lost, StatusChanged
5. Duplicate detection on create (exact phone/email)

See `README.md` and `docs/08-api/lead-service-openapi.yaml`.
