# plugin-sdk

Author contracts for **capability** and **industry** plugins.

## Rules

- Plugins contribute **metadata and configuration** (`PluginDescriptor`)
- Plugins must **not** implement business methods (`qualifyLead`, etc.)
- Plugins must **not** call vendor SDKs or own industry microservices
- Tenant enable/disable is owned by **marketplace-service**

## SPI

| Type | Interface |
|------|-----------|
| Base | `PlatformPlugin` → `descriptor()` |
| Capability | `CapabilityPlugin` |
| Industry | `IndustryPlugin` |

## Example

```java
public class EmailChannelPlugin implements CapabilityPlugin {
    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.builder()
            .pluginKey("email-channel")
            .type(PluginType.CAPABILITY)
            .version("1.0.0")
            .displayName("Email Channel")
            .capabilities(List.of("notification.email"))
            .configSchemaJson("{...}")
            .build();
    }
}
```
