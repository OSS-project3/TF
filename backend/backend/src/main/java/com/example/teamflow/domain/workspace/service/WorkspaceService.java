package com.example.teamflow.domain.workspace.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.workspace.entity.Workspace;
import com.example.teamflow.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public Workspace create(String name, Long memberId) {
        Workspace workspace = Workspace.create(name, memberId);
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace getById(Long workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }
}
