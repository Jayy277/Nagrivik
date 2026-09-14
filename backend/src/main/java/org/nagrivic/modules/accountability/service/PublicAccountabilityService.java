package org.nagrivic.modules.accountability.service;

import org.nagrivic.modules.accountability.dto.AccountabilityFilter;
import org.nagrivic.modules.accountability.dto.PublicAccountabilityResponse;

public interface PublicAccountabilityService {
    PublicAccountabilityResponse getPublicAccountability(AccountabilityFilter filter);
}
