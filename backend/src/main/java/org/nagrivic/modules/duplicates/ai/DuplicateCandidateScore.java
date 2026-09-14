package org.nagrivic.modules.duplicates.ai;

import org.nagrivic.modules.duplicates.model.DuplicateConfidence;

import java.util.List;
import java.util.UUID;

public record DuplicateCandidateScore(
    UUID candidateIssueId,
    int score,
    DuplicateConfidence confidence,
    List<String> signals
) {}
