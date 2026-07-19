package com.project.school_management.controller.view;

import java.util.UUID;

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

import com.project.school_management.dto.request.UserRequestDto;
import com.project.school_management.dto.request.UserRequestReplyDto;
import com.project.school_management.dto.request.UserRequestResponse;
import com.project.school_management.dto.user.DataUser;
import com.project.school_management.enums.RequestCategory;
import com.project.school_management.enums.RequestStatus;
import com.project.school_management.exception.UserNotFound;
import com.project.school_management.service.presence.PresenceTracker;
import com.project.school_management.service.request.UserRequestService;
import com.project.school_management.service.user.UserService;

@Controller
@RequestMapping("/admin/requests")
public class RequestViewController {

    private final UserRequestService userRequestService;
    private final UserService userService;
    private final PresenceTracker presenceTracker;

    public RequestViewController(
            UserRequestService userRequestService,
            UserService userService,
            PresenceTracker presenceTracker) {
        this.userRequestService = userRequestService;
        this.userService = userService;
        this.presenceTracker = presenceTracker;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) RequestStatus status,
            Authentication authentication,
            Model model,
            RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "requests", "Requests");
            boolean staff = authentication.getAuthorities().stream()
                    .anyMatch(a -> "REQUEST_WRITE".equals(a.getAuthority()));
            model.addAttribute("staffInbox", staff);
            model.addAttribute("requests", staff
                    ? userRequestService.listAll(status)
                    : userRequestService.listMine());
            model.addAttribute("selectedStatus", status);
            model.addAttribute("statuses", RequestStatus.values());
            return "pages/requests";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Unable to load requests");
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/new")
    public String createForm(Authentication authentication, Model model) {
        fillCommon(model, authentication, "requests", "New request");
        model.addAttribute("categories", RequestCategory.values());
        DataUser account = (DataUser) model.getAttribute("account");
        if (account != null && account.getSchoolName() != null) {
            // mailto uses school email from detail when available via create response later
        }
        return "pages/request-form";
    }

    @PostMapping
    public String create(
            @RequestParam String subject,
            @RequestParam String body,
            @RequestParam RequestCategory category,
            RedirectAttributes ra) {
        try {
            UserRequestDto dto = new UserRequestDto();
            dto.setSubject(subject);
            dto.setBody(body);
            dto.setCategory(category);
            UserRequestResponse created = userRequestService.create(dto);
            ra.addFlashAttribute("success", "Request submitted");
            return "redirect:/admin/requests/" + created.getUuid();
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/requests/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Authentication authentication, Model model, RedirectAttributes ra) {
        try {
            fillCommon(model, authentication, "requests", "Request");
            UserRequestResponse item = userRequestService.getById(id);
            model.addAttribute("item", item);
            model.addAttribute("statuses", RequestStatus.values());
            boolean staff = authentication.getAuthorities().stream()
                    .anyMatch(a -> "REQUEST_WRITE".equals(a.getAuthority()));
            model.addAttribute("staffInbox", staff);
            if (item.getSchoolEmail() != null) {
                model.addAttribute(
                        "mailtoLink",
                        "mailto:" + item.getSchoolEmail()
                                + "?subject=" + encode(item.getSubject())
                                + "&body=" + encode(item.getBody()));
            }
            return "pages/request-detail";
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/requests";
        }
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAuthority('REQUEST_WRITE')")
    public String reply(
            @PathVariable UUID id,
            @RequestParam RequestStatus status,
            @RequestParam(required = false) String adminReply,
            RedirectAttributes ra) {
        try {
            UserRequestReplyDto reply = new UserRequestReplyDto();
            reply.setStatus(status);
            reply.setAdminReply(adminReply);
            userRequestService.reply(id, reply);
            ra.addFlashAttribute("success", "Request updated");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/requests/" + id;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
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
