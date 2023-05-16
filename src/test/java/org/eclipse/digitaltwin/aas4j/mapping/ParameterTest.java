/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.mapping.model.Header;
import org.eclipse.digitaltwin.aas4j.mapping.model.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
