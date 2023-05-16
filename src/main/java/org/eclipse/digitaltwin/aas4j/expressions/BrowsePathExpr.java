/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.expressions;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.digitaltwin.aas4j.mapping.TransformationContext;
import org.eclipse.digitaltwin.aas4j.ua.transform.BrowsepathXPathBuilder;

public class BrowsePathExpr implements Expression {

    private final List<Expression> args;

    public BrowsePathExpr(List<Expression> args) {
        this.args = args;
    }

    @Override
    public String evaluate(TransformationContext ctx) {

        // Node context is not relevant to Browsepath functionality
        List<String> path = args.stream().map(arg -> arg.evaluate(ctx)).filter(val -> val instanceof String).map(val -> (String) val)
            .collect(Collectors.toList());
        String[] pathElems = new String[path.size()];
        String nodeId = BrowsepathXPathBuilder.getInstance().getNodeIdFromBrowsePath(path.toArray(pathElems));
        if (!path.isEmpty() && nodeId != null) {
            return nodeId;
        } else {
            throw new IllegalArgumentException(
                "@uaBrowsePath should be array of path elements as String, and should match exactly one UaNode.");
        }
    }
}
