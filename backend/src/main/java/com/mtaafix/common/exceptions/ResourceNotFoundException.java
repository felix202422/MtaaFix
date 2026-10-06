package com.mtaafix.common.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String id;

    public ResourceNotFoundException(String resourceType, String id) {
        super(String.format("%s not found: %s '%s'", resourceType, id != null ? id : "unknown", id));
        this.resourceType = resourceType;
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getId() {
        return id;
    }
}
