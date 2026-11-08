package com.david.hlp.spark.service.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TwoDimensionalAnalysisChartTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void salaryMidpointsAndUnknownRequirementsKeepTheirMeaning() {
        assertEquals(4.5, TwoDimensionalAnalysisChart.extractSalary("4-5K"));
        assertEquals(4.0, TwoDimensionalAnalysisChart.extractSalary("4K"));
        assertEquals(5.0, TwoDimensionalAnalysisChart.extractSalary("4-6K"));
        assertNull(TwoDimensionalAnalysisChart.extractSalary("面议"));
        assertNull(TwoDimensionalAnalysisChart.extractSalary(null));
        for (String unknown : Arrays.asList(null, "", "  ", "未知")) {
            assertNull(TwoDimensionalAnalysisChart.mapDegree(unknown));
            assertNull(TwoDimensionalAnalysisChart.mapExperience(unknown));
        }
        assertEquals(0, TwoDimensionalAnalysisChart.mapDegree("学历不限"));
        assertEquals(0, TwoDimensionalAnalysisChart.mapExperience("经验不限"));
        assertEquals(4, TwoDimensionalAnalysisChart.mapDegree("本科"));
        assertEquals(2, TwoDimensionalAnalysisChart.mapExperience("1-3年"));
    }

    @Test
    void isolatedRowsPreserveDoubleAndNullThroughUdfsFiltersBeanEncoderAndJson() throws Exception {
        // Only synthetic rows: no Spring context, JDBC source or existing Spark service.
        try (SparkSession spark = SparkSession.builder().master("local[1]")
                .appName("recruitment-contract-test")
                .config("spark.ui.enabled", "false")
                .config("spark.driver.host", "127.0.0.1")
                .config("spark.driver.bindAddress", "127.0.0.1")
                .config("spark.sql.warehouse.dir", temporaryDirectory.toUri().toString())
                .getOrCreate()) {
            StructType schema = new StructType();
            for (String field : new String[] { "position_id", "position_name", "city_name", "salary", "degree",
                    "experience", "companyName", "companySize", "financingStage", "companyUrl", "jobUrl",
                    "address", "employeeBenefits", "jobRequirements" }) {
                schema = schema.add(field, DataTypes.StringType, true);
            }
            var source = spark.createDataFrame(Arrays.asList(
                    sample("1", "4-5K", "不限", "不限"),
                    sample("2", "5K", "未知", "1-3年"),
                    sample("3", "6K", "本科", null),
                    sample("4", null, "本科", "1-3年")), schema);
            var service = new TwoDimensionalAnalysisChart(spark, null);
            var processed = service.processJobData(source);
            assertEquals(DataTypes.DoubleType, processed.schema().apply("salary_value").dataType());
            var salaryDegree = service.buildResultDataList(service.applyFilters(
                    processed, "all", "all", "salary_value", "degree_value"));
            assertEquals(2, salaryDegree.size());
            assertEquals(4.5, salaryDegree.get(0).getSalaryValue());
            assertEquals(0, salaryDegree.get(0).getDegreeValue());
            assertNull(salaryDegree.get(1).getExperienceValue());
            var salaryExperience = service.buildResultDataList(service.applyFilters(
                    processed, "all", "all", "salary_value", "experience_value"));
            assertEquals(2, salaryExperience.size());
            assertNull(salaryExperience.get(1).getDegreeValue());
            var json = new ObjectMapper().valueToTree(salaryDegree.get(0));
            assertEquals(4.5, json.get("salaryValue").doubleValue());
            var degreeExperience = service.buildResultDataList(service.applyFilters(
                    processed, "all", "all", "degree_value", "experience_value"));
            assertEquals(2, degreeExperience.size());
            assertNull(degreeExperience.get(1).getSalaryValue());
            assertTrue(new ObjectMapper().valueToTree(degreeExperience.get(1)).get("salaryValue").isNull());

            var cities = spark.createDataFrame(Arrays.asList(
                    RowFactory.create("普通城市", "普通职位"),
                    RowFactory.create("O'Reilly", "含引号职位"),
                    RowFactory.create("' OR 1=1 --", "表达式文本职位")),
                    new StructType().add("city_name", DataTypes.StringType)
                            .add("position_name", DataTypes.StringType));
            var options = new ListBasicSet(spark, null);
            assertEquals(Arrays.asList("含引号职位"), options.positionNames(cities, "O'Reilly"));
            assertEquals(Arrays.asList("表达式文本职位"), options.positionNames(cities, "' OR 1=1 --"));
            assertTrue(options.positionNames(cities, "' OR true --").isEmpty());
            assertEquals(3, options.positionNames(cities, "all").size());
            assertEquals(3, options.positionNames(cities, null).size());
            assertSoftDeleteTypesAcrossRecruitmentReaders(spark);
        }
    }

    private void assertSoftDeleteTypesAcrossRecruitmentReaders(SparkSession spark) {
        // MySQL TINYINT(1) is exposed as Boolean; older fixtures used integers.
        for (var type : Arrays.asList(DataTypes.BooleanType, DataTypes.IntegerType)) {
            boolean bool = type.equals(DataTypes.BooleanType);
            var schema = new StructType().add("city_name", DataTypes.StringType)
                    .add("position_name", DataTypes.StringType).add("is_deleted", type, true);
            Object active = bool ? Boolean.FALSE : Integer.valueOf(0);
            Object deleted = bool ? Boolean.TRUE : Integer.valueOf(1);
            var rows = new ArrayList<Row>();
            rows.add(RowFactory.create("active-city", "active-job", active));
            rows.add(RowFactory.create("deleted-city", "deleted-job", deleted));
            rows.add(RowFactory.create("unknown-city", "unknown-job", null));
            if (!bool) rows.add(RowFactory.create("other-city", "other-job", 2));
            var filtered = ListBasicSet.activeRows(spark.createDataFrame(rows, schema));
            assertEquals(1, filtered.count());
            assertEquals("active-city", filtered.first().getString(0));
            assertEquals(Arrays.asList("active-job"),
                    new ListBasicSet(spark, null).positionNames(filtered, "all"));
        }
    }

    private Row sample(String id, String salary, String degree, String experience) {
        return RowFactory.create(id, "测试职位", "测试城市", salary, degree, experience,
                "合成公司", null, null, null, null, null, null, null);
    }
    @Test
    void salaryMidpointIsNotTruncatedAndUnknownFormatsStayUnknown() {
        assertEquals(4.5, TwoDimensionalAnalysisChart.extractSalary("4-5K"));
        assertEquals(22.5, TwoDimensionalAnalysisChart.extractSalary("20-25K"));
        assertEquals(4.0, TwoDimensionalAnalysisChart.extractSalary("4K"));
        assertNull(TwoDimensionalAnalysisChart.extractSalary(null));
        assertNull(TwoDimensionalAnalysisChart.extractSalary(""));
        assertNull(TwoDimensionalAnalysisChart.extractSalary("面议"));
        assertNull(TwoDimensionalAnalysisChart.extractSalary("4.5-5K"));
    }

    @Test
    void experienceAndDegreeMappingsPreserveExplicitZeroAndRejectUnknownValues() {
        assertEquals(0, TwoDimensionalAnalysisChart.mapExperience("不限"));
        assertEquals(0, TwoDimensionalAnalysisChart.mapExperience("应届"));
        assertEquals(4, TwoDimensionalAnalysisChart.mapExperience("3-5年"));
        assertNull(TwoDimensionalAnalysisChart.mapExperience("未填写"));
        assertNull(TwoDimensionalAnalysisChart.mapExperience(null));

        assertEquals(0, TwoDimensionalAnalysisChart.mapDegree("不限"));
        assertNotNull(TwoDimensionalAnalysisChart.mapDegree("本科"));
        assertNull(TwoDimensionalAnalysisChart.mapDegree("未知学历"));
        assertNull(TwoDimensionalAnalysisChart.mapDegree(""));
    }
}
