package org.nagrivic.modules.duplicates.ai;

public interface DuplicateAiProvider {
    DuplicateAiAnalysisResult analyze(DuplicateAiRequest request);
    String getProviderName();
    String getModelName();
    boolean isAvailable();
}
