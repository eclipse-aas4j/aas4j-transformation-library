/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.expressions;

import com.sap.dsc.aas.lib.mapping.TransformationContext;

/**
 * Represents the value of a named variable.
 */
public class VarExpr implements Expression {
    /**
     * The variable's name.
     */
	private final String name;

    public VarExpr(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public Object evaluate(TransformationContext ctx) {
        return ctx.getVariables().get(name);
    }

    @Override
    public String evaluateAsString(TransformationContext ctx) {
        return ctx.getVariables().get(name);
    }
}
