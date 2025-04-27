# 用户分析

## 职位城市推荐

- `JobCityRecommender`

- input
```txt
inputdata = '数据分析师'
limitdata = 10
```

- result
```markdown
+------------+----------+
|city_name|proportion|
+------------+----------+
|        湖州|       0.8|
+------------+----------+
```

## 职位需求分析

- `JobRequirementAnalysis`

- input
```txt
final_position_name = '数据分析师'
```

- result
```markdown
| position_name | keyword_counts_json      |
|---------------|--------------------------|
| 数据分析师    | [{"数据分析/挖掘经验":11}...] |
```

## 城市职位分析

- `JobCityRecommenderAnalysis`

- input
```txt
final_position_name = '统计员'
city_name = '拉萨'
```

- result
```markdown
| city_name | position_name | keyword_counts_json         |
|--------------|---------------|-----------------------------|
| 拉萨         | 统计员        | [{"餐补":1},{"财务结算经...} |
```

## 热门城市

- `HotCityAnalyzer`

- result
```markdown
+------------+-------------+
|city_name|company_count|
+------------+-------------+
|        中山|          180|
|        保定|          168|
|        成都|          136|
|        深圳|          120|
|      张家口|          102|
|        威海|           94|
|        福州|           88|
|        太原|           85|
|        泰州|           73|
|        上饶|           69|
+------------+-------------+
```

## 城市热门职业分析

- `JobHeatCalculator`

- input
```markdown
inputdata = '拉萨'
···

- result

```markdown
+------------+-------------+-------------+
|city_name|position_name|company_count|
+------------+-------------+-------------+
|        拉萨|       统计员|           10|
|        拉萨|服装/纺织设计|            2|
+------------+-------------+-------------+
```