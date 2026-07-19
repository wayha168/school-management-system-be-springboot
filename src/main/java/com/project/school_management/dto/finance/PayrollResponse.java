package com.project.school_management.dto.finance;

import java.math.BigDecimal;
import java.util.UUID;

import com.project.school_management.entities.PayrollRecord;
import com.project.school_management.enums.PayrollStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayrollResponse {

    private UUID uuid;
    private UUID userUuid;
    private String userName;
    private String period;
    private BigDecimal amount;
    private PayrollStatus status;
    private String note;

    public static PayrollResponse from(PayrollRecord record) {
        return PayrollResponse.builder()
                .uuid(record.getUuid())
                .userUuid(record.getUser() != null ? record.getUser().getUuid() : null)
                .userName(record.getUser() != null ? record.getUser().getName() : null)
                .period(record.getPeriod())
                .amount(record.getAmount())
                .status(record.getStatus())
                .note(record.getNote())
                .build();
    }
}
