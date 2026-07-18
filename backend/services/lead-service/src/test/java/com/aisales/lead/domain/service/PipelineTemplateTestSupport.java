package com.aisales.lead.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class PipelineTemplateTestSupport {

    private PipelineTemplateTestSupport() {
    }

    public static PipelineTemplateRegistry registry() {
        return new PipelineTemplateRegistry(new ClasspathPipelineTemplateLoader(new ObjectMapper()));
    }
}
