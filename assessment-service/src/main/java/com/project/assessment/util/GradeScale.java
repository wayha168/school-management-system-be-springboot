package com.project.assessment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class GradeScale {

    private GradeScale() {
    }

    public static BigDecimal percent(BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return score.multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal gpaPoints(BigDecimal percent) {
        double p = percent == null ? 0 : percent.doubleValue();
        double points;
        if (p >= 90) {
            points = 4.0;
        } else if (p >= 80) {
            points = 3.0;
        } else if (p >= 70) {
            points = 2.0;
        } else if (p >= 60) {
            points = 1.0;
        } else {
            points = 0.0;
        }
        return BigDecimal.valueOf(points).setScale(2, RoundingMode.HALF_UP);
    }

    public static String letterGrade(BigDecimal percent) {
        double p = percent == null ? 0 : percent.doubleValue();
        if (p >= 90) {
            return "A";
        }
        if (p >= 80) {
            return "B";
        }
        if (p >= 70) {
            return "C";
        }
        if (p >= 60) {
            return "D";
        }
        return "F";
    }

    /** Canonical subject key for storage and GPA grouping. */
    public static String normalizeSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        String trimmed = subject.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    public static String normalizeTerm(String term) {
        if (term == null || term.isBlank()) {
            return "Term 1";
        }
        String trimmed = term.trim().replaceAll("\\s+", " ");
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("monthly ")) {
            // Keep "Monthly YYYY-MM" as stored key for the real calendar month.
            return "Monthly " + trimmed.substring("monthly ".length()).trim();
        }
        if (lower.equals("monthly")) {
            return "Monthly " + java.time.YearMonth.now();
        }
        if (lower.equals("term 1") || lower.equals("term1")) {
            return "Term 1";
        }
        if (lower.equals("term 2") || lower.equals("term2")) {
            return "Term 2";
        }
        if (lower.equals("midterm") || lower.equals("mid term")) {
            return "Midterm";
        }
        if (lower.equals("final")) {
            return "Final";
        }
        return trimmed;
    }
}
