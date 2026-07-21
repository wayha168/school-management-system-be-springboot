package com.project.assessment.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CallerResolver {

    public CallerContext require() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            Object caller = request.getAttribute(AssessmentAuthFilter.ATTR_CALLER);
            if (caller instanceof CallerContext ctx && (ctx.userUuid() != null || ctx.email() != null)) {
                return ctx;
            }
        }
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Caller context required");
    }
}
