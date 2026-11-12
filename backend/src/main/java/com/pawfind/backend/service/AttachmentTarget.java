package com.pawfind.backend.service;

public enum AttachmentTarget {
    LOST("/api/pet-lost/"), FOUND("/api/pet-found/"), POST("/api/community/posts/");

    private final String path;

    AttachmentTarget(String path) {
        this.path = path;
    }

    public String imageUrl(Long recordId, Long attachmentId) {
        return path + recordId + "/images/" + attachmentId;
    }
}
