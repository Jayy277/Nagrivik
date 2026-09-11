package org.nagrivic.modules.categories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nagrivic.modules.categories.entity.CategoryEntity;
import org.nagrivic.modules.categories.repository.CategoryRepository;
import org.nagrivic.modules.categories.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryDomainTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
    }

    @Test
    void shouldPersistAndRetrieveCategory() {
        CategoryEntity category = new CategoryEntity(
                "Roads / Potholes",
                "roads-potholes",
                "Pothole and road surface issues",
                1
        );
        CategoryEntity saved = categoryRepository.save(category);

        assertNotNull(saved.getId());
        assertEquals("Roads / Potholes", saved.getName());
        assertEquals("roads-potholes", saved.getSlug());
        assertTrue(saved.isActive());
        assertEquals(1, saved.getDisplayOrder());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldEnforceSlugUniqueness() {
        CategoryEntity cat1 = new CategoryEntity("Water", "water", "Water issues", 1);
        categoryRepository.saveAndFlush(cat1);

        CategoryEntity cat2 = new CategoryEntity("Water Supply", "water", "Duplicate slug", 2);
        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryRepository.saveAndFlush(cat2);
        });
    }

    @Test
    void shouldQueryActiveCategoriesOrderedByDisplayOrder() {
        CategoryEntity cat3 = new CategoryEntity("Streetlights", "streetlights", "Lighting", 3);
        CategoryEntity cat1 = new CategoryEntity("Roads / Potholes", "roads-potholes", "Roads", 1);
        CategoryEntity cat2 = new CategoryEntity("Garbage", "garbage", "Waste", 2);

        CategoryEntity inactiveCat = new CategoryEntity("Old Category", "old-cat", "Retired", 4);
        inactiveCat.setActive(false);

        categoryRepository.saveAll(List.of(cat3, cat1, cat2, inactiveCat));

        List<CategoryEntity> activeList = categoryService.getActiveCategories();
        assertEquals(3, activeList.size());
        assertEquals("roads-potholes", activeList.get(0).getSlug());
        assertEquals("garbage", activeList.get(1).getSlug());
        assertEquals("streetlights", activeList.get(2).getSlug());
    }

    @Test
    void shouldFindCategoryBySlug() {
        CategoryEntity cat = new CategoryEntity("Drainage", "drainage", "Drainage issues", 5);
        categoryRepository.save(cat);

        Optional<CategoryEntity> found = categoryService.findBySlug("drainage");
        assertTrue(found.isPresent());
        assertEquals("Drainage", found.get().getName());
    }
}
