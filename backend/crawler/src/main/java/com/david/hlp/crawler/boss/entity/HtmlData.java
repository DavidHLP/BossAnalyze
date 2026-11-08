package com.david.hlp.crawler.boss.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HtmlData {
    private Integer id;
    private String url;
    private String htmlContent;
    private String baseCity;
    private String basePosition;
    private String baseCityCode;
    private String basePositionCode;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    @Builder.Default
    private Integer status = 0;
    @Builder.Default
    private Boolean htmlContentException = false;
}
