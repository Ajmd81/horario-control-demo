package com.controlhorario.lite.dto;

import java.time.LocalDateTime;

public record FichajeSalidaRequest(
    String clientId,
    LocalDateTime clientTimestamp
) {}