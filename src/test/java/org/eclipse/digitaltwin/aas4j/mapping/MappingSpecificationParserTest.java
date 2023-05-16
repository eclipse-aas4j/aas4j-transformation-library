/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.expressions.Expression;
import org.eclipse.digitaltwin.aas4j.expressions.ExpressionWithDefault;
import org.eclipse.digitaltwin.aas4j.mapping.model.MappingSpecification;
import org.eclipse.digitaltwin.aas4j.mapping.model.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.SubmodelElement;

public class MappingSpecificationParserTest {

    private MappingSpecificationParser parser;

    @BeforeEach
    void setup() {
        parser = new MappingSpecificationParser();
    }

    @Test
    void expressionsExample() throws IOException {
        MappingSpecification result = parser
            .loadMappingSpecification("src/test/resources/mappings/simpleMapping_w_expressions.json");

        AssetAdministrationShellEnvironment mapping = result.getAasEnvironmentMapping();

        SubmodelElement submodelElement = mapping.getSubmodels().get(0).getSubmodelElements().get(0);
        Template temp = (Template) submodelElement;
        assertThat(temp.getBindSpecification().getBindings().keySet())
            .containsAtLeastElementsIn(new String[] {"idShort", "value", "mimeType"});
    }

    @Test
    void defaultExpressions() throws IOException {
        MappingSpecification result = parser
            .loadMappingSpecification("src/test/resources/mappings/simpleMapping_w_default.json");

        AssetAdministrationShellEnvironment mapping = result.getAasEnvironmentMapping();

        SubmodelElement submodelElement = mapping.getSubmodels().get(0).getSubmodelElements().get(0);
        Template temp = (Template) submodelElement;
        Map<String, Expression> bindings = temp.getBindSpecification().getBindings();
        Expression valueBinding = bindings.get("value");
        Expression idShortBinding = bindings.get("idShort");
        assertThat(valueBinding instanceof ExpressionWithDefault);
        assertThat(idShortBinding instanceof ExpressionWithDefault);
        String value = (String) ((List) valueBinding.evaluate(null)).get(0);
        String idShort = (String) idShortBinding.evaluate(null);
        assertEquals("Betriebsanleitung_ExpressionValue", value);
        assertEquals("DefaultIdShort", idShort);

    }

    @Test
    void invalidDefaultExpressions() throws IOException {
        assertThrows(InvalidFormatException.class, () -> parser
            .loadMappingSpecification("src/test/resources/mappings/simpleMapping_w_invaliddefault.json"));
    }
}
