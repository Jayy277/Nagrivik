package org.nagrivic.modules.duplicates.ai;

import java.util.Collections;

public class DisabledDuplicateAiProvider implements DuplicateAiProvider {

    @Override
    public DuplicateAiAnalysisResult analyze(DuplicateAiRequest request) {
        return new DuplicateAiAnalysisResult("DISABLED", "none", "none", Collections.emptyList());
    }

    @Override
    public String getProviderName() {
        return "DISABLED";
    }

    @Override
    public String getModelName() {
        return "none";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
