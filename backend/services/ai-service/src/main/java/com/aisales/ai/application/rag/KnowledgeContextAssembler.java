package com.aisales.ai.application.rag;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KnowledgeContextAssembler {

    public String assemble(List<RetrievedKnowledgeChunkDto> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (RetrievedKnowledgeChunkDto chunk : chunks) {
            if (!StringUtils.hasText(chunk.getContent())) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("[").append(i++).append("] ").append(chunk.getContent().trim());
        }
        return sb.toString();
    }
}
