package com.macoshapps.lootvault.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money and percentage rules. Amounts are always BigDecimal; float and double are never used
 * for currency, so totals and gains stay exact.
 */
public final class AssetMath {
    public static final int MONEY_SCALE = 2;
    public static final int QUANTITY_SCALE = 4;
    public static final int PERCENT_SCALE = 1;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private AssetMath() {}

    /** Parses input such as "1 234,50" or "1234.5". Returns null when the input is not a non-negative number. */
    public static BigDecimal parseMoney(String input) {
        BigDecimal value = parse(input);
        return value == null ? null : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Parses a quantity, which may be fractional (for example 0.5 kg). */
    public static BigDecimal parseQuantity(String input) {
        BigDecimal value = parse(input);
        return value == null ? null : value.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal parse(String input) {
        if (input == null) return null;
        String normalized = input.replaceAll("\\s", "").replace(',', '.');
        if (normalized.isEmpty()) return null;
        try {
            BigDecimal value = new BigDecimal(normalized);
            return value.signum() < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Unit price multiplied by quantity, rounded to money precision. */
    public static BigDecimal lineTotal(BigDecimal unit, BigDecimal quantity) {
        return unit.multiply(quantity).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Current market value minus acquisition cost. */
    public static BigDecimal gain(BigDecimal cost, BigDecimal value) {
        return value.subtract(cost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Gain as a percentage of acquisition cost. Null when cost is zero, because the result is undefined. */
    public static BigDecimal returnPercent(BigDecimal cost, BigDecimal value) {
        if (cost.signum() == 0) return null;
        return gain(cost, value).multiply(HUNDRED).divide(cost, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    /** Part as a percentage of total. Null when the total is zero. */
    public static BigDecimal share(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) return null;
        return part.multiply(HUNDRED).divide(total, PERCENT_SCALE, RoundingMode.HALF_UP);
    }
}
