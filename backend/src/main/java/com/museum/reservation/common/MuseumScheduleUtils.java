package com.museum.reservation.common;

import com.museum.reservation.module.museum.entity.Museum;

import java.time.LocalDate;

public final class MuseumScheduleUtils {

    private MuseumScheduleUtils() {
    }

    public static boolean isClosed(Museum museum, LocalDate date) {
        if (museum == null || date == null || museum.getClosedDays() == null
                || museum.getClosedDays().isBlank()) {
            return false;
        }
        int weekday = date.getDayOfWeek().getValue();
        for (String part : museum.getClosedDays().split(",")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                if (Integer.parseInt(part.trim()) == weekday) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // ignore dirty data
            }
        }
        return false;
    }
}