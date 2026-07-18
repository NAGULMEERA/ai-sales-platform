# ADR-025: Catalog

**Label:** ADR-006 (Catalog)  
**Status:** Accepted  
**Date:** 2026-07-18

## Context

Sellable items differ wildly (properties, vehicles, courses). A PropertyMatchingService in Platform Core would freeze the wrong abstraction.

## Decision

1. **catalog-service** owns industry-agnostic `CatalogProduct` / `CatalogOffer` and deterministic match APIs.
2. Industry specifics live in **attributes JSON** (and future plugin catalog schemas), not Platform Core tables.
3. Quotes/opportunities reference catalog IDs and snapshot prices; catalog remains pricing master.
4. Matching is not AI in MVP; AI may recommend, Business Services decide.

## Consequences

- Real Estate: product attributes like location/bedrooms; Automobile: make/model/finance flags — same product/offer APIs.
- Vertical validation Sprint (Matching): same match endpoint, different attribute filters per industry plugin config.
- No industry microservice owns the product master.
