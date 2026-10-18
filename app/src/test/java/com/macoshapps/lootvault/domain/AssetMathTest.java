package com.macoshapps.lootvault.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import org.junit.Test;

public class AssetMathTest {

    @Test
    public void parsesCommaAndDotDecimals() {
        assertEquals(new BigDecimal("1234.50"), AssetMath.parseMoney("1 234,50"));
        assertEquals(new BigDecimal("1234.50"), AssetMath.parseMoney("1234.5"));
    }

    @Test
    public void rejectsInvalidAndNegativeInput() {
        assertNull(AssetMath.parseMoney(null));
        assertNull(AssetMath.parseMoney(""));
        assertNull(AssetMath.parseMoney("abc"));
        assertNull(AssetMath.parseMoney("-5"));
    }

    @Test
    public void lineTotalIsExact() {
        // Binary floating point gives 0.30000000000000004 here; decimal arithmetic must give 0.30.
        assertEquals(new BigDecimal("0.30"),
                AssetMath.lineTotal(new BigDecimal("0.10"), AssetMath.parseQuantity("3")));
    }

    @Test
    public void gainAndPercentage() {
        assertEquals(new BigDecimal("50.00"),
                AssetMath.gain(new BigDecimal("100.00"), new BigDecimal("150.00")));
        assertEquals(new BigDecimal("50.0"),
                AssetMath.returnPercent(new BigDecimal("100.00"), new BigDecimal("150.00")));
        assertEquals(new BigDecimal("-25.0"),
                AssetMath.returnPercent(new BigDecimal("200.00"), new BigDecimal("150.00")));
    }

    @Test
    public void percentageIsUndefinedForZeroCost() {
        assertNull(AssetMath.returnPercent(new BigDecimal("0.00"), new BigDecimal("10.00")));
    }

    @Test
    public void shareOfTotal() {
        assertEquals(new BigDecimal("25.0"), AssetMath.share(new BigDecimal("25"), new BigDecimal("100")));
        assertNull(AssetMath.share(BigDecimal.ONE, BigDecimal.ZERO));
    }
}
