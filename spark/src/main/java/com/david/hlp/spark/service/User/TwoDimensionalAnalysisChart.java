package com.david.hlp.spark.service.User;

import static org.apache.spark.sql.functions.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class TwoDimensionalAnalysisChart {
    private final SparkSession sparkSession;

    /**
     * 获取职位二维分析数据
     * 
     * @param cityName 城市名称，如果为"all"则不筛选城市
     * @param positionName 职位名称，如果为"all"则不筛选职位
     * @param xAxis    X轴类型：salary_value, degree_value, experience_value
     * @param yAxis    Y轴类型：salary_value, degree_value, experience_value
     * @return 符合筛选条件的职位分析数据列表
     */
    public List<JobAnalysisData> getJobAnalysisData(String cityName, String positionName,
            String xAxis, String yAxis) {
        // 定义JSON结构以提取字段
        StructType jsonSchema = DataTypes.createStructType(new StructField[] {
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

        // 从MySQL读取数据并解析
        Dataset<Row> jobDf = sparkSession.read().format("jdbc")
                .option("url", "jdbc:mysql://localhost:3306/boss_data")
                .option("driver", "com.mysql.cj.jdbc.Driver")
                .option("user", "root")
                .option("password", "Alone117")
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                .select("position_id", "position_name", "detail_data", "city_name")
                .withColumn("jsonData", from_json(col("detail_data"), jsonSchema))
                .select(
                        col("position_id"),
                        col("position_name"),
                        col("city_name"),
                        col("position_name"),
                        col("jsonData.basicInfo.salary").alias("salary"),
                        col("jsonData.basicInfo.degree").alias("degree"),
                        col("jsonData.basicInfo.experience").alias("experience"),
                        col("jsonData.basicInfo.address").alias("address"),
                        col("jsonData.companyInfo.companyName").alias("companyName"),
                        col("jsonData.companyInfo.companySize").alias("companySize"),
                        col("jsonData.companyInfo.financingStage").alias("financingStage"),
                        col("jsonData.companyUrl").alias("companyUrl"))
                .filter(col("salary").isNotNull().and(col("degree").isNotNull()));

        // 注册UDF函数用于提取薪资数值
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

        // 注册UDF函数用于映射经验值
        sparkSession.udf().register("mapExperience", (String expStr) -> {
            if (expStr == null || expStr.isEmpty()) {
                return 0;
            }

            if (expStr.contains("应届")) {
                return 0;
            }
            if (expStr.contains("无需") || expStr.contains("不限")) {
                return 0;
            }
            if (expStr.contains("1年以内")) {
                return 1;
            }
            if (expStr.contains("1-3")) {
                return 2;
            }
            if (expStr.contains("3-5")) {
                return 4;
            }
            if (expStr.contains("5-10")) {
                return 7;
            }
            if (expStr.contains("10年以上")) {
                return 10;
            }
            return 0;
        }, DataTypes.IntegerType);

        // 注册UDF函数用于映射学历值
        sparkSession.udf().register("mapDegree", (String degreeStr) -> {
            if (degreeStr == null || degreeStr.isEmpty()) {
                return 0;
            }

            if (degreeStr.contains("不限")) {
                return 0;
            }
            if (degreeStr.contains("初中")) {
                return 1;
            }
            if (degreeStr.contains("高中")) {
                return 2;
            }
            if (degreeStr.contains("中专")) {
                return 2;
            }
            if (degreeStr.contains("大专")) {
                return 3;
            }
            if (degreeStr.contains("本科")) {
                return 4;
            }
            if (degreeStr.contains("硕士")) {
                return 5;
            }
            if (degreeStr.contains("博士")) {
                return 6;
            }
            return 0;
        }, DataTypes.IntegerType);

        // 应用UDF函数添加处理后的列
        Dataset<Row> processedDf = jobDf
                .withColumn("salary_value", callUDF("extractSalary", col("salary")))
                .withColumn("experience_value", callUDF("mapExperience", col("experience")))
                .withColumn("degree_value", callUDF("mapDegree", col("degree")));

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

        // 选择需要的列
        Dataset<Row> echartsDf = processedDf.select(
                "position_id",
                "position_name",
                "city_name",
                "salary",
                "salary_value",
                "degree",
                "degree_value",
                "experience",
                "experience_value",
                "companyName",
                "companySize",
                "position_name",
                "financingStage",
                "companyUrl");

        // 排序数据
        echartsDf = echartsDf.orderBy(col(xAxis), col(yAxis));

        // 将Dataset转换为Java对象列表
        List<JobAnalysisData> jobDataList = echartsDf.map(
                (MapFunction<Row, JobAnalysisData>) row -> {
                    JobAnalysisData data = new JobAnalysisData();
                    data.setPositionId(row.getString(row.fieldIndex("position_id")));
                    data.setPositionName(row.getString(row.fieldIndex("position_name")));
                    data.setCityName(row.getString(row.fieldIndex("city_name")));
                    data.setSalary(row.getString(row.fieldIndex("salary")));

                    if (!row.isNullAt(row.fieldIndex("salary_value"))) {
                        data.setSalaryValue(row.getInt(row.fieldIndex("salary_value")));
                    }

                    data.setDegree(row.getString(row.fieldIndex("degree")));

                    if (!row.isNullAt(row.fieldIndex("degree_value"))) {
                        data.setDegreeValue(row.getInt(row.fieldIndex("degree_value")));
                    }

                    data.setExperience(row.getString(row.fieldIndex("experience")));

                    if (!row.isNullAt(row.fieldIndex("experience_value"))) {
                        data.setExperienceValue(row.getInt(row.fieldIndex("experience_value")));
                    }

                    data.setCompanyName(row.getString(row.fieldIndex("companyName")));
                    data.setCompanySize(row.getString(row.fieldIndex("companySize")));
                    data.setPositionName(row.getString(row.fieldIndex("position_name")));
                    data.setFinancingStage(row.getString(row.fieldIndex("financingStage")));
                    data.setCompanyUrl(row.getString(row.fieldIndex("companyUrl")));

                    return data;
                },
                Encoders.bean(JobAnalysisData.class)).collectAsList();

        return jobDataList;
    }
}
