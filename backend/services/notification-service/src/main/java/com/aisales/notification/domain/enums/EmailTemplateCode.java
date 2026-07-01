package com.aisales.notification.domain.enums;

public enum EmailTemplateCode {
    EMAIL_VERIFICATION,
    PASSWORD_RESET;

    public static EmailTemplateCode from(String code) {
        return EmailTemplateCode.valueOf(code);
    }
}
