/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.transform;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.mapping.MappingSpecificationParser;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;

import io.adminshell.aas.v3.dataformat.DeserializationException;
import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.dataformat.Serializer;
import io.adminshell.aas.v3.dataformat.json.JsonSchemaValidator;
import io.adminshell.aas.v3.dataformat.json.JsonSerializer;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.Property;

public class AINSubmodelTransformationTest {

    public static final String AIN_SUBMODEL_CONFIG_JSON = "src/test/resources/config/AIN_submodel/ain_config.json";
    public static final String AML_INPUT = "src/test/resources/aml/full_AutomationComponent.aml";

    private static AssetAdministrationShellEnvironment shellEnv;
    private static JsonSchemaValidator validator;
    private static Serializer serializer;
    private AmlTransformer amlTransformer;
    private MappingSpecificationParser mappingParser;
    private InputStream amlInputStream;
    private String SAMPLE_MANUFACTURER_ID = "sampleReplacedManufacturerId";

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
    @DisplayName("Transform Custom AIN Submodel and replace the placeholders, then validate result with AAS Json Schema")
    void validateTransformedAINSubmodelAgainstAASJSONSchema()
        throws IOException, TransformationException, SerializationException, DeserializationException {

        MappingSpecification mapping = mappingParser.loadMappingSpecification(AIN_SUBMODEL_CONFIG_JSON);
        // replace the placeholder assigning a sample value
        Map<String, String> placeholderValues = new HashMap<>();
        placeholderValues.put("manufacturerId", SAMPLE_MANUFACTURER_ID);

        shellEnv = amlTransformer.execute(amlInputStream, mapping, placeholderValues);

        String serializedShellEnv = serializer.write(shellEnv);

        Set<String> errors = validator.validateSchema(serializedShellEnv);
        errors.stream().forEach(System.out::print);
        assertThat(errors.size()).isEqualTo(0);

        Property submodelElement = (Property) shellEnv.getSubmodels().stream()
            .filter(submodel -> submodel.getIdShort().equals("AIN_MEI_Tranformation_Submodel")).findFirst().get().getSubmodelElements()
            .get(0);

        assertThat(submodelElement).isNotNull();
        assertThat(submodelElement.getValue()).isEqualTo(SAMPLE_MANUFACTURER_ID);

    }

}
