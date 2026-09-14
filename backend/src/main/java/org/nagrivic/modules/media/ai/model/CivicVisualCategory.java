package org.nagrivic.modules.media.ai.model;

public enum CivicVisualCategory {
    ROADS_POTHOLES("roads-potholes", "Roads / Potholes"),
    GARBAGE("garbage", "Garbage"),
    STREETLIGHTS("streetlights", "Streetlights"),
    WATER("water", "Water"),
    DRAINAGE("drainage", "Drainage");

    private final String slug;
    private final String displayName;

    CivicVisualCategory(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CivicVisualCategory fromSlug(String slug) {
        if (slug == null) return null;
        for (CivicVisualCategory cat : values()) {
            if (cat.slug.equalsIgnoreCase(slug.trim())) {
                return cat;
            }
        }
        return null;
    }
}
