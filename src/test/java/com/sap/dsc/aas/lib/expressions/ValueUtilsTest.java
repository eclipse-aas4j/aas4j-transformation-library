package com.sap.dsc.aas.lib.expressions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class ValueUtilsTest {

    private ValueUtils instance;

    @BeforeEach
    void setUp() {
        instance = ValueUtils.getInstance();
    }

    @Test
    void getNumericType() {
        ValueUtils.ValueType numericType = instance.getNumericType(
                ValueUtils.ValueType.FLOAT, ValueUtils.ValueType.BIGDECIMAL, false);
        assertEquals(numericType, ValueUtils.ValueType.BIGDECIMAL);
    }

    @Test
    void compareWithConversion() {
        assertEquals(1, instance.compareWithConversion(1000239489, -1034.04));
        assertEquals(0, instance.compareWithConversion(-1034.04, -1034.04));
    }

    @Test
    void stringValue() {
        assertEquals("-12389.2348", instance.stringValue(-12389.2348, true));
    }

    @Test
    void longValue() {
        assertEquals(100, instance.longValue(100.92382348));
    }

    @Test
    void doubleValue() {
        assertEquals(100.0, instance.doubleValue(100));
    }

    @Test
    void bigDecValue() {
        assertEquals(BigDecimal.valueOf(100), instance.bigDecValue(100));
        assertEquals(BigDecimal.valueOf(100), instance.bigDecValue(100.0));
    }

    @Test
    void bigIntValue() {
        assertEquals(BigInteger.valueOf(100), instance.bigIntValue(100));
        assertEquals(BigInteger.valueOf(100), instance.bigIntValue(100.01));
    }

    @Test
    void booleanValue() {
        assertEquals(true, instance.booleanValue(1));
    }

    @Test
    void add() {
        assertEquals(100.3, instance.add(100, 0.3));
    }

    @Test
    void subtract() {
        assertEquals(1003, instance.subtract(1000, -3));
    }

    @Test
    void negate() {
        assertEquals(100.0, instance.negate(-100.0));
    }

    @Test
    void multiply() {
        assertEquals(100.0, instance.multiply(10, 10.0));
    }

    @Test
    void divide() {
        assertEquals(15, instance.divide(-30, -2));
    }

    @Test
    void remainder() {
        assertEquals(1, instance.remainder(5, 2));
    }

    @Test
    void createInteger() {
        assertEquals((float) 100.0, instance.createInteger(ValueUtils.ValueType.FLOAT, (long) 100.0000230));
    }

    @Test
    void createReal() {
        assertEquals((float) 100.0, instance.createReal(ValueUtils.ValueType.FLOAT, 100.0));
    }

    @Test
    void convertValue() {
        assertEquals((float) 100.0, instance.convertValue(Float.class, 100, 200.0));
        assertEquals("100", instance.convertValue(String.class, 100, "nope"));
    }
}