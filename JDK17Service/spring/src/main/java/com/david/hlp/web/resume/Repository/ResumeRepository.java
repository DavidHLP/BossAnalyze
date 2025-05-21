package com.david.hlp.web.resume.Repository;

import com.david.hlp.web.resume.entity.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends MongoRepository<Resume, String> {
    // 根据用户ID查询简历列表
    List<Resume> findByUserId(String userId);

    // 根据用户ID查询简历，并按创建时间降序排序
    List<Resume> findByUserIdOrderByCreatedAtDesc(String userId);

    // 根据用户ID查询简历，并按更新时间降序排序
    List<Resume> findByUserIdOrderByUpdatedAtDesc(String userId);

    // 分页查询
    Page<Resume> findByUserId(String userId, Pageable pageable);

    // 用户名模糊查询
    Page<Resume> findByUserNameContaining(String userName, Pageable pageable);

    List<Resume> findByUserNameContaining(String userName);

    // 或使用正则表达式查询
    Page<Resume> findByUserNameRegex(String regexPattern, Pageable pageable);
}
