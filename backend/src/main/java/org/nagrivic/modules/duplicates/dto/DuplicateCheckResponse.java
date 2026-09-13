package org.nagrivic.modules.duplicates.dto;

import java.util.Collections;
import java.util.List;

public record DuplicateCheckResponse(
    boolean hasPotentialDuplicates,
    List<DuplicateCandidateDto> candidates
) {
    public static DuplicateCheckResponse empty() {
        return new DuplicateCheckResponse(false, Collections.emptyList());
    }

    public static DuplicateCheckResponse of(List<DuplicateCandidateDto> candidates) {
        boolean hasDuplicates = candidates != null && !candidates.isEmpty();
        return new DuplicateCheckResponse(hasDuplicates, hasDuplicates ? candidates : Collections.emptyList());
    }
}
