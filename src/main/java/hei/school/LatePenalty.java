package hei.school;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


public final class LatePenalty {

    private static final long DAYS_PER_PENALTY_STEP = 1;
    private static final int PENALTY_PERCENT_PER_DAY = 10;
    private static final int MAX_PENALTY_PERCENT = 50;

    private LatePenalty() {
    }

    public static long daysLate(LocalDate deadline, LocalDate submittedDate) {
        return Math.max(0, ChronoUnit.DAYS.between(deadline, submittedDate));
    }

    public static Result calculate(BigDecimal rawPoints, BigDecimal maxPoints, LocalDate deadline,
            LocalDate submittedDate) {
        long daysLate = daysLate(deadline, submittedDate);
        int penaltyPercent = (int) Math.min(daysLate / DAYS_PER_PENALTY_STEP * PENALTY_PERCENT_PER_DAY,
                MAX_PENALTY_PERCENT);
        BigDecimal rawGrade = rawPoints.multiply(BigDecimal.valueOf(20))
                .divide(maxPoints, 1, RoundingMode.HALF_UP);
        BigDecimal grade = rawPoints.multiply(BigDecimal.valueOf(20))
                .multiply(BigDecimal.valueOf(100 - penaltyPercent))
                .divide(maxPoints.multiply(BigDecimal.valueOf(100)), 1, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        return new Result(daysLate, penaltyPercent, rawGrade, grade);
    }

    public record Result(long daysLate, int penaltyPercent, BigDecimal rawGrade, BigDecimal grade) {
    }
}
