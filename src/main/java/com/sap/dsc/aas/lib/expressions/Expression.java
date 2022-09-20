/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.expressions;

import java.util.Objects;

import com.sap.dsc.aas.lib.mapping.TransformationContext;

/**
 * Represents dynamic expressions within a template.
 */
public interface Expression {
    /**
     * Evaluates the expression using a thread-local context.
     *
     * @return computed result
     */
    Object evaluate(TransformationContext ctx);

    /**
     * evaluates as a String, if possible
     *
     * @param ctx
     * @return
     */
    default String evaluateAsString(TransformationContext ctx) {
        return Objects.toString(evaluate(ctx));
    }
}
