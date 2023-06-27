/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.eclipse.digitaltwin.aas4j.mapping.MappingSpecificationParser;
import org.eclipse.digitaltwin.aas4j.mapping.model.MappingSpecification;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericDocumentTransformerTest {

    public static final String XML_INPUT = "src/test/resources/ua/aasfull.xml";
    public static final String JSON_CONFIG = "src/test/resources/ua/genericSampleConfig.json";
    public static final String SPARQL_XML_INPUT = "src/test/resources/xml/groupable.xml";
    public static final String NESTED_FOR_EACH_JSON_CONFIG = "src/test/resources/mappings/generic/nestedForEach.json";


    private InputStream testInputStream;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {}

    @AfterAll
    static void tearDownAfterClass() throws Exception {}

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.resetBindings();
    }

    @AfterEach
    void tearDown() throws Exception {}

    @Test
    void testNsBindings() throws TransformationException, IOException {
        testInputStream = Files.newInputStream(Paths.get(XML_INPUT));
        DocumentTransformer transformer = new GenericDocumentTransformer();

        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(JSON_CONFIG);

        AssetAdministrationShellEnvironment transform = transformer.execute(testInputStream, mapping);
        Assert.assertNotNull(transform);
        Assert.assertEquals(71, transform.getSubmodels().size());
    }

    @Test
    void testNestedForEach() throws IOException, TransformationException {
        testInputStream = Files.newInputStream(Paths.get(SPARQL_XML_INPUT));
        DocumentTransformer transformer = new GenericDocumentTransformer();

        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(NESTED_FOR_EACH_JSON_CONFIG);

        AssetAdministrationShellEnvironment transform = transformer.execute(testInputStream, mapping);

        assertEquals(2, transform.getSubmodels().size());
        assertTrue(transform.getSubmodels().stream().anyMatch(sm -> sm.getIdShort().equals("131")));
    }

}
