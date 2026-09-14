package org.nagrivic.modules.priority.ai.provider;

import org.nagrivic.modules.priority.ai.dto.PriorityAiRequest;
import org.nagrivic.modules.priority.ai.dto.PriorityAiResult;

public class DisabledPriorityAiProvider implements PriorityAiProvider {

    private final String calculationVersion;

    public DisabledPriorityAiProvider(String calculationVersion) {
        this.calculationVersion = calculationVersion != null ? calculationVersion : "priority-assistance-v1";
    }

    @Override
    public PriorityAiResult analyze(PriorityAiRequest request) {
        return PriorityAiResult.unavailable(calculationVersion);
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
