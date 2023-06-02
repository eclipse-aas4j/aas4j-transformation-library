/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.transform;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.eclipse.digitaltwin.aas4j.mapping.MappingSpecificationParser;
import org.eclipse.digitaltwin.aas4j.mapping.model.MappingSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.adminshell.aas.v3.dataformat.DeserializationException;
import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.dataformat.Serializer;
import io.adminshell.aas.v3.dataformat.json.JsonSchemaValidator;
import io.adminshell.aas.v3.dataformat.json.JsonSerializer;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.File;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.SubmodelElementCollection;

public class DocumentationSubmodelTransformationTest {

    public static final String DOCU_SUBMODEL_CONFIG_JSON = "src/test/resources/config/documentation/documentation_config.json";
    public static final String AML_INPUT =
        "src/test/resources/amlx/minimal_AutomationMLComponent_WithDocuments/minimal_AutomationMLComponent_WithDocuments.aml";
    private static AssetAdministrationShellEnvironment shellEnv;
    private static JsonSchemaValidator validator;
    private static Serializer serializer;
    private AmlTransformer amlTransformer;
    private MappingSpecificationParser mappingParser;
    private InputStream amlInputStream;

    @BeforeEach
    protected void setUp() throws Exception {
        TestUtils.resetBindings();
        amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));
        amlTransformer = new AmlTransformer();
        mappingParser = new MappingSpecificationParser();
        validator = new JsonSchemaValidator();
        serializer = new JsonSerializer();
    }

    @Test
    @DisplayName("Transform Documentation Submodel, then validate result with AAS Json Schema")
    void validateTransformedAINSubmodelAgainstAASJSONSchema()
        throws IOException, TransformationException, SerializationException, DeserializationException {

        MappingSpecification mapping = mappingParser.loadMappingSpecification(DOCU_SUBMODEL_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);
        String serializedShellEnv = serializer.write(shellEnv);
        Set<String> errors = validator.validateSchema(serializedShellEnv);
        errors.stream().forEach(System.out::print);
        assertThat(errors.size()).isEqualTo(0);

    }

    @Test
    @DisplayName("validate that the new submodel is referenced in the AAShell")
    void validateReferencedSubmodel()
        throws IOException, TransformationException {

        MappingSpecification mapping = mappingParser.loadMappingSpecification(DOCU_SUBMODEL_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);
        String reference = shellEnv.getAssetAdministrationShells().get(0).getSubmodels().get(0).getKeys().get(0).getValue();
        assertThat(shellEnv.getSubmodels().get(0).getIdentification().getIdentifier()).isEqualTo(reference);
    }

    @Test
    @DisplayName("Transform Documentation Submodel, then validate that the result contains required elements")
    void validateTransformedAINSubmodelContainedElements()
        throws IOException, TransformationException {

        MappingSpecification mapping = mappingParser.loadMappingSpecification(DOCU_SUBMODEL_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        // test that the submodel with the semanticId for documentation is there
        Submodel documentationSubmodel = shellEnv.getSubmodels().stream()
            .filter(submodel -> submodel.getSemanticId().getKeys().get(0).getValue().equals("https://sap.com/ain/documentation"))
            .findFirst().orElseThrow();
        assertThat(documentationSubmodel).isNotNull();

        // there should be 3 SubmodelElement collections in the Submodel, one for each document
        assertThat(documentationSubmodel.getSubmodelElements().size()).isEqualTo(3);

        SubmodelElementCollection firstCollection = (SubmodelElementCollection) documentationSubmodel.getSubmodelElements().get(0);

        // Each collection should contain at least 6 Elements (DocumentId, File, LifecyclePhase Properties
        // and 2 collections)
        assertThat(firstCollection.getValues().size()).isGreaterThan(5);

        // check that at least one file element is included in the submodel
        File fileElement = (File) firstCollection.getValues().stream().filter(SubmodelElement -> SubmodelElement.getSemanticId().getKeys()
            .get(0).getValue().equals("http://www.sap.com/ain/documentation/document/file/@ID")).findFirst().get();
        assertThat(fileElement).isNotNull();
        assertThat(fileElement.getValue()).isEqualTo("/files/TestTXTDeviceManual.txt");
        assertThat(fileElement.getMimeType()).isEqualTo("plain/text");
    }

}
