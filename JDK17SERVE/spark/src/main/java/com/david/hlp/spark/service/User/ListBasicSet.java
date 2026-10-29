package com.david.hlp.spark.service.User;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListBasicSet {
    private final SparkSession sparkSession;
    private final Environment environment;

    public List<String> getCityNameList() {
        return sparkSession.read().format("jdbc")
                .option("url", environment.getRequiredProperty("spring.datasource.url"))
                .option("driver", environment.getRequiredProperty("spring.datasource.driver-class-name"))
                .option("user", environment.getRequiredProperty("spring.datasource.username"))
                .option("password", environment.getRequiredProperty("spring.datasource.password"))
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load()
                .select("city_name")
                .groupBy("city_name")
                .agg(functions.count("*").alias("count"))
                .orderBy(functions.desc("count"))
                .select("city_name")
                .distinct()
                .collectAsList()
                .stream()
                .map(row -> row.getString(0))
                .collect(Collectors.toList());
    }

    public List<String> getPositionNameList(String cityName) {
        Dataset<Row> query = sparkSession.read().format("jdbc")
                .option("url", environment.getRequiredProperty("spring.datasource.url"))
                .option("driver", environment.getRequiredProperty("spring.datasource.driver-class-name"))
                .option("user", environment.getRequiredProperty("spring.datasource.username"))
                .option("password", environment.getRequiredProperty("spring.datasource.password"))
                .option("dbtable", "t_job_detail")
                .option("fetchsize", "10000")
                .load();

        if (cityName != null && !cityName.equals("all")) {
            query = query.where("city_name = '" + cityName + "'");
        }

        return query.select("position_name")
                .groupBy("position_name")
                .agg(functions.count("*").alias("count"))
                .orderBy(functions.desc("count"))
                .select("position_name")
                .distinct()
                .collectAsList()
                .stream()
                .map(row -> row.getString(0))
                .collect(Collectors.toList());
    }
}
