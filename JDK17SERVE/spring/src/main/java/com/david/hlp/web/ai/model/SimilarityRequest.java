package com.david.hlp.web.ai.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimilarityRequest implements Serializable {
    private List<String> city;
    private String resumeId;
    private String position;
    private String resume;
}
