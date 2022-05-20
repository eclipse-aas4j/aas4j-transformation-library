/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.XMLConstants;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import com.sap.dsc.aas.lib.transform.GenericDocumentTransformer;
import com.sap.dsc.aas.lib.transform.XPathHelper;

import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.File;
import io.adminshell.aas.v3.model.KeyElements;
import io.adminshell.aas.v3.model.Property;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.SubmodelElement;
import io.adminshell.aas.v3.model.SubmodelElementCollection;

public class TemplateTransformerTest {

    private MappingSpecificationParser parser;
    private TemplateTransformer aasMappingTransformer;

    // static {
    // System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
    // }

    @BeforeEach
    void setup() {
        parser = new MappingSpecificationParser();
        aasMappingTransformer = new TemplateTransformer();
    }

    @Test
    void testNestedForeachEvaluations() throws IOException {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/foreachTest.json");

        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, null, null);

        // ASSERT
        List<Submodel> transformedSubmodels = transform.getSubmodels();
        Assertions.assertEquals(3, transformedSubmodels.size());
        Assertions.assertNull(transformedSubmodels.get(0).getIdentification());
        List<SubmodelElement> submodelElements = transformedSubmodels.get(0).getSubmodelElements();
        Assertions.assertEquals(3, submodelElements.size());
        Assertions.assertEquals(2, submodelElements.stream().filter(File.class::isInstance).count());
        Assertions.assertEquals(1,
            submodelElements.stream().filter(SubmodelElementCollection.class::isInstance).count());
        SubmodelElementCollection SMEC = (SubmodelElementCollection) submodelElements.stream()
            .filter(SubmodelElementCollection.class::isInstance).findFirst().get();
        Assertions.assertEquals(3,
            SMEC.getValues().stream().filter(io.adminshell.aas.v3.model.Property.class::isInstance).count());
        Assertions.assertEquals(0, SMEC.getValues().stream()
            .filter(io.adminshell.aas.v3.model.MultiLanguageProperty.class::isInstance).count());
    }

    @Test
    void testForeachEvaluationsTargetNotAList() throws IOException {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/foreachTest2.json");

        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, null, null);

        // ASSERT
        Submodel submodel = transform.getSubmodels().get(0);
        String singleIdentifier = submodel.getIdentification().getIdentifier();
        Assertions.assertEquals("https://test.org/transform_for_first_in_list", singleIdentifier);
    }

    @Test
    void testBindingsEvaluation() throws IOException {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/bindingsTest.json");

        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, null, null);

        // ASSERT
        Submodel submodel = transform.getSubmodels().get(0);
        String identifier = submodel.getIdentification().getIdentifier();
        Assertions.assertEquals("https://test.org/id_via_bind", identifier);

        String fileIdShort = submodel.getSubmodelElements().get(0).getIdShort();
        Assertions.assertEquals("idshort_via_bind", fileIdShort);

        Property prop = (Property) submodel.getSubmodelElements().get(1);
        KeyElements type = prop.getValueId().getKeys().get(0).getType();
        Assertions.assertEquals(KeyElements.GLOBAL_REFERENCE, type);
    }

    @Test
    void testBindingsEvaluationWithInvalidBindings() throws IOException {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/bindingsTest_w_errors.json");

        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, null, null);

        // ASSERT
        Submodel submodel = transform.getSubmodels().get(0);
        Property prop = (Property) submodel.getSubmodelElements().get(1);
        KeyElements type = prop.getValueId().getKeys().get(0).getType();
        Assertions.assertNull(type);
    }

    @Test
    void testXpathTransformation() throws Exception {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/genericXpathTest.json");

        Document testDoc = null;
        try (InputStream testResource = Files
            .newInputStream(Paths.get("src/test/resources/mappings/generic/generic.xml"))) {

            // ACT
            AssetAdministrationShellEnvironment transform = new GenericDocumentTransformer().execute(testResource,
                mapSpec);
            // ASSERT
            Assertions.assertEquals(2, transform.getSubmodels().size());
        }

    }

    @Test
    void testVarsEvaluation() throws IOException, SerializationException {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/variablesTest.json");

        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, null, null);

        // ASSERT
        Submodel submodel = transform.getSubmodels().get(0);
        String idShort = submodel.getIdShort();
        Assertions.assertEquals("myvarforidshort", idShort);

        Property property1 = (Property) transform.getSubmodels().get(0).getSubmodelElements().get(0);
        Assertions.assertEquals("myoverriddenvarforidshort", property1.getIdShort());
        Assertions.assertEquals("myvarforlateruse", property1.getValue());
        Property property2 = (Property) transform.getSubmodels().get(0).getSubmodelElements().get(1);
        Assertions.assertEquals("myoverriddenvarforidshort", property2.getIdShort());
        Assertions.assertEquals("myvarforlateruse", property2.getValue());
        Property property3 = (Property) transform.getSubmodels().get(0).getSubmodelElements().get(2);
        Assertions.assertEquals("myoverriddenvarforidshort", property3.getIdShort());
        Assertions.assertEquals("myvarforlateruse", property3.getValue());
    }

    @Test
    void testDefinitionsVsVariablesTransformation() throws Exception {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/example_definitions_vs_vars.json");

        Document testDoc = null;
        try (InputStream testResource = Files
            .newInputStream(Paths.get("src/test/resources/mappings/generic/generic.xml"))) {

            SAXReader reader = new SAXReader();
            reader.setEncoding("UTF-8");
            reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            testDoc = reader.read(testResource);
        }

        XPathHelper.getInstance().setNamespaceBinding("ns", "http://ns.org/");
        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, testDoc, null);

        // ASSERT
        Submodel submodel = transform.getSubmodels().get(0);
        Assertions.assertNotNull(submodel);
        // context is same, var and def evaluates as same
        Assertions.assertEquals("a", submodel.getIdShort());// id => var
        Assertions.assertEquals("a", submodel.getCategory());// category => def

        // context now differs, var (transformed to idShort) is static and def
        // reevaluates again, now to "b" since the context changed
        SubmodelElement submodelElement = submodel.getSubmodelElements().get(0);
        Assertions.assertEquals("a", submodelElement.getIdShort());// id => var
        Assertions.assertEquals("b", submodelElement.getCategory());// category => def

    }

    @Test
    void testBindingsNonStringValues() throws Exception {
        // ARRANGE
        MappingSpecification mapSpec = parser
            .loadMappingSpecification("src/test/resources/mappings/generic/bindingsTest_nonStrings.json");

        // ACT
        AssetAdministrationShellEnvironment transform = aasMappingTransformer.transform(mapSpec, null, null);

        // ASSERT
        Submodel submodel = transform.getSubmodels().get(0);
        SubmodelElementCollection smc = (SubmodelElementCollection) submodel.getSubmodelElements().get(0);
        Assertions.assertTrue(smc.getOrdered());
        Assertions.assertTrue(smc.getAllowDuplicates());

    }

}
