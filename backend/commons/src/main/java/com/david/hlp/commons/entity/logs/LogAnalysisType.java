package com.david.hlp.commons.entity.logs;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LogAnalysisType {
    COMPLETE("complete"),
    HEATMAP("heatmap"),
    IP_STATISTICS("ip-statistics"),
    HTTP_METHODS("http-methods"),
    WEEKDAY_PATTERNS("weekday-patterns"),
    BROWSER_USAGE("browser-usage");

    private final String code;

    LogAnalysisType(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static LogAnalysisType fromCode(String code) {
        for (LogAnalysisType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
