package com.aisales.ai.application.service;

import com.aisales.common.exception.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PromptRenderer {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final PromptVariableSanitizer variableSanitizer;

    public String render(String template, Map<String, String> variables, List<String> declaredVariables) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        Map<String, String> vars = variableSanitizer.sanitizeVariables(variables);
        List<String> missing = new ArrayList<>();
        if (declaredVariables != null) {
            for (String required : declaredVariables) {
                if (!StringUtils.hasText(vars.get(required))) {
                    missing.add(required);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new ValidationException("Missing prompt variables: " + String.join(", ", missing));
        }

        Matcher matcher = VARIABLE.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = vars.get(key);
            if (value == null) {
                throw new ValidationException("Unresolved prompt variable: " + key);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
