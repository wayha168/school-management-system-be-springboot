package com.project.school_management.controller.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.school_management.dto.ApiResponse;
import com.project.school_management.dto.finance.FinanceMeResponse;
import com.project.school_management.dto.finance.PaymentResponse;
import com.project.school_management.dto.finance.PayrollResponse;
import com.project.school_management.enums.PaymentStatus;
import com.project.school_management.enums.PaymentType;
import com.project.school_management.enums.PayrollStatus;
import com.project.school_management.service.finance.FinanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/finance")
@Tag(name = "Finance")
@SecurityRequirement(name = "bearerAuth")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/me")
    @Operation(summary = "My payroll and payments")
    public ResponseEntity<ApiResponse<FinanceMeResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok("My finance fetched", financeService.myFinance()));
    }

    @GetMapping("/users/{userUuid}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    @Operation(summary = "User finance check")
    public ResponseEntity<ApiResponse<FinanceMeResponse>> userFinance(@PathVariable UUID userUuid) {
        return ResponseEntity.ok(ApiResponse.ok("User finance fetched", financeService.userFinance(userUuid)));
    }

    @GetMapping("/payroll")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> payroll(
            @RequestParam(required = false) UUID userUuid) {
        return ResponseEntity.ok(ApiResponse.ok("Payroll fetched", financeService.listPayroll(userUuid)));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> payments(
            @RequestParam(required = false) UUID userUuid) {
        return ResponseEntity.ok(ApiResponse.ok("Payments fetched", financeService.listPayments(userUuid)));
    }

    @PostMapping("/payroll")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ResponseEntity<ApiResponse<PayrollResponse>> createPayroll(
            @RequestParam UUID userUuid,
            @RequestParam String period,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) PayrollStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Payroll created",
                        financeService.createPayroll(userUuid, period, amount, status, note)));
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestParam UUID userUuid,
            @RequestParam PaymentType paymentType,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Payment created",
                        financeService.createPayment(userUuid, paymentType, amount, dueDate, status, note)));
    }

    @PutMapping("/payroll/{id}")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ResponseEntity<ApiResponse<PayrollResponse>> updatePayroll(
            @PathVariable UUID id,
            @RequestParam String period,
            @RequestParam BigDecimal amount,
            @RequestParam PayrollStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Payroll updated",
                financeService.updatePayroll(id, period, amount, status, note)));
    }

    @PutMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePayment(
            @PathVariable UUID id,
            @RequestParam PaymentType paymentType,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam PaymentStatus status,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Payment updated",
                financeService.updatePayment(id, paymentType, amount, dueDate, status, note)));
    }

    @DeleteMapping("/payroll/{id}")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ResponseEntity<ApiResponse<Void>> deletePayroll(@PathVariable UUID id) {
        financeService.deletePayroll(id);
        return ResponseEntity.ok(ApiResponse.ok("Payroll deleted", null));
    }

    @DeleteMapping("/payments/{id}")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable UUID id) {
        financeService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.ok("Payment deleted", null));
    }
}
