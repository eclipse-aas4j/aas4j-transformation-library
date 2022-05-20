/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.transform;

import static com.google.common.truth.Truth.assertThat;
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

public class NameplateSubmodelTransformationTest {

    public static final String NAMEPLATE_CONFIG_JSON = "src/test/resources/config/nameplate/nameplateConfig.json";
    public static final String SIMPLE_CONFIG_JSON = "src/test/resources/config/simpleConfig.json";
    public static final String NAMEPLATE_CONFIG_MISSING_ADDRESS = "src/test/resources/config/nameplate/nameplateConfigMissingAddress.json";
    public static final String NAMEPLATE_CONFIG_WRONG_ADDRESS_WRONG_SEMANTICID =
        "src/test/resources/config/nameplate/nameplateConfigWrongAddressSemanticId.json";
    public static final String NAMEPLATE_CONFIG_MISSING_MANUFACTURERPRODUCTDESIGNATION =
        "src/test/resources/config/nameplate/nameplateConfigMissingManufacturerProductDesignation.json";
    public static final String AML_INPUT = "src/test/resources/aml/full_AutomationComponent.aml";
    public static final String JSON_SCHEMA_NAMEPLATE = "src/test/resources/schema/schema_nameplate.json";

    private static AssetAdministrationShellEnvironment shellEnv;
    private static JsonSchemaValidator validator;
    private static Serializer serializer;
    private AmlTransformer amlTransformer;
    private MappingSpecificationParser mappingParser;
    private ObjectMapper mapper;
    private InputStream amlInputStream;

    @BeforeEach
    protected void setUp() throws Exception {
        TestUtils.resetBindings();
        amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));
        mapper = new ObjectMapper();

        amlTransformer = new AmlTransformer();
        mappingParser = new MappingSpecificationParser();
        validator = new JsonSchemaValidator();
        serializer = new JsonSerializer();
    }

    @Test
    void validateTransformedNamePlateAgainstAASJSONSchema() throws IOException, TransformationException, SerializationException {

        MappingSpecification mapping = mappingParser.loadMappingSpecification(NAMEPLATE_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        String serializedShellEnv = serializer.write(shellEnv);

        Set<String> errors = validator.validateSchema(serializedShellEnv);
        errors.stream().forEach(System.out::print);

        assertThat(errors.size()).isEqualTo(0);

    }

    @Test
    void validateTransformedGenericAgainstAASJSONSchema() throws IOException, TransformationException, SerializationException {

        MappingSpecification mapping = mappingParser.loadMappingSpecification(SIMPLE_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        Serializer serializer = new JsonSerializer();
        String serializedShellEnv = serializer.write(shellEnv);

        Set<String> errors = validator.validateSchema(serializedShellEnv);
        errors.stream().forEach(System.out::println);
        assertThat(errors.size()).isEqualTo(0);

    }

    @Test
    void validatesCorrectNamePlateSubmodel() throws IOException, TransformationException, SerializationException {
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        MappingSpecification mapping = mappingParser.loadMappingSpecification(NAMEPLATE_CONFIG_JSON);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        SchemaValidatorsConfig schemaValidatorsConfigconfig = new SchemaValidatorsConfig();
        // When set to true, the validation process stops immediately when the first error occurs.
        schemaValidatorsConfigconfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_NAMEPLATE)));
        JsonSchema schema =
            JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfigconfig);

        // we need to get the submodel only for the comparison
        JsonNode jsonNode = mapper.readTree(serializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            final Set<ValidationMessage> messages = e.getValidationMessages();
            messages.stream().forEach(message -> System.out.println(message.getMessage()));
            assertThat(messages.size()).isEqualTo(0);
        }
    }

    /*
     * this test should fail when the config file does not specify an Address SubmodelElement that is
     * required by the Nameplate Schema
     */

    @Test
    void failsWhenAddressElementNotPresent() throws IOException, TransformationException, SerializationException {
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        MappingSpecification mapping = mappingParser.loadMappingSpecification(NAMEPLATE_CONFIG_MISSING_ADDRESS);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        SchemaValidatorsConfig schemaValidatorsConfigconfig = new SchemaValidatorsConfig();
        // When set to true, the validation process stops immediately when the first error occurs.
        schemaValidatorsConfigconfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_NAMEPLATE)));
        JsonSchema schema =
            JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfigconfig);

        // we need to get the submodel only for the comparison
        JsonNode jsonNode = mapper.readTree(serializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            final Set<ValidationMessage> messages = e.getValidationMessages();
            messages.stream().forEach(message -> System.out.println(message.getMessage()));
            assertThat(messages.size()).isEqualTo(1);
        }
    }

    @Test
    void failsAddressHasWrongSubmodelId() throws IOException, TransformationException, SerializationException {
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        MappingSpecification mapping = mappingParser.loadMappingSpecification(NAMEPLATE_CONFIG_WRONG_ADDRESS_WRONG_SEMANTICID);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        SchemaValidatorsConfig schemaValidatorsConfigconfig = new SchemaValidatorsConfig();
        // When set to true, the validation process stops immediately when the first error occurs.
        schemaValidatorsConfigconfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_NAMEPLATE)));
        JsonSchema schema =
            JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfigconfig);
        // we need to get the submodel only for the comparison
        JsonNode jsonNode = mapper.readTree(serializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            System.out.println(e);
            assertThat(e.getMessage().contains("must be a constant value 0173-1#02-AAQ832#005")).isTrue();
        }
    }

    /*
     * this test should fail when the config file does not specify an ManufacturerProductDesignation
     * SubmodelElement that is required by the Nameplate Schema
     */

    @Test
    void failsWhenManufacturerProductDesignationElementNotPresent() throws IOException, TransformationException, SerializationException {
        InputStream amlInputStream = Files.newInputStream(Paths.get(AML_INPUT));

        MappingSpecification mapping = mappingParser.loadMappingSpecification(NAMEPLATE_CONFIG_MISSING_MANUFACTURERPRODUCTDESIGNATION);
        shellEnv = amlTransformer.execute(amlInputStream, mapping);

        SchemaValidatorsConfig schemaValidatorsConfigconfig = new SchemaValidatorsConfig();
        // When set to true, the validation process stops immediately when the first error occurs.
        schemaValidatorsConfigconfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_NAMEPLATE)));
        JsonSchema schema =
            JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfigconfig);

        // we need to get the submodel only for the comparison
        JsonNode jsonNode = mapper.readTree(serializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            final Set<ValidationMessage> messages = e.getValidationMessages();
            assertThat(messages.size()).isEqualTo(1);
        }
    }

}
