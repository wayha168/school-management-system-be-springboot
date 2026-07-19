package com.project.school_management.service.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.project.school_management.dto.finance.FinanceMeResponse;
import com.project.school_management.dto.finance.PaymentResponse;
import com.project.school_management.dto.finance.PayrollResponse;
import com.project.school_management.enums.PaymentStatus;
import com.project.school_management.enums.PaymentType;
import com.project.school_management.enums.PayrollStatus;

public interface FinanceService {

    List<PayrollResponse> listPayroll(UUID userUuid);

    List<PaymentResponse> listPayments(UUID userUuid);

    FinanceMeResponse myFinance();

    FinanceMeResponse userFinance(UUID userUuid);

    PayrollResponse createPayroll(UUID userUuid, String period, BigDecimal amount, PayrollStatus status, String note);

    PaymentResponse createPayment(
            UUID userUuid, PaymentType type, BigDecimal amount, LocalDate dueDate, PaymentStatus status, String note);

    PayrollResponse updatePayroll(UUID id, String period, BigDecimal amount, PayrollStatus status, String note);

    PaymentResponse updatePayment(
            UUID id, PaymentType type, BigDecimal amount, LocalDate dueDate, PaymentStatus status, String note);

    void deletePayroll(UUID id);

    void deletePayment(UUID id);
}
