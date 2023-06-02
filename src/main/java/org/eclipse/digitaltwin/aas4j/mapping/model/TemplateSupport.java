/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping.model;

import java.util.Map;

import org.eclipse.digitaltwin.aas4j.expressions.Expression;

public class TemplateSupport implements Template {

    private Object target;

    private BindSpecification bindSpecification;
    private Expression foreachExpression;
    private Map<String, Expression> definitions;
    private Map<String, Expression> variables;

    public TemplateSupport() {}

    public TemplateSupport(Object target) {
        this.target = target;
    }

    protected void setTarget(Object target) {
        this.target = target;
    }

    protected Object getTarget() {
        return target;
    }

    @Override
    public BindSpecification getBindSpecification() {
        return bindSpecification;
    }

    @Override
    public void setBindSpecification(BindSpecification bindSpecification) {
        this.bindSpecification = bindSpecification;
    }

    @Override
    public Expression getForeachExpression() {
        return foreachExpression;
    }

    @Override
    public void setForeachExpression(Expression foreachExpression) {
        this.foreachExpression = foreachExpression;
    }

    @Override
    public Map<String, Expression> getDefinitions() {
        return definitions;
    }

    @Override
    public void setDefinitions(Map<String, Expression> definitions) {
        this.definitions = definitions;
    }

    @Override
    public Map<String, Expression> getVariables() {
        return variables;
    }

    @Override
    public void setVariables(Map<String, Expression> variables) {
        this.variables = variables;
    }
}
