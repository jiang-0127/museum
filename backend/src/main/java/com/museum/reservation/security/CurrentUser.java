package com.museum.reservation.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static LoginUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }

    public static Long id() {
        LoginUser user = get();
        return user == null ? null : user.getId();
    }

    public static boolean isAdmin() {
        LoginUser user = get();
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().contains("RESERVATION_ADMIN") || user.getRoles().contains("SYSTEM_ADMIN");
    }
}
