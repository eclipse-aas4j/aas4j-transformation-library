/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.ua.transform;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.mapping.MappingSpecificationParser;
import com.sap.dsc.aas.lib.mapping.TemplateTransformer;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import com.sap.dsc.aas.lib.transform.DocumentTransformer;
import com.sap.dsc.aas.lib.transform.GenericDocumentTransformer;
import com.sap.dsc.aas.lib.transform.MappingSpecificationDocumentTransformer;
import com.sap.dsc.aas.lib.transform.XPathHelper;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

public class BrowsepathXPathBuilderTest {
    private MappingSpecificationParser parser;
    private final String nodesetMinimalInputFileName = "src/test/resources/ua/minimal-nodeset.xml";
    private final String nodesetBigMachineInput = "src/test/resources/ua/big.machine.nodeset.xml";
    private final String modifiedBigMachineInput = "src/test/resources/ua/modified.big.machine.nodeset.xml";
    private final String browsePathConfigTest = "src/test/resources/mappings/generic/browsepathTest.json";
    private final String invalidBrowsePathConfigTest = "src/test/resources/mappings/generic/invalidBrowsepathTest.json";
    private final String browsePathChildrenConfigTest = "src/test/resources/mappings/generic/browsePathChildrenTest.json";
    private Document xmlDoc;
    private TemplateTransformer transformer;

    @BeforeEach
    void setUp() throws TransformationException, IOException {
        parser = new MappingSpecificationParser();
        transformer = new TemplateTransformer();
        try (InputStream nodesetStream = Files.newInputStream(Paths.get(nodesetMinimalInputFileName))) {
            UANodeSetTransformer transformer = new UANodeSetTransformer();
            xmlDoc = transformer.readXmlDocument(nodesetStream);
        }
    }

    @Test
    void simpleTest() {
        BrowsepathXPathBuilder.updateInstance(xmlDoc);
        BrowsepathXPathBuilder pathBuilder = BrowsepathXPathBuilder.getInstance();
        String[] browsePath = {"1:ExampleObject", "1:ExampleIntegerVariable"};
        String nodeId = pathBuilder.getNodeIdFromBrowsePath(browsePath);
        assertEquals("ns=1;i=1010", nodeId);
        String exp = pathBuilder.pathExpression(browsePath);
        List<Node> nodeFromExp = XPathHelper.getInstance().getNodes(xmlDoc, exp);
        assertNotNull(nodeFromExp);
        assertEquals(nodeFromExp.size(), 1);
        assertThat(nodeFromExp.get(0)).isInstanceOf(Element.class);
        assertEquals(nodeId, ((Element) nodeFromExp.get(0)).attribute("NodeId").getValue());
        assertEquals("http://iwu.fraunhofer.de/c32/arno", pathBuilder.getNamespace(browsePath[0]));

    }

    @Test
    void testUaBrowsePathExpr() throws IOException, TransformationException {
        BrowsepathXPathBuilder.updateInstance(xmlDoc);
        MappingSpecification spec = parser
            .loadMappingSpecification(browsePathConfigTest);
        AssetAdministrationShellEnvironment aas = transformer.transform(spec, xmlDoc, null);
        assertNotNull(aas);
        assertEquals(1, aas.getSubmodels().size());
        assertEquals(aas.getSubmodels().get(0).getIdShort(), "ns=1;i=1010");
    }

    @Test
    void testInvalidConfigFile() throws IOException {
        BrowsepathXPathBuilder.updateInstance(xmlDoc);
        MappingSpecification spec = parser
            .loadMappingSpecification(invalidBrowsePathConfigTest);
        RuntimeException e = assertThrows(RuntimeException.class, () -> transformer.transform(spec, xmlDoc, null));
        Throwable cause = e.getCause();
        assertThat(cause instanceof IllegalArgumentException);
    }

    @Test
    void testHasAndIsRelations() throws IOException, TransformationException {
        testBrowsePathChildren(nodesetBigMachineInput);
        testBrowsePathChildren(modifiedBigMachineInput);
    }

    void testBrowsePathChildren(String input) throws IOException, TransformationException {
        try (InputStream nodesetStream = Files.newInputStream(Paths.get(input))) {
            UANodeSetTransformer uANodeSetTransformer = new UANodeSetTransformer();
            Document uaDoc = uANodeSetTransformer.readXmlDocument(nodesetStream);
            BrowsepathXPathBuilder.updateInstance(uaDoc);
            BrowsepathXPathBuilder pathBuilder = BrowsepathXPathBuilder.getInstance();
            MappingSpecification spec = new MappingSpecificationParser()
                .loadMappingSpecification(browsePathChildrenConfigTest);
            AssetAdministrationShellEnvironment aas = new TemplateTransformer().transform(spec, xmlDoc, null);
            assertNotNull(aas);
            assertEquals(aas.getSubmodels().size(), 11);
            String[] browsePath = {"3:Machines", "4:KR16-2-MotionSystem", "2:Controllers", "2:0", "2:CurrentUser", "2:Level"};
            String nodeId = pathBuilder.getNodeIdFromBrowsePath(browsePath);
            assertEquals(nodeId, "ns=4;i=6019");
            String exp = pathBuilder.pathExpression(browsePath);
            List<Node> nodeFromExp = XPathHelper.getInstance().getNodes(uaDoc, exp);
            assertNotNull(nodeFromExp);
            assertEquals(nodeFromExp.size(), 1);
            assertThat(nodeFromExp.get(0)).isInstanceOf(Element.class);
            assertEquals(nodeId, ((Element) nodeFromExp.get(0)).attribute("NodeId").getValue());
            assertEquals("http://exp.organization.com/UA/BigMachine", pathBuilder.getNamespace(browsePath[1]));
        }
    }

}
