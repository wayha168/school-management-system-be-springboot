package com.project.school_management.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    /** 4.0 scale from percent. */
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
}
