package org.nagrivic.modules.priority.ai.provider;

import org.nagrivic.modules.priority.ai.dto.PriorityAiRequest;
import org.nagrivic.modules.priority.ai.dto.PriorityAiResult;

public interface PriorityAiProvider {

    /**
     * Generate an AI-assisted priority recommendation for a civic issue.
     * All component outputs must be validated and clamped server-side.
     */
    PriorityAiResult analyze(PriorityAiRequest request);

    String getProviderName();

    String getModelName();

    boolean isAvailable();
}
