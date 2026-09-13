package org.nagrivic.modules.categories.dto;

import org.nagrivic.modules.categories.entity.CategoryEntity;
import java.util.UUID;

public record CategoryResponse(
    UUID id,
    String name,
    String slug,
    String description,
    int displayOrder
) {
    public static CategoryResponse fromEntity(CategoryEntity entity) {
        return new CategoryResponse(
            entity.getId(),
            entity.getName(),
            entity.getSlug(),
            entity.getDescription(),
            entity.getDisplayOrder()
        );
    }
}
