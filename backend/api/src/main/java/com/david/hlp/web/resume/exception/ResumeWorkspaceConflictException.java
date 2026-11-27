package com.david.hlp.web.resume.exception;

import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.resume.entity.Resume;

public class ResumeWorkspaceConflictException extends BusinessException {
    private final State state;

    public ResumeWorkspaceConflictException(Resume resume) {
        super(409, "简历工作区已更新，请重新读取后再试");
        state = new State(resume.getWorkspaceRevision(), resume.getHeadCommit());
    }

    public State getState() {
        return state;
    }

    public record State(long workspaceRevision, String headCommit) {}
}
