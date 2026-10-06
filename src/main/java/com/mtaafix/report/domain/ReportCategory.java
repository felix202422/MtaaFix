package com.mtaafix.report.domain;

import com.mtaafix.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_categories", indexes = {
        @Index(name = "idx_report_categories_parent", columnList = "parent_id")
})
public class ReportCategory extends BaseEntity {

    public enum Type {
        PRIMARY,
        SECONDARY
    }

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "parent_id")
    private String parentId;

    public ReportCategory() {
    }

    public ReportCategory(String name, String slug, Type type) {
        this.name = name;
        this.slug = slug;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
