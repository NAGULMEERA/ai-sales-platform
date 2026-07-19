package com.aisales.workflow.application.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.contracts.workflow.WorkflowConditionDto;
import com.aisales.common.contracts.workflow.WorkflowConditionType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowConditionEvaluatorTest {

    private final WorkflowConditionEvaluator evaluator = new WorkflowConditionEvaluator();

    @Test
    void shouldPassWhenLeadScoreGreaterThanThreshold() {
        boolean matched = evaluator.matches(
                List.of(WorkflowConditionDto.builder()
                        .type(WorkflowConditionType.LEAD_SCORE_GT)
                        .params(Map.of("value", 70))
                        .build()),
                Map.of("leadScore", 85));
        assertThat(matched).isTrue();
    }

    @Test
    void shouldFailWhenLeadScoreTooLow() {
        boolean matched = evaluator.matches(
                List.of(WorkflowConditionDto.builder()
                        .type(WorkflowConditionType.LEAD_SCORE_GT)
                        .params(Map.of("value", 70))
                        .build()),
                Map.of("leadScore", 40));
        assertThat(matched).isFalse();
    }

    @Test
    void shouldRequireAllConditions() {
        boolean matched = evaluator.matches(
                List.of(
                        WorkflowConditionDto.builder()
                                .type(WorkflowConditionType.CUSTOMER_EXISTS)
                                .build(),
                        WorkflowConditionDto.builder()
                                .type(WorkflowConditionType.AI_CONFIDENCE_GTE)
                                .params(Map.of("value", 0.8))
                                .build()),
                Map.of("customerExists", true, "aiConfidence", 0.5));
        assertThat(matched).isFalse();
    }

    @Test
    void shouldPassAlwaysCondition() {
        assertThat(evaluator.matches(
                        List.of(WorkflowConditionDto.builder()
                                .type(WorkflowConditionType.ALWAYS)
                                .build()),
                        Map.of()))
                .isTrue();
    }
}
