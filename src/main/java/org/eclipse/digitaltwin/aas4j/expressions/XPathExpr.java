/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.expressions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dom4j.Node;

import org.eclipse.digitaltwin.aas4j.mapping.TransformationContext;
import org.eclipse.digitaltwin.aas4j.transform.XPathHelper;

/**
 * Represents an operator that evaluates a specific XPath
 */
public class XPathExpr implements Expression {

    /**
     * Expressions that are evaluated to compute the actual list elements.
     */
	private final List<Expression> args;

    public XPathExpr(List<Expression> args) {
        this.args = args;
    }

    @Override
    public List<Node> evaluate(TransformationContext ctx) {
        // evaluate multiple xpath expressions and create joined stream of all resulting
        // nodes
        return args.stream().map(arg -> arg.evaluate(ctx)).flatMap(value -> {
            if (value instanceof String && ctx.getContextItem() instanceof Node) {
                // evaluate XPath against context node
                return XPathHelper.getInstance().getNodes((Node) ctx.getContextItem(), (String) value).stream();

            } else {
                // invalid XPath or no Node Context
                throw new IllegalArgumentException("Invalid XPath or no Node Context is given.");
            }
        }).collect(Collectors.toList());
    }

    @Override
    public String evaluateAsString(TransformationContext ctx) {
        Optional<String> xpath = args.stream().map(arg -> arg.evaluateAsString(ctx)).findFirst();
        if (xpath.isPresent() && ctx.getContextItem() instanceof Node) {
            return Objects.toString(
                XPathHelper.getInstance().getStringValueOrNull((Node) ctx.getContextItem(), xpath.get()));
        } else {
            return "";
        }
    }
}
