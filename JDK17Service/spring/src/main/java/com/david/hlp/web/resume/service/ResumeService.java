package com.david.hlp.web.resume.service;

import com.david.hlp.web.resume.Repository.ResumeRepository;
import com.david.hlp.web.resume.entity.Resume;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumeService {
    private final ResumeRepository resumeRepository;

    public Page<Resume> findAll(Pageable pageable) {
        return resumeRepository.findAll(pageable);
    }

    public Optional<Resume> findById(String id) {
        return resumeRepository.findById(id);
    }

    public Resume save(Resume resume) {
        return resumeRepository.save(resume);
    }

    public void deleteById(String id) {
        resumeRepository.deleteById(id);
    }

    /**
     * 根据用户ID查询所有简历
     */
    public List<Resume> findByUserId(String userId) {
        return resumeRepository.findByUserId(userId);
    }

    /**
     * 根据用户ID查询最新创建的简历
     */
    public List<Resume> findLatestByUserId(String userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户最新创建的一份简历
     */
    public Optional<Resume> findLatestOneByUserId(String userId) {
        List<Resume> resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return resumes.isEmpty() ? Optional.empty() : Optional.of(resumes.get(0));
    }

    /**
     * 根据用户ID查询最近更新的简历
     */
    public List<Resume> findRecentlyUpdatedByUserId(String userId) {
        return resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 获取用户最近更新的一份简历
     */
    public Optional<Resume> findRecentlyUpdatedOneByUserId(String userId) {
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        return resumes.isEmpty() ? Optional.empty() : Optional.of(resumes.get(0));
    }

    /**
     * 分页查询用户简历
     */
    public Page<Resume> findByUserId(String userId, Pageable pageable) {
        return resumeRepository.findByUserId(userId, pageable);
    }

    /**
     * 分页查询用户简历（按用户名）
     */
    public Page<Resume> findByUserName(String userName, Pageable pageable) {
        return resumeRepository.findByUserNameContaining(userName, pageable);
    }
}
