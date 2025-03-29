package com.david.hlp.Spring.boss.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PositionData {
    private Integer id;
    private String parentId;
    private String code;
    private String name;
    private String type;
}