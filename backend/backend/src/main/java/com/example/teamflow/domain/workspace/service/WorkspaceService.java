package com.example.teamflow.domain.workspace.service;

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
}
