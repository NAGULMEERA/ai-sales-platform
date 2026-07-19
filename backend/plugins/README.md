# Plugins

| Module | Role |
|--------|------|
| `plugin-sdk` | Contracts only (`PluginDescriptor`, capability/industry SPI) |
| `capability/email-channel-plugin` | Email channel metadata stub |
| `capability/whatsapp-channel-plugin` | WhatsApp channel metadata stub |
| `capability/meta-lead-ads-plugin` | Meta Lead Ads capability metadata |
| `industry/real-estate-plugin` | Real estate metadata |
| `industry/automobile-plugin` | Automobile metadata |
| `industry/natural-farming-plugin` | Natural farming metadata (first production vertical beyond RE/Auto) |

Registry APIs live in `services/marketplace-service`. Platform Core services must not depend on industry plugin jars.

See [Natural Farming vertical](../../docs/16-roadmap/natural-farming-vertical.md) and [ADR-034](../../docs/15-adr/adr-034-natural-farming-vertical.md).
