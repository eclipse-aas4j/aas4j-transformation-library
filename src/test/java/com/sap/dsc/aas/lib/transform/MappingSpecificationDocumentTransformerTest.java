/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.XMLConstants;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.mapping.MappingSpecificationParser;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

class MappingSpecificationDocumentTransformerTest {

    private MappingSpecificationDocumentTransformer transformer;
    private MappingSpecificationParser parser;

    @BeforeEach
    void setUp() throws Exception {
        transformer = new MappingSpecificationDocumentTransformer();
        parser = new MappingSpecificationParser();
        TestUtils.resetBindings();
    }

    @Test
    void testUAEuromapToAASNameplate() throws Exception {
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/ua/euromap2nameplate.json");

        Document testDoc = null;
        try (InputStream testResource = Files.newInputStream(Paths.get("src/test/resources/ua/example_euromap.xml"))) {

            SAXReader reader = new SAXReader();
            reader.setEncoding("UTF-8");
            reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            testDoc = reader.read(testResource);
        }

        XPathHelper.getInstance().setNamespaceBinding("ua", "http://opcfoundation.org/UA/2011/03/UANodeSet.xsd");
        XPathHelper.getInstance().setNamespaceBinding("uax", "http://opcfoundation.org/UA/2008/02/Types.xsd");

        AssetAdministrationShellEnvironment createShellEnv = transformer.createShellEnv(testDoc, mapSpec, null);
    }

}
