/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.expressions;

import com.sap.dsc.aas.lib.mapping.TransformationContext;

/**
 * Represents a constant value.
 */
public class ConstantExpr implements Expression {
    /**
     * The constant value.
     */
    private final Object value;

    public ConstantExpr(Object value) {
        this.value = value;
    }

    @Override
    public Object evaluate(TransformationContext ctx) {
        return value;
    }

    public Object getValue() {
        return value;
    }
}
