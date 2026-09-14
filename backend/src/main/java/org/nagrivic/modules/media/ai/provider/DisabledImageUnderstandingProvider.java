package org.nagrivic.modules.media.ai.provider;

public class DisabledImageUnderstandingProvider implements ImageUnderstandingProvider {

    private final String calculationVersion;

    public DisabledImageUnderstandingProvider(String calculationVersion) {
        this.calculationVersion = calculationVersion != null ? calculationVersion : "image-understanding-v1";
    }

    @Override
    public ImageUnderstandingResult analyze(ImageUnderstandingRequest request) {
        return ImageUnderstandingResult.disabled(calculationVersion);
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
