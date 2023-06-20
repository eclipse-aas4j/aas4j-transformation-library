/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.expressions;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.dom4j.Node;

import com.google.common.hash.Hashing;

public class Expressions {

    private static final Map<String, Expression> constants = new HashMap<>();
    private static final Map<String, Function<Object, Object>> functions = new HashMap<>();

    private static final ValueUtils values = ValueUtils.getInstance();

    static {
        functions.put("list", args -> Helpers.valueToStream(args).collect(Collectors.toList()));

        functions.put("set", args -> Helpers.valueToStream(args).collect(Collectors.toSet()));

        functions.put("range", Helpers.binaryObj((a, b) -> IntStream.rangeClosed(((Number) a).intValue(), ((Number) b).intValue()) //
            .mapToObj(i -> i).collect(Collectors.toList())));

        functions.put("entry", Helpers.binaryObj((list, i) -> {
            if ((int) values.longValue(i) <= 0) {
                throw new IllegalArgumentException("not a positive index: " + i);
            }
            return Helpers.valueToStream(list).skip((int) values.longValue(i) - 1).findFirst().get();
        }));

        functions.put("block", Helpers.reduce((a, b) -> b));

        functions.put("negate", Helpers.unaryObj(values::negate));
        functions.put("max", Helpers.reduce(Expressions::max));
        functions.put("min", Helpers.reduce(Expressions::min));
        functions.put("minus", Helpers.binaryObj(values::subtract));

        functions.put("root", Helpers.binaryDouble((a, b) -> Math.pow(a, 1 / b)));

        functions.put("intersect", args -> {
            @SuppressWarnings("unchecked")
            Stream<Object> stream = (Stream<Object>) Helpers.valueToStream(args);
            return stream.reduce(null, (a, b) -> {
                if (a == null) {
                    // the first element
                    return Helpers.valueToSet(b, true);
                } else {
                    ((Set<?>) a).retainAll(Helpers.valueToSet(b, false));
                    return a;
                }
            });
        });

        functions.put("sin", Helpers.unaryDouble(Math::sin));
        functions.put("cos", Helpers.unaryDouble(Math::cos));
        functions.put("tan", Helpers.unaryDouble(Math::tan));

        functions.put("arcsin", Helpers.unaryDouble(Math::asin));
        functions.put("arccos", Helpers.unaryDouble(Math::acos));
        functions.put("arctan", Helpers.unaryDouble(Math::atan));

        functions.put("abs", Helpers.unaryObj(Expressions::abs));
        functions.put("plus", Helpers.reduce(values::add));
        functions.put("times", Helpers.reduce(values::multiply));
        functions.put("power", Helpers.binaryDouble(Math::pow));
        functions.put("divide", Helpers.binaryObj(values::divide));

        functions.put("eq", Helpers.binaryObj((a, b) -> {
            if (Objects.equals(a, b)) {
                return true;
            }
            return values.compareWithConversion(a, b) == 0;
        }));
        functions.put("lt", Helpers.binaryObj((a, b) -> values.compareWithConversion(a, b) < 0));
        functions.put("leq", Helpers.binaryObj((a, b) -> values.compareWithConversion(a, b) <= 0));
        functions.put("gt", Helpers.binaryObj((a, b) -> values.compareWithConversion(a, b) > 0));
        functions.put("geq", Helpers.binaryObj((a, b) -> values.compareWithConversion(a, b) >= 0));
        functions.put("neq", Helpers.binaryObj((a, b) -> values.compareWithConversion(a, b) != 0));

        functions.put("not", Helpers.unaryObj(arg -> !values.booleanValue(arg)));
        functions.put("or", Helpers.reduce((a, b) -> values.booleanValue(a) || values.booleanValue(b)));
        functions.put("and", Helpers.reduce((a, b) -> values.booleanValue(a) && values.booleanValue(b)));

        functions.put("round", Helpers.unaryDouble(Expressions::round));
        functions.put("ceiling", Helpers.unaryDouble(Math::ceil));
        functions.put("floor", Helpers.unaryDouble(Math::floor));
        functions.put("trunc", Helpers.unaryDouble(Expressions::truncate));

        constants.put("null", new ConstantExpr(null));
        constants.put("pi", new ConstantExpr(Math.PI));
        constants.put("e", new ConstantExpr(Math.E));
        constants.put("NaN", new ConstantExpr(Double.NaN));
        constants.put("nil", new ConstantExpr(Collections.emptyList()));

        // not actually part of the nums1 CD, but NaN is useless without this check
        functions.put("isNaN", Helpers.unaryObj(arg -> Double.isNaN(values.doubleValue(arg))));

        functions.put("println", args -> {
            Iterator<?> it = Helpers.valueToIterator(args);
            while (it.hasNext()) {
                System.out.print(it.next());
                if (it.hasNext()) {
                    System.out.print(" ");
                }
            }
            System.out.println();
            return null;
        });

        // special functions for ID generation
        functions.put("concatenate", args -> {
            Stream<String> strStream = nodeListsToString(Helpers.valueToStream(args));
            return strStream.collect(Collectors.joining());
        });

        functions.put("concatenate_and_hash", args -> {
            Stream<String> strStream = nodeListsToString(Helpers.valueToStream(args));
            String concatenated = strStream.collect(Collectors.joining());
            return Hashing.sha256().hashString(concatenated, StandardCharsets.UTF_8).toString();
        });

        functions.put("generateId", args ->
                "urn:uuid:" + UUID.randomUUID().toString()
        );

        // string encoding
        functions.put("base64", args -> {
            Stream<Object> stream = (Stream<Object>) Helpers.valueToStream(args);
            String concatenated = stream.flatMap(o -> Helpers.valueToStream(o)).map(o -> {
                if (o instanceof Node) {
                    return ((Node) o).getStringValue();
                } else {
                    return String.valueOf(o);
                }
            }).collect(Collectors.joining());
            return Base64.getEncoder().encodeToString(concatenated.getBytes());
        });
    }

    private static Stream<String> nodeListsToString(Stream<?> stream) {
        return stream.flatMap(o -> Helpers.valueToStream(o)).map(o -> {
            if (o instanceof Node) {
                return ((Node) o).getStringValue();
            } else {
                return String.valueOf(o);
            }
        });
    }

    public static Function<Object, Object> getFunctionByName(String name) {
        return functions.get(name);
    }

    public static Expression getConstantByName(String name) {
        return constants.get(name);
    }

    static Object divide(Object a, Object b) {
        return values.divide(a, values.doubleValue(b));
    }

    static Object max(Object a, Object b) {
        if (values.compareWithConversion(a, b) >= 0) {
            return a;
        } else {
            return b;
        }
    }

    static Object min(Object a, Object b) {
        if (values.compareWithConversion(a, b) >= 0) {
            return b;
        } else {
            return a;
        }
    }

    static Object abs(Object x) {
        if (values.compareWithConversion(x, 0.0) >= 0) {
            return x;
        } else {
            return values.negate(x);
        }
    }

    static double truncate(double x) {
        return (long) x;
    }

    static double round(double x) {
        return Math.round(x);
    }

}
