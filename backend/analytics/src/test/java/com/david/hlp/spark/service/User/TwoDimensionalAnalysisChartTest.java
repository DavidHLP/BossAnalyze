package com.david.hlp.spark.service.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwoDimensionalAnalysisChartTest {

    @Test
    void salaryMidpointIsNotTruncatedAndUnknownFormatsStayUnknown() {
        assertEquals(4.5, TwoDimensionalAnalysisChart.extractSalaryValue("4-5K"));
        assertEquals(22.5, TwoDimensionalAnalysisChart.extractSalaryValue("20-25K"));
        assertEquals(4.0, TwoDimensionalAnalysisChart.extractSalaryValue("4K"));
        assertNull(TwoDimensionalAnalysisChart.extractSalaryValue(null));
        assertNull(TwoDimensionalAnalysisChart.extractSalaryValue(""));
        assertNull(TwoDimensionalAnalysisChart.extractSalaryValue("面议"));
        assertNull(TwoDimensionalAnalysisChart.extractSalaryValue("4.5-5K"));
    }

    @Test
    void experienceAndDegreeMappingsPreserveExplicitZeroAndRejectUnknownValues() {
        assertEquals(0, TwoDimensionalAnalysisChart.mapExperienceValue("不限"));
        assertEquals(0, TwoDimensionalAnalysisChart.mapExperienceValue("应届"));
        assertEquals(4, TwoDimensionalAnalysisChart.mapExperienceValue("3-5年"));
        assertNull(TwoDimensionalAnalysisChart.mapExperienceValue("未填写"));
        assertNull(TwoDimensionalAnalysisChart.mapExperienceValue(null));

        assertEquals(0, TwoDimensionalAnalysisChart.mapDegreeValue("不限"));
        assertNotNull(TwoDimensionalAnalysisChart.mapDegreeValue("本科"));
        assertNull(TwoDimensionalAnalysisChart.mapDegreeValue("未知学历"));
        assertNull(TwoDimensionalAnalysisChart.mapDegreeValue(""));
    }
}
