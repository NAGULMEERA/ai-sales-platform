# Plugins

| Module | Role |
|--------|------|
| `plugin-sdk` | Contracts only (`PluginDescriptor`, capability/industry SPI) |
| `capability/email-channel-plugin` | Email channel metadata stub |
| `capability/whatsapp-channel-plugin` | WhatsApp channel metadata stub |
| `industry/real-estate-plugin` | Real estate metadata stub |

Registry APIs live in `services/marketplace-service`. Platform Core services must not depend on industry plugin jars.
