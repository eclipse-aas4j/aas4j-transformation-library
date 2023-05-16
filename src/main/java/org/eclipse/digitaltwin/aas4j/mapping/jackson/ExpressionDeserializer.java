/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.eclipse.digitaltwin.aas4j.expressions.*;

public class ExpressionDeserializer extends JsonDeserializer<Expression> {

    public ExpressionDeserializer() {}

    @Override
    public Expression deserialize(JsonParser jp, DeserializationContext dc)
        throws IOException, JsonProcessingException {
        JsonToken token = jp.currentToken();
        if (token == JsonToken.START_OBJECT) {
            Expression result = null;
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                // read property name
                String property = jp.getCurrentName();

                // move to the corresponding value
                jp.nextToken();

                if (property.equals("default")) {
                    if (result == null) {
                        throw new InvalidFormatException(jp, "Default is used for no Expression",
                            jp.getValueAsString(), Expression.class);
                    }
                    Expression toWrap = result;
                    Expression defaultExpr = jp.getCodec().readValue(jp, Expression.class);
                    result = new ExpressionWithDefault(toWrap, defaultExpr);
                }

                else if (property.startsWith("@")) {
                    String symbol = property.substring(1);

                    // deserialize value as expression
                    Expression argsExpr = jp.getCodec().readValue(jp, Expression.class);
                    List<Expression> argsList;
                    if (argsExpr instanceof ListExpr) {
                        argsList = Arrays.asList(((ListExpr) argsExpr).getArgs());
                    } else {
                        argsList = Collections.singletonList(argsExpr);
                    }

                    // symbol is some built-in function
                    Function<Object, Object> f = Expressions.getFunctionByName(symbol);
                    if (f != null) {
                        result = new BuiltinCallExpr(f, argsList);
                    }

                    if (result == null) {
                        // symbol is some constant
                        Expression constant = Expressions.getConstantByName(symbol);
                        if (constant != null) {
                            result = constant;
                            continue;
                        }
                    }

                    if (result == null) {
                        // symbol needs special handling
                        switch (symbol) {
                            case "xpath":
                                result = new XPathExpr(argsList);
                                break;
                            case "caexAttributeName":
                                if (argsList.size() == 1 && argsList.get(0) instanceof ConstantExpr
                                    && ((ConstantExpr) argsList.get(0)).getValue() instanceof String) {
                                    String attributeName = (String) ((ConstantExpr) argsList.get(0)).getValue();
                                    result = new CaexAttributeNameExpr(attributeName);
                                } else {
                                    throw new InvalidFormatException(jp,
                                        "Only string constants are supported as variable names",
                                        jp.getValueAsString(), Expression.class);
                                }
                                break;
                            case "uaBrowsePath":
                                result = new BrowsePathExpr(argsList);
                                break;
                            case "uaChildren":
                                result = new UaChildrenExpr(argsList);
                                break;
                            case "var":
                                if (argsList.size() == 1 &&
                                    argsList.get(0) instanceof ConstantExpr &&
                                    ((ConstantExpr) argsList.get(0)).getValue() instanceof String) {
                                    result = new VarExpr((String) ((ConstantExpr) argsList.get(0)).getValue());
                                } else {
                                    throw new InvalidFormatException(jp, "Only string constants are supported as variable names",
                                        jp.getValueAsString(), Expression.class);
                                }
                                break;
                            case "def":
                                if (argsList.size() == 1 && argsList.get(0) instanceof ConstantExpr
                                    && ((ConstantExpr) argsList.get(0)).getValue() instanceof String) {
                                    result = new DefExpr((String) ((ConstantExpr) argsList.get(0)).getValue());
                                } else {
                                    throw new InvalidFormatException(jp,
                                        "Only string constants are supported as definition names",
                                        jp.getValueAsString(), Expression.class);
                                }
                                break;
                        }
                    }

                    if (result == null) {
                        throw new InvalidFormatException(jp, "Invalid operator: " + property, jp.getValueAsString(), Expression.class);
                    }
                } else {
                    // read value and ignore for now
                    jp.getCodec().readTree(jp);
                }
            }
            if (result == null) {
                throw new InvalidFormatException(jp, "Missing operator", jp.getValueAsString(), Expression.class);
            }
            return result;
        } else if (token == JsonToken.START_ARRAY) {
            List<Expression> elements = new ArrayList<>();
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                // deserialize value as expression
                elements.add(jp.getCodec().readValue(jp, Expression.class));
            }
            return new ListExpr(elements);
        } else if (token.isNumeric()) {
            return new ConstantExpr(jp.getNumberValue());
        } else if (token.isBoolean()) {
            return new ConstantExpr(jp.getBooleanValue());
        } else if (token == JsonToken.VALUE_STRING) {
            return new ConstantExpr(jp.getValueAsString());
        }
        throw new InvalidFormatException(jp, "Invalid expression", jp.getValueAsString(), Expression.class);
    }
}
