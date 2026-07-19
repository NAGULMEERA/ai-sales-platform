package com.aisales.search.application.audit;

import com.aisales.common.core.audit.Auditable;
import org.springframework.stereotype.Component;

@Component
public class SearchAuditor {

    @Auditable(action = "SEARCH_EXECUTED", resourceType = "SEARCH")
    public void searchExecuted(String query) {
    }
}
