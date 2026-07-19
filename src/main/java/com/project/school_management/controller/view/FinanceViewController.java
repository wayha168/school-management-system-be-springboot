package com.project.school_management.controller.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.school_management.dto.finance.FinanceMeResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.enums.PaymentStatus;
import com.project.school_management.enums.PaymentType;
import com.project.school_management.enums.PayrollStatus;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.finance.FinanceService;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.user.UserService;

@Controller
@RequestMapping("/admin/finance")
public class FinanceViewController {

    private final FinanceService financeService;
    private final UserService userService;
    private final PresenceTracker presenceTracker;

    public FinanceViewController(
            FinanceService financeService,
            UserService userService,
            PresenceTracker presenceTracker) {
        this.financeService = financeService;
        this.userService = userService;
        this.presenceTracker = presenceTracker;
    }

    @GetMapping
    public String index(Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "finance", "Finance");
            boolean staff = hasFinanceRead(authentication);
            model.addAttribute("staffView", staff);
            if (staff) {
                model.addAttribute("payroll", financeService.listPayroll(null));
                model.addAttribute("payments", financeService.listPayments(null));
            } else {
                FinanceMeResponse mine = financeService.myFinance();
                model.addAttribute("payroll", mine.getPayroll());
                model.addAttribute("payments", mine.getPayments());
            }
            return "pages/finance";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load finance");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/users/{userUuid}")
    @PreAuthorize("hasAuthority('FINANCE_READ')")
    public String userFinance(@PathVariable UUID userUuid, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "finance", "User finance");
            FinanceMeResponse data = financeService.userFinance(userUuid);
            model.addAttribute("finance", data);
            model.addAttribute("payroll", data.getPayroll());
            model.addAttribute("payments", data.getPayments());
            return "pages/finance-user";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/finance";
        }
    }

    @GetMapping("/payroll/new")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public String payrollForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "finance", "Add payroll");
        model.addAttribute("users", userService.getAll());
        model.addAttribute("statuses", PayrollStatus.values());
        return "pages/payroll-form";
    }

    @PostMapping("/payroll")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public String createPayroll(
            @RequestParam UUID userUuid,
            @RequestParam String period,
            @RequestParam BigDecimal amount,
            @RequestParam PayrollStatus status,
            @RequestParam(required = false) String note,
            RedirectAttributes ra) {
        try {
            financeService.createPayroll(userUuid, period, amount, status, note);
            ra.addFlashAttribute("success", "Payroll created");
            return "redirect:/admin/finance?tab=payroll";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/finance/payroll/new";
        }
    }

    @GetMapping("/payments/new")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public String paymentForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "finance", "Add payment");
        model.addAttribute("users", userService.getAll());
        model.addAttribute("types", PaymentType.values());
        model.addAttribute("statuses", PaymentStatus.values());
        return "pages/payment-form";
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public String createPayment(
            @RequestParam UUID userUuid,
            @RequestParam PaymentType paymentType,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam PaymentStatus status,
            @RequestParam(required = false) String note,
            RedirectAttributes ra) {
        try {
            financeService.createPayment(userUuid, paymentType, amount, dueDate, status, note);
            ra.addFlashAttribute("success", "Payment created");
            return "redirect:/admin/finance?tab=payments";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/finance/payments/new";
        }
    }

    @PostMapping("/payroll/{id}/delete")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public String deletePayroll(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            financeService.deletePayroll(id);
            ra.addFlashAttribute("success", "Payroll deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/finance?tab=payroll";
    }

    @PostMapping("/payments/{id}/delete")
    @PreAuthorize("hasAuthority('FINANCE_WRITE')")
    public String deletePayment(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            financeService.deletePayment(id);
            ra.addFlashAttribute("success", "Payment deleted");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/finance?tab=payments";
    }

    private static boolean hasFinanceRead(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "FINANCE_READ".equals(a.getAuthority()));
    }

    private void fillCommon(Model model, Authentication authentication, String activePage, String pageTitle) {
        if (authentication == null || authentication.getName() == null) {
            throw new UserNotFound("No authenticated account");
        }
        DataUser account = userService.getAccountByEmail(authentication.getName());
        model.addAttribute("account", account);
        model.addAttribute("dataUser", account);
        model.addAttribute("username", account.getEmail());
        model.addAttribute("activePage", activePage);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("onlineCount", presenceTracker.snapshot().getOnlineCount());
    }
}
