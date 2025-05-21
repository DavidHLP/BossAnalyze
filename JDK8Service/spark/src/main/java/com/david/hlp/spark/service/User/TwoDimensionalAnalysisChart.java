package com.david.hlp.spark.service.User;

import static org.apache.spark.sql.functions.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.david.hlp.spark.model.User.JobAnalysisData;

@Service
@RequiredArgsConstructor
public class TwoDimensionalAnalysisChart implements Serializable {
    private static final long serialVersionUID = 1L;
    private final SparkSession sparkSession;

    // 经验关键词到数值的映射
    private static final Map<String, Integer> EXPERIENCE_MAP = new HashMap<>();
    // 学历关键词到数值的映射
    private static final Map<String, Integer> DEGREE_MAP = new HashMap<>();

    static {
        // 初始化经验映射
        EXPERIENCE_MAP.put("应届", 0);
        EXPERIENCE_MAP.put("无需", 0);
        EXPERIENCE_MAP.put("不限", 0);
        EXPERIENCE_MAP.put("1年以内", 1);
        EXPERIENCE_MAP.put("1-3", 2);
        EXPERIENCE_MAP.put("3-5", 4);
        EXPERIENCE_MAP.put("5-10", 7);
        EXPERIENCE_MAP.put("10年以上", 10);

        // 初始化学历映射
        DEGREE_MAP.put("不限", 0);
        DEGREE_MAP.put("初中", 1);
        DEGREE_MAP.put("高中", 2);
        DEGREE_MAP.put("中专", 2);
        DEGREE_MAP.put("大专", 3);
        DEGREE_MAP.put("本科", 4);
        DEGREE_MAP.put("硕士", 5);
        DEGREE_MAP.put("博士", 6);
    }

    /**
     * 获取职位二维分析数据
     * 
     * @param cityName     城市名称，如果为"all"则不筛选城市
     * @param positionName 职位名称，如果为"all"则不筛选职位
     * @param xAxis        X轴类型：salary_value, degree_value, experience_value
     * @param yAxis        Y轴类型：salary_value, degree_value, experience_value
     * @return 符合筛选条件的职位分析数据列表
     */
    public List<JobAnalysisData> getJobAnalysisData(String cityName, String positionName,
            String xAxis, String yAxis) {
        // 创建职位数据的JSON解析Schema
        StructType jsonSchema = createJobDataSchema();

        // 从MySQL读取并处理数据
        Dataset<Row> jobDf = readJobDataFromDatabase(jsonSchema);

        // 处理薪资、学历和经验数据
        Dataset<Row> processedDf = processJobData(jobDf);

        // 应用城市和职位的过滤条件
        processedDf = applyFilters(processedDf, cityName, positionName, xAxis, yAxis);

        // 选择需要的列并构建返回结果
        return buildResultDataList(processedDf);
    }

    /**
     * 创建职位数据的JSON解析Schema
     */
    private StructType createJobDataSchema() {
        return DataTypes.createStructType(new StructField[] {
                DataTypes.createStructField("basicInfo", DataTypes.createStructType(new StructField[] {
                        DataTypes.createStructField("salary", DataTypes.StringType, true),
                        DataTypes.createStructField("degree", DataTypes.StringType, true),
                        DataTypes.createStructField("experience", DataTypes.StringType, true),
                        DataTypes.createStructField("address", DataTypes.StringType, true)
                }), true),
                DataTypes.createStructField("companyInfo", DataTypes.createStructType(new StructField[] {
                        DataTypes.createStructField("companyName", DataTypes.StringType, true),
                        DataTypes.createStructField("companySize", DataTypes.StringType, true),
                        DataTypes.createStructField("financingStage", DataTypes.StringType, true)
                }), true),
                DataTypes.createStructField("companyUrl", DataTypes.StringType, true)
        });
    }

    /**
     * 从MySQL数据库读取职位数据
     */
    private Dataset<Row> readJobDataFromDatabase(StructType jsonSchema) {
        return sparkSession.read().format("jdbc")
                .option("url", "jdbc:mysql://localhost:3306/boss_data")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("user", "root")
                .option("password", "Alone117")
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                .select("position_id", "position_name", "detail_data", "city_name", "html_url", "employee_benefits",
                        "job_requirements")
                .withColumn("jsonData", from_json(col("detail_data"), jsonSchema))
                .select(
                        col("position_id"),
                        col("position_name"),
                        col("city_name"),
                        col("position_name"),
                        col("html_url").alias("jobUrl"),
                        col("jsonData.basicInfo.salary").alias("salary"),
                        col("jsonData.basicInfo.degree").alias("degree"),
                        col("jsonData.basicInfo.experience").alias("experience"),
                        col("jsonData.basicInfo.address").alias("address"),
                        col("jsonData.companyInfo.companyName").alias("companyName"),
                        col("jsonData.companyInfo.companySize").alias("companySize"),
                        col("jsonData.companyInfo.financingStage").alias("financingStage"),
                        col("jsonData.basicInfo.address").alias("address"),
                        col("jsonData.companyUrl").alias("companyUrl"),
                        // 处理数组格式的字符串：移除方括号并压缩空格
                        regexp_replace(regexp_replace(col("employee_benefits"), "\\[|\\]", ""), "\\s+", "")
                                .alias("employeeBenefits"),
                        regexp_replace(regexp_replace(col("job_requirements"), "\\[|\\]", ""), "\\s+", "")
                                .alias("jobRequirements"));
    }

