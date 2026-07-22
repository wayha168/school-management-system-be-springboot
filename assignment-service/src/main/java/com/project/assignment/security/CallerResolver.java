package com.project.assignment.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CallerResolver {

    public CallerContext require() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new AccessDeniedException("No request context");
        }
        HttpServletRequest request = attrs.getRequest();
        Object raw = request.getAttribute(AssignmentAuthFilter.ATTR_CALLER);
        if (!(raw instanceof CallerContext caller) || caller.userUuid() == null) {
            throw new AccessDeniedException("Caller required");
        }
        return caller;
    }
}
