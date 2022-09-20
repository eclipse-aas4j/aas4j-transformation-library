/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.mapping.MappingSpecificationParser;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

class GenericDocumentTransformerTest {

    public static final String XML_INPUT = "src/test/resources/ua/aasfull.xml";
    public static final String JSON_CONFIG = "src/test/resources/ua/genericSampleConfig.json";

    private InputStream testInputStream;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {}

    @AfterAll
    static void tearDownAfterClass() throws Exception {}

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.resetBindings();
        testInputStream = Files.newInputStream(Paths.get(XML_INPUT));
    }

    @AfterEach
    void tearDown() throws Exception {}

    @Test
    void testNsBindings() throws TransformationException, IOException {
        DocumentTransformer transformer = new GenericDocumentTransformer();

        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(JSON_CONFIG);

        AssetAdministrationShellEnvironment transform = transformer.execute(testInputStream, mapping);
        Assert.assertNotNull(transform);
        Assert.assertEquals(71, transform.getSubmodels().size());
    }

}
