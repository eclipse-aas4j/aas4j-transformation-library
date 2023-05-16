/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.expressions;

import org.eclipse.digitaltwin.aas4j.mapping.TransformationContext;

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
