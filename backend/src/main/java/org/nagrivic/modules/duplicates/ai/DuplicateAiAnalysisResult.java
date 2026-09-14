package org.nagrivic.modules.duplicates.ai;

import java.util.List;

public record DuplicateAiAnalysisResult(
    String provider,
    String model,
    String calculationVersion,
    List<DuplicateCandidateScore> candidateScores
) {}
