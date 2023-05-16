/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping.model;

import java.util.Map;

import org.eclipse.digitaltwin.aas4j.expressions.Expression;
import io.adminshell.aas.v3.model.LangString;

/**
 * Extension of class {@link LangString} with support for the {@link Template} interface.
 */
public class LangStringTemplate extends LangString implements Template {

    private final Template template = new TemplateSupport(this);

    @Override
    public BindSpecification getBindSpecification() {
        return template.getBindSpecification();
    }

    @Override
    public void setBindSpecification(BindSpecification bindSpecification) {
        template.setBindSpecification(bindSpecification);
    }

    @Override
    public Expression getForeachExpression() {
        return template.getForeachExpression();
    }

    @Override
    public void setForeachExpression(Expression expression) {
        template.setForeachExpression(expression);
    }

    @Override
    public Map<String, Expression> getDefinitions() {
        return template.getDefinitions();
    }

    @Override
    public void setDefinitions(Map<String, Expression> definitions) {
        template.setDefinitions(definitions);
    }

    @Override
    public Map<String, Expression> getVariables() {
        return template.getVariables();
    }

    @Override
    public void setVariables(Map<String, Expression> variables) {
        template.setVariables(variables);
    }
}
