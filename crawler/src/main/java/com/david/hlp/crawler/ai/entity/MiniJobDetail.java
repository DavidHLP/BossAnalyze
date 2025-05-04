package com.david.hlp.crawler.ai.entity;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MiniJobDetail {
    private Long id;
    private String detailData;
    private String employeeBenefits;
    private String jobRequirements;
}
