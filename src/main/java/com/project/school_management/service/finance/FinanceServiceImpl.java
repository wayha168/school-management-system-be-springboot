package com.project.school_management.service.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.school_management.dto.finance.FinanceMeResponse;
import com.project.school_management.dto.finance.PaymentResponse;
import com.project.school_management.dto.finance.PayrollResponse;
import com.project.school_management.entities.PaymentRecord;
import com.project.school_management.entities.PayrollRecord;
import com.project.school_management.entities.User;
import com.project.school_management.enums.PaymentStatus;
import com.project.school_management.enums.PaymentType;
import com.project.school_management.enums.PayrollStatus;
import com.project.school_management.exception.ExceptionNotFound;
import com.project.school_management.repository.PaymentRecordRepository;
import com.project.school_management.repository.PayrollRecordRepository;
import com.project.school_management.repository.UserRepository;
import com.project.school_management.security.SchoolScopeService;

@Service
@Transactional
public class FinanceServiceImpl implements FinanceService {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private final PayrollRecordRepository payrollRepository;
    private final PaymentRecordRepository paymentRepository;
    private final UserRepository userRepository;
    private final SchoolScopeService schoolScopeService;

    public FinanceServiceImpl(
            PayrollRecordRepository payrollRepository,
            PaymentRecordRepository paymentRepository,
            UserRepository userRepository,
            SchoolScopeService schoolScopeService) {
        this.payrollRepository = payrollRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.schoolScopeService = schoolScopeService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollResponse> listPayroll(UUID userUuid) {
        requireFinanceRead();
        boolean filterUser = userUuid != null;
        return payrollRepository.findDetailed(filterUser, filterUser ? userUuid : NIL_UUID).stream()
                .filter(this::payrollInScope)
                .map(PayrollResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(UUID userUuid) {
        requireFinanceRead();
        boolean filterUser = userUuid != null;
        return paymentRepository.findDetailed(filterUser, filterUser ? userUuid : NIL_UUID).stream()
                .filter(this::paymentInScope)
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceMeResponse myFinance() {
        User current = schoolScopeService.requireCurrentUser();
        return buildForUser(current);
    }

    @Override
    @Transactional(readOnly = true)
    public FinanceMeResponse userFinance(UUID userUuid) {
        requireFinanceRead();
        User user = userRepository.findDetailedById(userUuid)
                .orElseThrow(() -> new ExceptionNotFound("User not found: " + userUuid));
        if (user.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(user.getSchool().getUuid());
        }
        return buildForUser(user);
    }

    @Override
    public PayrollResponse createPayroll(
            UUID userUuid, String period, BigDecimal amount, PayrollStatus status, String note) {
        requireFinanceWrite();
        User actor = schoolScopeService.requireCurrentUser();
        User user = findUser(userUuid);
        PayrollRecord record = new PayrollRecord();
        record.setUser(user);
        record.setPeriod(period.trim());
        record.setAmount(amount);
        record.setStatus(status != null ? status : PayrollStatus.DRAFT);
        record.setNote(blankToNull(note));
        record.setCreatedBy(actor);
        return PayrollResponse.from(payrollRepository.save(record));
    }

    @Override
    public PaymentResponse createPayment(
            UUID userUuid,
            PaymentType type,
            BigDecimal amount,
            LocalDate dueDate,
            PaymentStatus status,
            String note) {
        requireFinanceWrite();
        User actor = schoolScopeService.requireCurrentUser();
        User user = findUser(userUuid);
        PaymentRecord record = new PaymentRecord();
        record.setUser(user);
        record.setPaymentType(type != null ? type : PaymentType.TUITION);
        record.setAmount(amount);
        record.setDueDate(dueDate);
        record.setStatus(status != null ? status : PaymentStatus.PENDING);
        record.setNote(blankToNull(note));
        record.setCreatedBy(actor);
        return PaymentResponse.from(paymentRepository.save(record));
    }

    @Override
    public PayrollResponse updatePayroll(
            UUID id, String period, BigDecimal amount, PayrollStatus status, String note) {
        requireFinanceWrite();
        PayrollRecord record = payrollRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Payroll not found: " + id));
        assertPayrollScope(record);
        record.setPeriod(period.trim());
        record.setAmount(amount);
        record.setStatus(status);
        record.setNote(blankToNull(note));
        return PayrollResponse.from(payrollRepository.save(record));
    }

    @Override
    public PaymentResponse updatePayment(
            UUID id, PaymentType type, BigDecimal amount, LocalDate dueDate, PaymentStatus status, String note) {
        requireFinanceWrite();
        PaymentRecord record = paymentRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Payment not found: " + id));
        assertPaymentScope(record);
        record.setPaymentType(type);
        record.setAmount(amount);
        record.setDueDate(dueDate);
        record.setStatus(status);
        record.setNote(blankToNull(note));
        return PaymentResponse.from(paymentRepository.save(record));
    }

    @Override
    public void deletePayroll(UUID id) {
        requireFinanceWrite();
        PayrollRecord record = payrollRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Payroll not found: " + id));
        assertPayrollScope(record);
        payrollRepository.delete(record);
    }

    @Override
    public void deletePayment(UUID id) {
        requireFinanceWrite();
        PaymentRecord record = paymentRepository.findDetailedById(id)
                .orElseThrow(() -> new ExceptionNotFound("Payment not found: " + id));
        assertPaymentScope(record);
        paymentRepository.delete(record);
    }

    private FinanceMeResponse buildForUser(User user) {
        return FinanceMeResponse.builder()
                .userUuid(user.getUuid())
                .userName(user.getName())
                .payroll(payrollRepository.findDetailed(true, user.getUuid()).stream().map(PayrollResponse::from).toList())
                .payments(paymentRepository.findDetailed(true, user.getUuid()).stream().map(PaymentResponse::from).toList())
                .build();
    }

    private User findUser(UUID userUuid) {
        User user = userRepository.findDetailedById(userUuid)
                .orElseThrow(() -> new ExceptionNotFound("User not found: " + userUuid));
        if (user.getSchool() != null) {
            schoolScopeService.assertSchoolAccess(user.getSchool().getUuid());
        }
        return user;
    }

    private boolean payrollInScope(PayrollRecord record) {
        if (record.getUser() == null || record.getUser().getSchool() == null) {
            return true;
        }
        return schoolScopeService.scopedSchoolUuid().isEmpty()
                || schoolScopeService.scopedSchoolUuid().get().equals(record.getUser().getSchool().getUuid());
    }

    private boolean paymentInScope(PaymentRecord record) {
        if (record.getUser() == null || record.getUser().getSchool() == null) {
            return true;
        }
        return schoolScopeService.scopedSchoolUuid().isEmpty()
                || schoolScopeService.scopedSchoolUuid().get().equals(record.getUser().getSchool().getUuid());
    }

    private void assertPayrollScope(PayrollRecord record) {
        if (!payrollInScope(record)) {
            throw new AccessDeniedException("Out of school scope");
        }
    }

    private void assertPaymentScope(PaymentRecord record) {
        if (!paymentInScope(record)) {
            throw new AccessDeniedException("Out of school scope");
        }
    }

    private void requireFinanceRead() {
        if (!hasAuthority("FINANCE_READ")) {
            throw new AccessDeniedException("FINANCE_READ required");
        }
    }

    private void requireFinanceWrite() {
        if (!hasAuthority("FINANCE_WRITE")) {
            throw new AccessDeniedException("FINANCE_WRITE required");
        }
    }

    private static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
