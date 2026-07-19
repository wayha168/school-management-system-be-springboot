package com.project.school_management.dto.finance;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FinanceMeResponse {

    private UUID userUuid;
    private String userName;
    private List<PayrollResponse> payroll;
    private List<PaymentResponse> payments;
}
