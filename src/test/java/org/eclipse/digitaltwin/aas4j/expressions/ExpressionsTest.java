package org.eclipse.digitaltwin.aas4j.expressions;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionsTest {

    @Test
    void getFunctionByName() {
        Function<Object, Object> max = Expressions.getFunctionByName("max");
        List<Double> l = new ArrayList<>();
        l.add(0, 133.4);
        l.add(1, 2490234.40);
        assertEquals(l.get(1), max.apply(l));

        Function<Object, Object> println = Expressions.getFunctionByName("println");
        Object apply = println.apply(l);
        assertNull(apply);

        Function<Object, Object> intersect = Expressions.getFunctionByName("intersect");
        l.add(2, null);
        l.add(3, 133.4);
        Object intersection = intersect.apply(l);
        assertEquals(new HashSet<Object>(),intersection);
    }

    @Test
    void getConstantByName() {
        assertEquals(Math.PI, Expressions.getConstantByName("pi").evaluate(null));
    }

    @Test
    void divide() {
        assertEquals(3.0, Expressions.divide(6.0, 2));
    }

    @Test
    void max() {
        assertEquals(382, Expressions.max(100.0, 382));
    }

    @Test
    void min() {
        assertEquals(100, Expressions.min(100, 1382349));
    }

    @Test
    void abs() {
        assertEquals(10.0, Expressions.abs(-10.0));
    }

    @Test
    void truncate() {
        Double d = new Random().nextDouble();
        assertEquals(d.longValue(), Expressions.truncate(d));
    }

    @Test
    void round() {
        Double d = Double.valueOf(0.92348);
        assertEquals(1.0,Expressions.round(d));
    }
}