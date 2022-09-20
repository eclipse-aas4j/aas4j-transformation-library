/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.mapping.model.Header;
import com.sap.dsc.aas.lib.mapping.model.Parameter;

public class ParameterTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws Exception {
        TestUtils.resetBindings();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void fromJsonString() throws JsonMappingException, JsonProcessingException {
        String input = "{\"@parameters\": {\"placeholderName1\": \"ui text 1\", \"placeholderName2\": \"ui text 2\"}}";

        Header header = objectMapper.readValue(input, Header.class);
        List<Parameter> parameters = header.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            assertEquals("placeholderName" + (i + 1), parameters.get(i).getName());
            assertEquals("ui text " + (i + 1), parameters.get(i).getDescription());
        }
    }

}
