package com.macoshapps.lootvault.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Financial calculations use decimal arithmetic rather than binary floating point. */
public final class AssetMath {
  private AssetMath() {}
  public static BigDecimal money(String input) {
    if (input == null || input.trim().isEmpty()) return BigDecimal.ZERO.setScale(2);
    return new BigDecimal(input.trim().replace(" ", "").replace(",", ".")).setScale(2, RoundingMode.HALF_UP);
  }
  public static BigDecimal total(BigDecimal unitPrice, BigDecimal quantity) {
    if (unitPrice == null || quantity == null || quantity.signum() < 0) throw new IllegalArgumentException("Invalid asset values");
    return unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
  }
  public static BigDecimal profit(BigDecimal cost, BigDecimal value) {
    return value.subtract(cost).setScale(2, RoundingMode.HALF_UP);
  }
  /** Returns null if percentage cannot be computed because cost is zero. */
  public static BigDecimal returnPercent(BigDecimal cost, BigDecimal value) {
    if (cost.signum() == 0) return null;
    return profit(cost, value).multiply(new BigDecimal("100")).divide(cost, 2, RoundingMode.HALF_UP);
  }
}
