package org.nagrivic.modules.departments.service;

import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.departments.entity.CategoryDepartmentMappingEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;
import org.nagrivic.modules.departments.entity.WardDepartmentMappingEntity;
import org.nagrivic.modules.departments.repository.CategoryDepartmentMappingRepository;
import org.nagrivic.modules.departments.repository.WardDepartmentMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DepartmentResolverServiceImpl implements DepartmentResolverService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentResolverServiceImpl.class);

    private final CategoryDepartmentMappingRepository categoryDepartmentMappingRepository;
    private final WardDepartmentMappingRepository wardDepartmentMappingRepository;

    public DepartmentResolverServiceImpl(
            CategoryDepartmentMappingRepository categoryDepartmentMappingRepository,
            WardDepartmentMappingRepository wardDepartmentMappingRepository
    ) {
        this.categoryDepartmentMappingRepository = categoryDepartmentMappingRepository;
        this.wardDepartmentMappingRepository = wardDepartmentMappingRepository;
    }

    @Override
    public Optional<DepartmentEntity> resolveDepartment(UUID categoryId, CivicBodyEntity civicBody, WardEntity ward) {
        if (categoryId == null || civicBody == null) {
            return Optional.empty();
        }

        // 1. Fetch category-to-department mappings
        List<CategoryDepartmentMappingEntity> catMappings = categoryDepartmentMappingRepository.findByCategoryId(categoryId);
        if (catMappings.isEmpty()) {
            return Optional.empty();
        }

        // 2. Filter to active departments of the matching civic body and active mapping
        List<DepartmentEntity> candidateDepartments = catMappings.stream()
                .filter(CategoryDepartmentMappingEntity::isActive)
                .map(CategoryDepartmentMappingEntity::getDepartment)
                .filter(dept -> dept.isActive() && dept.getCivicBody().getId().equals(civicBody.getId()))
                .distinct()
                .toList();

        if (candidateDepartments.isEmpty()) {
            return Optional.empty();
        }

        // 3. Specific authoritative mapping: evaluate ward-department service mappings if ward is provided
        if (ward != null) {
            List<WardDepartmentMappingEntity> wardMappings = wardDepartmentMappingRepository.findByWardIdAndIsActiveTrue(ward.getId());
            if (!wardMappings.isEmpty()) {
                Set<UUID> allowedDeptIds = wardMappings.stream()
                        .map(wm -> wm.getDepartment().getId())
                        .collect(Collectors.toSet());

                // Find candidate departments specifically active/servicing this ward
                List<DepartmentEntity> wardSpecificDepts = candidateDepartments.stream()
                        .filter(dept -> allowedDeptIds.contains(dept.getId()))
                        .toList();

                if (wardSpecificDepts.size() == 1) {
                    log.debug("Resolved department via ward-specific mapping: {} (wardId={})",
                            wardSpecificDepts.get(0).getName(), ward.getId());
                    return Optional.of(wardSpecificDepts.get(0));
                } else if (wardSpecificDepts.size() > 1) {
                    // Ambiguous ward-specific mappings for this category -> do not guess
                    log.warn("Conflicting ward-specific department mappings for categoryId={}, wardId={}: count={}",
                            categoryId, ward.getId(), wardSpecificDepts.size());
                    return Optional.empty();
                }
                // If ward has mappings but none match the category candidates, we fall back to civic-body level
            }
        }

        // 4. Broader category-level mapping (at civic body level):
        // If exactly one candidate department exists, resolve deterministically.
        if (candidateDepartments.size() == 1) {
            return Optional.of(candidateDepartments.get(0));
        }

        // Multiple conflicting candidate departments at civic body level without ward disambiguation:
        // Do not guess or silently pick the first one.
        log.warn("Conflicting civic-body department mappings for categoryId={}, civicBodyId={}: candidateCount={}",
                categoryId, civicBody.getId(), candidateDepartments.size());
        return Optional.empty();
    }
}
