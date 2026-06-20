package com.example.teamflow.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class WorkspaceContext {

    private WorkspaceContext() {}

    public static Long get() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        return (Long) details;
    }
}
