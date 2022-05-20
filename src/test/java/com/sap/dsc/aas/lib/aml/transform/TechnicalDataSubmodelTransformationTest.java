/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.transform;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;
import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.mapping.MappingSpecificationParser;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;

import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.dataformat.Serializer;
import io.adminshell.aas.v3.dataformat.json.JsonSchemaValidator;
import io.adminshell.aas.v3.dataformat.json.JsonSerializer;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

public class TechnicalDataSubmodelTransformationTest {

    public static final String TECHNICAL_DATA_CONFIG_JSON = "src/test/resources/config/technicalData/technicalDataConfig.json";
    public static final String TECHNICAL_DATA_CONFIG_MISSING_MANUFACTURER_NAME =
        "src/test/resources/config/technicalData/technicalDataConfigMissingManufacturerName.json";
    public static final String TECHNICAL_DATA_CONFIG_MISSING_IDENTIFICATION_DATA =
        "src/test/resources/config/technicalData/technicalDataConfigMissingIdentificationData.json";
    public static final String AML_INPUT = "src/test/resources/aml/full_AutomationComponent.aml";
    public static final String JSON_SCHEMA_TECHNICAL_DATA = "src/test/resources/schema/schema_technicaldata_v02.json";

    private static AssetAdministrationShellEnvironment shellEnv;
    private static Serializer serializer;
    private static JsonSchemaValidator validator;

    @BeforeEach
    protected void setUp() throws Exception {
        TestUtils.resetBindings();
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        AmlTransformer amlTransformer = new AmlTransformer();

        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(TECHNICAL_DATA_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);
        validator = new JsonSchemaValidator();
        serializer = new JsonSerializer();

    }

    @Test
    void validateTransformedAgainstAASJSONSchema() throws SerializationException, TransformationException {

        String serializedShellEnv = serializer.write(shellEnv);

        Set<String> errors = validator.validateSchema(serializedShellEnv);
        errors.stream().forEach(System.out::print);

        assertThat(errors.size()).isEqualTo(0);
    }

    @Test
    void validateTransformedAgainstTechnicalDataJSONSchema() throws IOException, SerializationException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_TECHNICAL_DATA)));
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode);
        // we need to get the submodel only for the comparison
        String write = serializer.write(shellEnv);
        System.out.println(write);
        JsonNode jsonNode = mapper.readTree(write).get("submodels").get(0);

        Set<ValidationMessage> errors = schema.validate(jsonNode);
        if (errors.size() != 0) {
            errors.forEach((error) -> System.out.println(error));
        }
        assertEquals(0, errors.size());
    }

    /*
     * this test should fail when the config file does not specify a ManufacturerName SubmodelElement
     * that is required by the TechnicalData Schema
     */

    @Test
    void failsWhenManufacturerNameElementNotPresent() throws IOException, TransformationException, SerializationException {
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        AmlTransformer amlTransformer = new AmlTransformer();

        MappingSpecification mapping =
            new MappingSpecificationParser().loadMappingSpecification(TECHNICAL_DATA_CONFIG_MISSING_MANUFACTURER_NAME);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        ObjectMapper mapper = new ObjectMapper();

        SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
        // When set to true, the validation process stops immediately when the first error occurs.
        schemaValidatorsConfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_TECHNICAL_DATA)));
        JsonSchema schema =
            JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfig);

        // we need to get the submodel only for the comparison
        JsonNode jsonNode = mapper.readTree(serializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            // check that it fails indeed due to the missing ManufacturerName Element
            assertTrue(e.getMessage().contains("https://admin-shell.io/ZVEI/TechnicalData/ManufacturerName/1/1"));
        }
    }

    /*
     * this test should fail when the config file does not specify a ManufacturerName SubmodelElement
     * that is required by the TechnicalData Schema
     */
    @Test
    void failsWhenIndentificationDataSubmodelElementNotPresent() throws IOException, TransformationException, SerializationException {
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        AmlTransformer amlTransformer = new AmlTransformer();

        MappingSpecification mapping =
            new MappingSpecificationParser().loadMappingSpecification(TECHNICAL_DATA_CONFIG_MISSING_IDENTIFICATION_DATA);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        ObjectMapper mapper = new ObjectMapper();

        SchemaValidatorsConfig schemaValidatorsConfigconfig = new SchemaValidatorsConfig();
        // When set to true, the validation process stops immediately when the first error occurs.
        schemaValidatorsConfigconfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_TECHNICAL_DATA)));
        JsonSchema schema =
            JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfigconfig);
        // we need to get the submodel only for the comparison
        JsonNode jsonNode = mapper.readTree(serializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            // check that it fails indeed due to the missing Identification Element
            assertTrue(e.getMessage().contains("https://admin-shell.io/ZVEI/TechnicalData/GeneralInformation/1/1"));
        }
    }

}
