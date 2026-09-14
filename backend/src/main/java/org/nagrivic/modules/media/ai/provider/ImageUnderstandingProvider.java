package org.nagrivic.modules.media.ai.provider;

public interface ImageUnderstandingProvider {
    ImageUnderstandingResult analyze(ImageUnderstandingRequest request);
    String getProviderName();
    String getModelName();
    boolean isAvailable();
}
