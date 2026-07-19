package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PluginEnabledEvent extends BaseEvent {

    public static final String EVENT_TYPE = "PluginEnabled";

    private String pluginKey;
    private String pluginType;
    private String pluginVersion;

    public static PluginEnabledEvent of(String tenantId, String installationId, String pluginKey,
                                        String pluginType, String pluginVersion, String correlationId) {
        PluginEnabledEvent event = new PluginEnabledEvent();
        event.init(EVENT_TYPE, tenantId, installationId, correlationId);
        event.pluginKey = pluginKey;
        event.pluginType = pluginType;
        event.pluginVersion = pluginVersion;
        return event;
    }
}
