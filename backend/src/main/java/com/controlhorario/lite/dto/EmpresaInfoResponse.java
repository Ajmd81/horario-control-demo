package com.controlhorario.lite.dto;

import java.time.LocalDateTime;

public record EmpresaInfoResponse(
    String empresaNombre,
    String empresaSlug,
    boolean demo,
    long diasRestantesDemo,
    String plan,
    String subscriptionStatus,
    LocalDateTime currentPeriodEnd
) {}