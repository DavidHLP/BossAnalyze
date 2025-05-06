/*
 Navicat Premium Dump SQL

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : 127.0.0.1:3306
 Source Schema         : blog_data

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 06/05/2025 19:39:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for nginx_access_log
-- ----------------------------
DROP TABLE IF EXISTS `nginx_access_log`;
CREATE TABLE `nginx_access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ip` varchar(50) NOT NULL COMMENT 'IP地址',
  `access_time` varchar(50) NOT NULL COMMENT '访问时间',
  `method` varchar(10) NOT NULL COMMENT 'HTTP方法',
  `path` varchar(255) NOT NULL COMMENT '访问路径',
  `protocol` varchar(20) DEFAULT NULL COMMENT 'HTTP协议版本',
  `status` varchar(10) NOT NULL COMMENT 'HTTP状态码',
  `bytes` varchar(20) DEFAULT NULL COMMENT '响应大小(字节)',
  `referrer` text COMMENT '引用来源',
  `user_agent` text COMMENT '用户代理',
  `user_id` varchar(50) DEFAULT NULL COMMENT '用户ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip` (`ip`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_path` (`path`),
  KEY `idx_status` (`status`),
  KEY `idx_method` (`method`)
) ENGINE=InnoDB AUTO_INCREMENT=701 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nginx访问日志表';

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(255) DEFAULT NULL,
  `permission` varchar(255) NOT NULL COMMENT '权限标识',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission` (`permission`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(255) NOT NULL COMMENT '角色名称',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `permission_id` (`permission_id`),
  CONSTRAINT `role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `role_permission_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色-权限关联表';

-- ----------------------------
-- Table structure for router
-- ----------------------------
DROP TABLE IF EXISTS `router`;
CREATE TABLE `router` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `pid` bigint DEFAULT NULL COMMENT '父路由ID',
  `menu_order` int NOT NULL COMMENT '菜单顺序',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `permission` varchar(255) DEFAULT NULL COMMENT '权限标识',
  `path` varchar(255) NOT NULL COMMENT '路由路径',
  `name` varchar(255) NOT NULL COMMENT '路由名称',
  `type` enum('C','M','F') NOT NULL COMMENT '类型：C-目录，M-菜单，F-功能',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `redirect` varchar(255) DEFAULT NULL COMMENT '重定向路径',
  `always_show` tinyint(1) NOT NULL DEFAULT '0' COMMENT '总是显示',
  `meta_title` varchar(255) NOT NULL COMMENT '元标题',
  `meta_icon` varchar(255) DEFAULT NULL COMMENT '元图标',
  `meta_hidden` tinyint(1) DEFAULT '0' COMMENT '元隐藏',
  `meta_roles` json DEFAULT NULL COMMENT '元角色，存储角色名称数组',
  `meta_keep_alive` tinyint(1) DEFAULT '0' COMMENT '元保持活跃',
  `hidden` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏',
  `icon` varchar(255) DEFAULT NULL,
  `role_id` bigint DEFAULT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  KEY `pid` (`pid`),
  KEY `permission` (`permission`),
  CONSTRAINT `router_ibfk_1` FOREIGN KEY (`pid`) REFERENCES `router` (`id`) ON DELETE CASCADE,
  CONSTRAINT `router_ibfk_2` FOREIGN KEY (`permission`) REFERENCES `permission` (`permission`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路由表';

-- ----------------------------
-- Table structure for token
-- ----------------------------
DROP TABLE IF EXISTS `token`;
CREATE TABLE `token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `user_id` bigint NOT NULL COMMENT '用户ID，外键关联user表',
  `token` varchar(255) NOT NULL COMMENT 'Token值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `token_type` varchar(20) NOT NULL COMMENT '令牌类型: BEARER',
  `expired` tinyint(1) DEFAULT '0',
  `revoked` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `token_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Token表';

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `introduction` varchar(255) DEFAULT '用户未填写',
  `email` varchar(255) NOT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '用户状态：0-禁用，1-启用',
  `address` varchar(255) DEFAULT '用户未填写',
  `last_login_ip` varchar(45) DEFAULT NULL,
  `last_login` datetime DEFAULT NULL,
  `role_id` bigint NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `password` varchar(255) NOT NULL COMMENT '加密后的密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID，外键关联user表',
  `role_id` bigint NOT NULL COMMENT '角色ID，外键关联role表',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `role_id` (`role_id`),
  CONSTRAINT `user_role_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_role_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户-角色关联表';

SET FOREIGN_KEY_CHECKS = 1;
