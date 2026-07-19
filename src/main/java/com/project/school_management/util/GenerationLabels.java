package com.project.school_management.util;

public final class GenerationLabels {

    private GenerationLabels() {
    }

    public static String code(Integer generation) {
        return generation == null ? null : "G" + generation;
    }

    public static String label(Integer generation) {
        if (generation == null) {
            return null;
        }
        return ordinal(generation) + " generation";
    }

    /** e.g. G9 · 9th generation · 2025 */
    public static String display(Integer generation, Integer academicYear) {
        if (generation == null && academicYear == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (generation != null) {
            sb.append(code(generation)).append(" · ").append(label(generation));
        }
        if (academicYear != null) {
            if (!sb.isEmpty()) {
                sb.append(" · ");
            }
            sb.append(academicYear);
        }
        return sb.toString();
    }

    private static String ordinal(int n) {
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return n + "th";
        }
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
