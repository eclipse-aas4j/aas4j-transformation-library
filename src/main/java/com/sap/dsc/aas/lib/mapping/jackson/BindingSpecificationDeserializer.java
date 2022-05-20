/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.mapping.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sap.dsc.aas.lib.expressions.Expression;
import com.sap.dsc.aas.lib.mapping.model.BindSpecification;

public class BindingSpecificationDeserializer extends JsonDeserializer<BindSpecification> {

    public BindingSpecificationDeserializer() {}

    @Override
    public BindSpecification deserialize(JsonParser jp, DeserializationContext dc)
        throws IOException, JsonProcessingException {
        BindSpecification spec = new BindSpecification();

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            // read property name
            String property = jp.getCurrentName();

            // move to the corresponding value
            jp.nextToken();

            Expression valueExpr = jp.getCodec().readValue(jp, Expression.class);
            spec.setBinding(property, valueExpr);
        }

        return spec;
    }
}
