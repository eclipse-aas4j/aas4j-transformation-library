/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.expressions;

import java.util.List;
import java.util.stream.Collectors;

import org.dom4j.Element;
import org.dom4j.Node;

import org.eclipse.digitaltwin.aas4j.mapping.TransformationContext;
import org.eclipse.digitaltwin.aas4j.ua.transform.BrowsepathXPathBuilder;

public class UaChildrenExpr implements Expression {

	private final List<Expression> args;

    public UaChildrenExpr(List<Expression> args) {
        this.args = args;
    }

    @Override
    public List<Node> evaluate(TransformationContext ctx) {
        if (!(ctx.getContextItem() instanceof Node)) {
            throw new IllegalArgumentException("no Node Context is given.");
        }
        List<String> path = args.stream().map(arg -> arg.evaluate(ctx))
            .filter(val -> val instanceof String).map(val -> (String) val).collect(Collectors.toList());
        String[] pathElems = new String[path.size()];
        Node uaNode = BrowsepathXPathBuilder.getInstance().getNodeFromBrowsePath(path.toArray(pathElems));
        if (uaNode instanceof Element) {
            return BrowsepathXPathBuilder.getInstance().getUaChildren((Element) uaNode);
        } else {
            throw new IllegalArgumentException(
                "@uaBrowsePath should be array of path elements as String, and should match exactly one UaNode.");
        }
    }
}
