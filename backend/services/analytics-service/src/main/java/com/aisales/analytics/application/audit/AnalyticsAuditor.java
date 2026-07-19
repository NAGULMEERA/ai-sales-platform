package com.aisales.analytics.application.audit;

import com.aisales.common.core.audit.Auditable;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsAuditor {

    @Auditable(action = "ANALYTICS_DASHBOARD_VIEWED", resourceType = "ANALYTICS")
    public void dashboardViewed(String period) {
    }

    @Auditable(action = "ANALYTICS_FUNNEL_VIEWED", resourceType = "ANALYTICS")
    public void funnelViewed() {
    }
}
