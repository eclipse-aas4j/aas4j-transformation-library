/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.mapping.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.dsc.aas.lib.expressions.Expression;

/**
 * Interface for the configuration of AAS model templates.
 */
public interface Template {
    BindSpecification getBindSpecification();

    @JsonProperty("@bind")
    void setBindSpecification(BindSpecification bindSpecification);

    Expression getForeachExpression();

    @JsonProperty("@foreach")
    void setForeachExpression(Expression expression);

    Map<String, Expression> getDefinitions();

    @JsonProperty("@definitions")
    void setDefinitions(Map<String, Expression> definitions);

    Map<String, Expression> getVariables();

    @JsonProperty("@variables")
    void setVariables(Map<String, Expression> variables);
}
