/*
 Navicat Premium Dump SQL

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : 127.0.0.1:3306
 Source Schema         : boss_data

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 06/06/2025 19:47:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for city_data
-- ----------------------------
DROP TABLE IF EXISTS `city_data`;
CREATE TABLE `city_data` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID，主键',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '城市名称',
  `code` int NOT NULL COMMENT '城市代码（唯一）',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '城市URL路径',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `code_2` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2612 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for degree
-- ----------------------------
DROP TABLE IF EXISTS `degree`;
CREATE TABLE `degree` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增ID，主键',
  `unique_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '唯一标识',
  `city` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '城市名称',
  `degree` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学位',
  `salary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '薪资',
  `experience` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '经验',
  `update_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_id` (`unique_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2558 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for html_data
-- ----------------------------
DROP TABLE IF EXISTS `html_data`;
CREATE TABLE `html_data` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `url` varchar(255) NOT NULL COMMENT '网页的唯一地址',
  `html_content` longtext COMMENT 'HTML 内容，允许为空',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `base_city` varchar(100) NOT NULL COMMENT '城市名称',
  `base_position` varchar(100) NOT NULL COMMENT '职位名称',
  `base_city_code` varchar(50) NOT NULL COMMENT '城市代码',
  `base_position_code` varchar(50) NOT NULL COMMENT '职位代码',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态标识: 0-待处理, 1-已下载网页, 2-人工已经处理数据, 3-AI已经处理数据 , 4-下载失败',
  PRIMARY KEY (`id`),
  UNIQUE KEY `url` (`url`),
  KEY `idx_url` (`url`)
) ENGINE=InnoDB AUTO_INCREMENT=22405 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='HTML 数据表，用于存储网页原始数据';

-- ----------------------------
-- Table structure for industry_data
-- ----------------------------
DROP TABLE IF EXISTS `industry_data`;
CREATE TABLE `industry_data` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一标识ID',
  `parent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '父级职位的code值，构建层级关系',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位唯一编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位类型',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_code_type` (`code`,`type`),
  KEY `idx_parent_id` (`parent_id`) COMMENT 'parent_id索引，优化层级查询',
  CONSTRAINT `fk_parent` FOREIGN KEY (`parent_id`) REFERENCES `industry_data` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3863 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位数据表，支持层级结构';

-- ----------------------------
-- Table structure for job_list
-- ----------------------------
DROP TABLE IF EXISTS `job_list`;
CREATE TABLE `job_list` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `html_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HTML页面URL',
  `json_data` json DEFAULT NULL COMMENT '存储JSON格式的数据',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_html_url` (`html_url`(255)) COMMENT 'URL唯一索引'
) ENGINE=InnoDB AUTO_INCREMENT=12109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='boss直聘2025年新页面工作列表表';

-- ----------------------------
-- Table structure for position_data
-- ----------------------------
DROP TABLE IF EXISTS `position_data`;
CREATE TABLE `position_data` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '唯一标识ID',
  `parent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '父级职位的code值，构建层级关系',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位唯一编码',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位类型',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2451 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位数据表，支持层级结构';

-- ----------------------------
-- Table structure for t_job_detail
-- ----------------------------
DROP TABLE IF EXISTS `t_job_detail`;
CREATE TABLE `t_job_detail` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `position_id` varchar(64) NOT NULL COMMENT '职位唯一标识',
  `position_name` varchar(128) NOT NULL COMMENT '职位名称',
  `city_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市唯一标识',
  `city_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市名称',
  `detail_data` json NOT NULL COMMENT 'JobDetailData完整数据(JSON格式)',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除(0-未删除,1-已删除)',
  `html_url` varchar(255) NOT NULL COMMENT 'html_data 表中的url内容',
  `employee_benefits` text COMMENT '员工福利',
  `job_requirements` text COMMENT '职位需求',
  PRIMARY KEY (`id`),
  UNIQUE KEY `html_url` (`html_url`),
  KEY `idx_company_id` (`city_id`),
  KEY `idx_create_time` (`gmt_create`),
  KEY `idx_position_name` (`position_name`),
  KEY `idx_html_url_tb` (`html_url`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15173 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位详情表';

SET FOREIGN_KEY_CHECKS = 1;
