package com.github.spector517.xtbot.core.application.logger;

import org.slf4j.MDC;

import com.github.spector517.xtbot.core.application.data.inbound.ClientData;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MDCLogManager {

    public final String EXTERNAL_ID_KEY = "externalId";
    public final String CURRENT_STAGE_KEY = "stage";

    public void put(ClientData clientData) {
        if (clientData == null) {
            return;
        }
        MDC.put(EXTERNAL_ID_KEY, String.valueOf(clientData.externalId()));
        if (!clientData.currentStage().isBlank()) {
            MDC.put(CURRENT_STAGE_KEY, clientData.currentStage());
        }
    }

    public void clear() {
        MDC.clear();
    }

    public void putClientId(long clientId) {
        MDC.put(EXTERNAL_ID_KEY, String.valueOf(clientId));
    }
}
