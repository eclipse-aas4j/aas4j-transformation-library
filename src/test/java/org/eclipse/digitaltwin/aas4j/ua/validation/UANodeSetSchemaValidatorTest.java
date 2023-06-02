/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.ua.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.eclipse.digitaltwin.aas4j.transform.validation.SchemaValidator;
import org.eclipse.digitaltwin.aas4j.ua.transform.UANodeSetTransformer;
import org.eclipse.digitaltwin.aas4j.ua.transform.validation.UANodeSetSchemaValidator;

public class UANodeSetSchemaValidatorTest {

    private final String MINIMAL_NODESET = "ua/minimal-nodeset.xml";
    private final String HUGE_NODESET = "ua/EntType.xml";

    SchemaValidator classUnderTest;

    @BeforeEach
    void setUp() throws FileNotFoundException, TransformationException {
        classUnderTest = new UANodeSetSchemaValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {MINIMAL_NODESET, HUGE_NODESET})
    @DisplayName("Validates OPCUA NodeSet xmls. Two valid node sets are validated against v 1.04 schema")
    void validateValidNodeSets(String filePath) throws FileNotFoundException {

        InputStream is = new FileInputStream(
            new File(Thread.currentThread().getContextClassLoader().getResource(filePath).getPath()));

        assertDoesNotThrow(() -> classUnderTest.validate(new UANodeSetTransformer().readXmlDocument(is)));
    }

    @ParameterizedTest
    @ValueSource(strings = {MINIMAL_NODESET, HUGE_NODESET})
    @DisplayName("Validates OPCUA NodeSet xmls. Two invalid node sets are validated against v 1.04 schema")
    void validateInvalidNodeSets(String filePath) throws IOException {

        invalidateNodeSet(filePath).forEach((InputStream is) -> {
            assertThrows(TransformationException.class,
                () -> classUnderTest.validate(new UANodeSetTransformer().readXmlDocument(is)));
        });

    }

    /**
     * Replaces the attribute names of the xml content with invalid names and returns a list of invalid
     * (against schema) xml content
     **/
    private List<InputStream> invalidateNodeSet(String filePath) throws IOException {

        String content = FileUtils.readFileToString(
            new File(Thread.currentThread().getContextClassLoader().getResource(filePath).getPath()),
            StandardCharsets.UTF_8);

        List<InputStream> retList = new ArrayList<>();
        // Alter some attribute names to invalidate against the schema
        retList.add(new ByteArrayInputStream(content.replaceAll("UAVariable", "foo")
            .getBytes(StandardCharsets.UTF_8)));
        retList.add(new ByteArrayInputStream(content.replaceAll("Reference", "fooBar")
            .getBytes(StandardCharsets.UTF_8)));
        retList.add(new ByteArrayInputStream(content.replaceAll("UAObject", "fooFoo")
            .getBytes(StandardCharsets.UTF_8)));
        retList.add(new ByteArrayInputStream(content.replaceAll("UANodeSet", "fooFooBar")
            .getBytes(StandardCharsets.UTF_8)));
        // Blank xml
        retList.add(new ByteArrayInputStream("<x/>".getBytes(StandardCharsets.UTF_8)));
        // Blank file
        retList.add(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
        // Invalid xml file
        retList.add(new ByteArrayInputStream("<foo>foobar".getBytes(StandardCharsets.UTF_8)));

        return retList;
    }
}