    /**
     * 处理职位数据：提取薪资值、学历值和经验值
     */
    private Dataset<Row> processJobData(Dataset<Row> jobDf) {
        // 注册UDF函数用于提取薪资数值
        registerSalaryExtractorUdf();

        // 注册UDF函数用于映射经验值
        registerExperienceMapperUdf();

        // 注册UDF函数用于映射学历值
        registerDegreeMapperUdf();

        // 应用UDF函数添加处理后的列
        return jobDf
                .withColumn("salary_value", callUDF("extractSalary", col("salary")))
                .withColumn("experience_value", callUDF("mapExperience", col("experience")))
                .withColumn("degree_value", callUDF("mapDegree", col("degree")));
    }

    /**
     * 注册薪资提取UDF函数
     */
    private void registerSalaryExtractorUdf() {
        sparkSession.udf().register("extractSalary", (String salaryStr) -> {
            if (salaryStr == null || salaryStr.isEmpty()) {
                return null;
            }

            try {
                Pattern singlePattern = Pattern.compile("^(\\d+)K$");
                Pattern rangePattern = Pattern.compile("^(\\d+)-(\\d+)K$");

                Matcher singleMatcher = singlePattern.matcher(salaryStr);
                Matcher rangeMatcher = rangePattern.matcher(salaryStr);

                if (singleMatcher.matches()) {
                    // 单一数值，如"4K"
                    return Integer.parseInt(singleMatcher.group(1));
                } else if (rangeMatcher.matches()) {
                    // 范围值，如"4-5K"
                    int lower = Integer.parseInt(rangeMatcher.group(1));
                    int upper = Integer.parseInt(rangeMatcher.group(2));
                    return (lower + upper) / 2;
                } else {
                    // 其他格式忽略
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }, DataTypes.IntegerType);
    }

    /**
     * 注册经验映射UDF函数
     */
    private void registerExperienceMapperUdf() {
        sparkSession.udf().register("mapExperience", (String expStr) -> {
            if (expStr == null || expStr.isEmpty()) {
                return 0;
            }
            
            // 遍历映射表查找匹配的关键词
            return EXPERIENCE_MAP.entrySet().stream()
                .filter(entry -> expStr.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0);
        }, DataTypes.IntegerType);
    }

    /**
     * 注册学历映射UDF函数
     */
    private void registerDegreeMapperUdf() {
        sparkSession.udf().register("mapDegree", (String degreeStr) -> {
            if (degreeStr == null || degreeStr.isEmpty()) {
                return 0;
            }
            
            // 遍历映射表查找匹配的关键词
            return DEGREE_MAP.entrySet().stream()
                .filter(entry -> degreeStr.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0);
        }, DataTypes.IntegerType);
    }

    /**
     * 应用城市和职位的过滤条件
     */
    private Dataset<Row> applyFilters(Dataset<Row> processedDf, String cityName, String positionName, String xAxis,
            String yAxis) {
        // 应用过滤条件
        if (cityName != null && !cityName.isEmpty() && !"all".equalsIgnoreCase(cityName)) {
            processedDf = processedDf.filter(col("city_name").equalTo(cityName));
        }

        if (positionName != null && !positionName.isEmpty() && !"all".equalsIgnoreCase(positionName)) {
            processedDf = processedDf.filter(col("position_name").equalTo(positionName));
        }

        // 确保X轴和Y轴的值不为空
        processedDf = processedDf.filter(
                col(xAxis).isNotNull().and(col(yAxis).isNotNull()));

        // 排序数据
        return processedDf.orderBy(col(xAxis), col(yAxis));
    }

    /**
     * 解析逗号分隔的字符串为List
     */
    private List<String> parseStringList(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Arrays.asList("职位福利目前未解析");
        }
        List<String> result = Arrays.asList(input.split(","));
        return result.isEmpty() ? Arrays.asList("职位福利目前未解析") : result;
    }

    /**
     * 解析工作需求字符串为List
     */
    private List<String> parseJobRequirements(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Arrays.asList("工作需求目前未解析");
        }
        List<String> result = Arrays.asList(input.split(","));
        return result.isEmpty() ? Arrays.asList("工作需求目前未解析") : result;
    }

    /**
     * 构建结果数据列表
     */
    private List<JobAnalysisData> buildResultDataList(Dataset<Row> echartsDf) {
        return echartsDf.map(
                (MapFunction<Row, JobAnalysisData>) row -> {
                    return JobAnalysisData.builder()
                            .positionId(row.getString(row.fieldIndex("position_id")))
                            .positionName(row.getString(row.fieldIndex("position_name")))
                            .cityName(row.getString(row.fieldIndex("city_name")))
                            .salary(row.getString(row.fieldIndex("salary")))
                            .salaryValue(getIntValue(row, "salary_value"))
                            .degree(row.getString(row.fieldIndex("degree")))
                            .degreeValue(getIntValue(row, "degree_value"))
                            .experience(row.getString(row.fieldIndex("experience")))
                            .experienceValue(getIntValue(row, "experience_value"))
                            .companyName(row.getString(row.fieldIndex("companyName")))
                            .companySize(row.getString(row.fieldIndex("companySize")))
                            .financingStage(row.getString(row.fieldIndex("financingStage")))
                            .companyUrl(row.getString(row.fieldIndex("companyUrl")))
                            .JobUrl(row.getString(row.fieldIndex("jobUrl")))
                            .address(row.getString(row.fieldIndex("address")))
                            .employeeBenefits(parseStringList(row.getString(row.fieldIndex("employeeBenefits"))))
                            .jobRequirements(parseJobRequirements(row.getString(row.fieldIndex("jobRequirements"))))
                            .build();
                },
                Encoders.bean(JobAnalysisData.class)).collectAsList();
    }

    /**
     * 安全获取Row中的整数值
     */
    private Integer getIntValue(Row row, String fieldName) {
        return row.isNullAt(row.fieldIndex(fieldName)) ? null : row.getInt(row.fieldIndex(fieldName));
    }
}
