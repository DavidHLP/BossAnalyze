package com.david.hlp.web.resume.service;

import com.david.hlp.web.common.entity.PageInfo;
import com.david.hlp.web.common.enums.RedisKeyEnum;
import com.david.hlp.commons.utils.RedisCacheHelper;
import com.mongodb.client.result.DeleteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简历服务类，处理简历的CRUD操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {
    private static final String COLLECTION_NAME = "resumes";
    private static final String ID_FIELD = "_id";
    private static final String UPDATED_AT_FIELD = "updatedAt";
    private static final String CREATED_AT_FIELD = "createdAt";
    private static final String NAME_FIELD = "name";
    private static final String EXPERIENCE_FIELD = "experience";
    private static final String JOB_TARGET_FIELD = "jobTarget";

    private final MongoTemplate mongoTemplate;
    private final RedisCacheHelper redisCacheHelper;

    /**
     * 分页查询简历列表
     * 
     * @param pageable   分页参数
     * @param name       姓名模糊查询
     * @param experience 工作经验
     * @param jobTarget  求职目标
     * @return 分页简历数据
     */
    public Page<Map<String, Object>> findAllAsJson(Pageable pageable, String name, String experience,
            String jobTarget) {
        Query query = buildSearchQuery(name, experience, jobTarget).with(pageable);

        List<Document> documents = mongoTemplate.find(query, Document.class, COLLECTION_NAME);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), COLLECTION_NAME);

        return new PageImpl<>(
                documents.stream()
                        .map(this::convertIdToFrontend)
                        .collect(Collectors.toList()),
                pageable,
                total);
    }

    /**
     * 搜索简历（带缓存）
     */
    public PageInfo<Map<String, Object>> searchResumes(Long userId, Pageable pageable, String name,
            String experience, String jobTarget) {
        String key = buildCacheKey(userId, pageable, name, experience, jobTarget);

        return (PageInfo<Map<String, Object>>) redisCacheHelper.getOrLoadObject(
                key,
                PageInfo.class,
                RedisKeyEnum.RESUME_LIST_KEY.getTimeout(),
                () -> PageInfo.of(findAllAsJson(pageable, name, experience, jobTarget)));
    }

    /**
     * 根据ID查询简历
     */
    public Optional<Map<String, Object>> findByIdAsJson(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Document.class, COLLECTION_NAME))
                .map(this::convertIdToFrontend);
    }

    /**
     * 保存简历
     */
    public Map<String, Object> saveAsJson(Long userId, Map<String, Object> resumeData) {
        Document document = new Document(convertIdToBackend(new HashMap<>(resumeData)));
        document.put(CREATED_AT_FIELD, new Date());
        document.put(UPDATED_AT_FIELD, new Date());

        Document saved = mongoTemplate.save(document, COLLECTION_NAME);
        clearUserCache(userId);

        return convertIdToFrontend(saved);
    }

    /**
     * 更新简历
     */
    public Optional<Map<String, Object>> updateAsJson(String id, Long userId, Map<String, Object> resumeData) {
        Update update = new Update();
        Map<String, Object> dataToUpdate = convertIdToBackend(new HashMap<>(resumeData));

        // 设置更新字段
        dataToUpdate.entrySet().stream()
                .filter(entry -> !ID_FIELD.equals(entry.getKey()))
                .forEach(entry -> update.set(entry.getKey(), entry.getValue()));

        update.set(UPDATED_AT_FIELD, new Date());

        Document updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where(ID_FIELD).is(new ObjectId(id))),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Document.class,
                COLLECTION_NAME);

        if (updated != null) {
            clearUserCache(userId);
            return Optional.of(convertIdToFrontend(updated));
        }
        return Optional.empty();
    }

    /**
     * 删除简历
     */
    public boolean deleteById(String id, Long userId) {
        DeleteResult result = mongoTemplate.remove(
                Query.query(Criteria.where(ID_FIELD).is(new ObjectId(id))),
                COLLECTION_NAME);

        if (result.getDeletedCount() > 0) {
            clearUserCache(userId);
            return true;
        }
        return false;
    }

    /**
     * 构建查询条件
     */
    private Query buildSearchQuery(String name, String experience, String jobTarget) {
        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(name)) {
            criteria.add(Criteria.where(NAME_FIELD).regex(name, "i"));
        }
        if (StringUtils.hasText(experience)) {
            criteria.add(Criteria.where(EXPERIENCE_FIELD).is(experience));
        }
        if (StringUtils.hasText(jobTarget)) {
            criteria.add(Criteria.where(JOB_TARGET_FIELD).regex(jobTarget, "i"));
        }

        return criteria.isEmpty() ? new Query()
                : new Query(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(Long userId, Pageable pageable, String... params) {
        String prefix = String.format("%s%d:%d:%d",
                RedisKeyEnum.RESUME_LIST_KEY.getKey(),
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize());

        return Arrays.stream(params)
                .filter(Objects::nonNull)
                .reduce(prefix, (key, param) -> key + ":" + param);
    }

    /**
     * 清除用户相关缓存
     */
    private void clearUserCache(Long userId) {
        String pattern = RedisKeyEnum.RESUME_LIST_KEY.getKey() + userId + ":*";
        redisCacheHelper.deleteByPattern(pattern);
    }

    /**
     * 转换ID格式为前端所需格式
     */
    private Map<String, Object> convertIdToFrontend(Map<String, Object> data) {
        if (data == null)
            return null;

        Map<String, Object> result = new HashMap<>(data);
        if (result.containsKey(ID_FIELD)) {
            result.put("id", result.get(ID_FIELD).toString());
            result.remove(ID_FIELD);
        }
        return result;
    }

    /**
     * 转换ID格式为后端存储格式
     */
    private Map<String, Object> convertIdToBackend(Map<String, Object> data) {
        if (data == null)
            return null;

        Map<String, Object> result = new HashMap<>(data);
        if (result.containsKey("id")) {
            result.put(ID_FIELD, new ObjectId(result.get("id").toString()));
            result.remove("id");
        }
        return result;
    }
}
