/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.expressions;

import org.eclipse.digitaltwin.aas4j.mapping.TransformationContext;

/**
 * Represents the value of a named definition.
 */
public class DefExpr implements Expression {
    /**
     * The definition's name.
     */
    private final String name;

    public DefExpr(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public Object evaluate(TransformationContext ctx) {
        return ctx.getDefinitions().get(name).evaluate(ctx);
    }

    @Override
    public String evaluateAsString(TransformationContext ctx) {
        return ctx.getDefinitions().get(name).evaluateAsString(ctx);
    }
}
