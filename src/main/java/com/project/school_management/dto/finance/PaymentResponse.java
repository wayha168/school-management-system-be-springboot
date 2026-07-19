package com.project.school_management.dto.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.project.school_management.entities.PaymentRecord;
import com.project.school_management.enums.PaymentStatus;
import com.project.school_management.enums.PaymentType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentResponse {

    private UUID uuid;
    private UUID userUuid;
    private String userName;
    private PaymentType paymentType;
    private BigDecimal amount;
    private LocalDate dueDate;
    private PaymentStatus status;
    private String note;

    public static PaymentResponse from(PaymentRecord record) {
        return PaymentResponse.builder()
                .uuid(record.getUuid())
                .userUuid(record.getUser() != null ? record.getUser().getUuid() : null)
                .userName(record.getUser() != null ? record.getUser().getName() : null)
                .paymentType(record.getPaymentType())
                .amount(record.getAmount())
                .dueDate(record.getDueDate())
                .status(record.getStatus())
                .note(record.getNote())
                .build();
    }
}
