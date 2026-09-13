package org.nagrivic.modules.departments.service;

import org.nagrivic.modules.civicgeography.entity.CivicBodyEntity;
import org.nagrivic.modules.civicgeography.entity.WardEntity;
import org.nagrivic.modules.departments.entity.DepartmentEntity;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentResolverService {
    Optional<DepartmentEntity> resolveDepartment(UUID categoryId, CivicBodyEntity civicBody, WardEntity ward);
}
