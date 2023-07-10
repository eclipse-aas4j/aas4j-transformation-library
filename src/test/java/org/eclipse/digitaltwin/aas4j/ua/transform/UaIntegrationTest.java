package org.eclipse.digitaltwin.aas4j.ua.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.exceptions.UnableToReadXmlException;
import org.eclipse.digitaltwin.aas4j.mapping.MappingSpecificationParser;
import org.eclipse.digitaltwin.aas4j.mapping.model.MappingSpecification;
import io.adminshell.aas.v3.dataformat.json.JsonSerializer;
import io.adminshell.aas.v3.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class UaIntegrationTest {

    public static final String MACHINE_TOOL_CONIFG = "src/test/resources/ua/machineToolToDomainModel.json";
    public static final String NAMEPLATE_CONFIG = "src/test/resources/ua/diToNameplate.json";
    public static final String INTEGRATION_CONFIG = "src/test/resources/ua/uaIntegrationTest.json";
    public static final String UA_MACHINE_TOOL = "src/test/resources/ua/machineTool-example-all-in-one.xml";
    public static final String UA_BIG_MACHINE = "src/test/resources/ua/big.machine.nodeset.xml";
    public static final String NOT_A_NODESET = "src/test/resources/aml/full_AutomationComponent.aml";
    public static final String JSON_SCHEMA_NAMEPLATE = "src/test/resources/schema/schema_nameplate.json";
    private static AssetAdministrationShellEnvironment shellEnv;
    private ObjectMapper mapper;

    @BeforeEach
    protected void setUp() throws Exception {
        TestUtils.resetBindings();
        mapper = new ObjectMapper();
    }

    @Test
    void testIntegration() throws Exception {
        InputStream uaInputStream = Files.newInputStream(Paths.get(UA_BIG_MACHINE));
        UANodeSetTransformer uaTransformer = new UANodeSetTransformer();
        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(INTEGRATION_CONFIG);
        shellEnv = uaTransformer.execute(uaInputStream, mapping);
        boolean idInEnv = shellEnv.getSubmodels().stream().map(s -> s.getIdentification().getIdentifier())
                .collect(Collectors.toList()).contains("http://exp.organization.com/UA/BigMachine/ns=4;i=1281");
        assertTrue(idInEnv);

        assertEquals("407ef772-7040-3b9e-b51a-80286d1c8e49", shellEnv.getAssetAdministrationShells().get(0).getIdentification().getIdentifier());

        //this value was extracted from the first run. Since it should change
        assertNotEquals("6b6dcb8f-352c-49f7-8267-e2586601e858", shellEnv.getAssetAdministrationShells().get(0).getIdShort());

    }

    @Test
    void testUaDiNameplate() throws Exception {
        InputStream uaInputStream = Files.newInputStream(Paths.get(UA_BIG_MACHINE));
        UANodeSetTransformer uaTransformer = new UANodeSetTransformer();
        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(NAMEPLATE_CONFIG);
        shellEnv = uaTransformer.execute(uaInputStream, mapping);
        Submodel np = getSubmodel("Nameplate");
        assertEquals(5, np.getSubmodelElements().size());
        SubmodelElementCollection address = (SubmodelElementCollection) getElement("Address", np);
        address.getValues().stream().map(Referable::getIdShort)
                .collect(Collectors.toList()).forEach(Assertions::assertNotNull);
        assertTrue(shellEnv.getAssetAdministrationShells().size() > 0);
        shellEnv.getAssetAdministrationShells().get(0).getSubmodels().forEach(sm ->
                assertTrue(sm.getKeys().size() > 0));
        JsonSerializer jsonSerializer = new JsonSerializer();
        SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
        schemaValidatorsConfig.setFailFast(true);
        JsonNode schemaNode = mapper.readTree(Files.newInputStream(Paths.get(JSON_SCHEMA_NAMEPLATE)));
        JsonSchema schema =
                JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaNode)).getSchema(schemaNode, schemaValidatorsConfig);
        JsonNode jsonNode = mapper.readTree(jsonSerializer.write(shellEnv)).get("submodels").get(0);

        try {
            schema.validate(jsonNode);
            fail("JsonSchemaException must be thrown");
        } catch (JsonSchemaException e) {
            final Set<ValidationMessage> messages = e.getValidationMessages();
            messages.stream().forEach(message -> System.out.println(message.getMessage()));
            assertThat(messages.size()).isEqualTo(0);
        }
    }

    @Test
    void testMachineTool() throws Exception {
        InputStream uaInputStream = Files.newInputStream(Paths.get(UA_MACHINE_TOOL));
        UANodeSetTransformer uaTransformer = new UANodeSetTransformer();
        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(MACHINE_TOOL_CONIFG);
        shellEnv = uaTransformer.execute(uaInputStream, mapping);
        assertEquals(1, shellEnv.getSubmodels().size());
        Submodel machineTool = getSubmodel("SampleMachineTool");
        assertEquals(2, machineTool.getSubmodelElements().size());
        String operationMode = ((Property) getElement("OperationMode", machineTool)).getValue();
        assertEquals("1", operationMode);
        assertEquals(2, ((SubmodelElementCollection) getElement("Production", machineTool)).getValues().size());
    }

    @Test
    void testInvalidInput() throws Exception {
        InputStream invalidInputStream = Files.newInputStream(Paths.get(NOT_A_NODESET));
        UANodeSetTransformer uaTransformer = new UANodeSetTransformer();
        MappingSpecification mapping = new MappingSpecificationParser().loadMappingSpecification(NAMEPLATE_CONFIG);
        assertThrows(UnableToReadXmlException.class, () -> uaTransformer.execute(invalidInputStream, mapping));
    }


    SubmodelElement getElement(String idShort, Submodel submodel) {
        assertThat(submodel.getSubmodelElements()).isNotNull();

        return submodel.getSubmodelElements().stream()
                .filter(submodelElement -> submodelElement.getIdShort().equals(idShort))
                .findFirst()
                .orElseThrow(() -> new AssertionFailedError("SubmodelElement with IdShort '" + idShort + "' not found"));
    }

    Submodel getSubmodel(String idShort) {
        assertNotNull(shellEnv.getSubmodels());

        return shellEnv.getSubmodels().stream()
                .filter(submodel -> submodel.getIdShort().equals(idShort))
                .findFirst()
                .orElseThrow(() -> new AssertionFailedError("Submodel with IdShort '" + idShort + "' not found"));
    }
}