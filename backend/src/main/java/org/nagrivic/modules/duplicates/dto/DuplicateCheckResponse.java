package org.nagrivic.modules.duplicates.dto;

import java.util.Collections;
import java.util.List;

public record DuplicateCheckResponse(
    boolean hasPotentialDuplicates,
    List<DuplicateCandidateDto> candidates,
    AiStatusDto ai
) {
    public DuplicateCheckResponse(boolean hasPotentialDuplicates, List<DuplicateCandidateDto> candidates) {
        this(hasPotentialDuplicates, candidates, AiStatusDto.disabled());
    }

    public static DuplicateCheckResponse empty() {
        return new DuplicateCheckResponse(false, Collections.emptyList(), AiStatusDto.disabled());
    }

    public static DuplicateCheckResponse empty(AiStatusDto ai) {
        return new DuplicateCheckResponse(false, Collections.emptyList(), ai != null ? ai : AiStatusDto.disabled());
    }

    public static DuplicateCheckResponse of(List<DuplicateCandidateDto> candidates) {
        boolean hasDuplicates = candidates != null && !candidates.isEmpty();
        return new DuplicateCheckResponse(hasDuplicates, hasDuplicates ? candidates : Collections.emptyList(), AiStatusDto.disabled());
    }

    public static DuplicateCheckResponse of(List<DuplicateCandidateDto> candidates, AiStatusDto ai) {
        boolean hasDuplicates = candidates != null && !candidates.isEmpty();
        return new DuplicateCheckResponse(hasDuplicates, hasDuplicates ? candidates : Collections.emptyList(), ai != null ? ai : AiStatusDto.disabled());
    }
}
