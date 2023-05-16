/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.eclipse.digitaltwin.aas4j.aml.helper.AmlxPackageCreator;

import io.adminshell.aas.v3.dataformat.json.JsonDeserializer;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.Property;
import io.adminshell.aas.v3.model.Submodel;

public class ConsoleApplicationTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private final String HELP_CONTENT = "usage: transform";
    // private final String CONFIG_FILE_PATH = "src/test/resources/config/simpleConfig.json";
    private final String CONFIG_FILE_PATH = "src/test/resources/mappings/generic/genericXpathTest.json";
    private final String EMPTY_CONFIG_FILE_PATH = "src/test/resources/config/emptyConfig.json";
    private final String AML_FILE_PATH = "src/test/resources/aml/full_AutomationComponent.aml";
    private final String GENERIC_FILE_PATH = "src/test/resources/mappings/generic/generic.xml";

    private ConsoleApplication classUnderTest;

    /**
     * Capture system.out in a stream
     * https://stackoverflow.com/questions/1119385/junit-test-for-system-out-println
     *
     * @throws Exception
     */
    @BeforeEach
    public void setUpStreams() throws Exception {
        TestUtils.resetBindings();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        classUnderTest = new ConsoleApplication(Mockito.mock(CommandLine.class));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String getPrintedOut() {
        return outContent.toString();
    }

    private String getPrintedError() {
        return errContent.toString();
    }

    private String getPrinted() {
        return getPrintedOut() + getPrintedError();
    }

    @Test
    void noArgs() {
        ConsoleApplication.main(new String[] {});
        assertThat(getPrintedOut()).contains(HELP_CONTENT);
    }

    @Test
    void invalidArg() {
        ConsoleApplication.main(new String[] {"--invalidOption"});
        assertThat(getPrintedOut()).contains(HELP_CONTENT);
    }

    @Test
    void printHelp() {
        ConsoleApplication.main(new String[] {"-h"});
        assertThat(getPrintedOut()).contains(HELP_CONTENT);

        ConsoleApplication.main(new String[] {"-h", "-c", CONFIG_FILE_PATH});
        assertThat(getPrintedOut()).contains(HELP_CONTENT);
    }

    @Test
    void missingConfig() {
        ConsoleApplication.main(new String[] {"-c", "src/test/resources/config/nonExistentConfig.json",
            "-a", AML_FILE_PATH});
        assertThat(getPrinted()).contains("FileNotFoundException");
    }

    @Test
    void validConfig() throws IOException {
        classUnderTest.loadConfig(CONFIG_FILE_PATH);
        assertThat(classUnderTest.mapping).isNotNull();
    }

    @Test
    void validConfigAndAml() {
        ConsoleApplication.main(new String[] {"-c", CONFIG_FILE_PATH,
            "-a", AML_FILE_PATH});
        assertThat(getPrinted()).contains("Loaded config version");
        assertThat(getPrinted()).contains("Wrote AAS file");

        String outputFileName = classUnderTest.deriveOutputFileName(AML_FILE_PATH);
        File outputFile = Paths.get(outputFileName).toFile();

        assertThat(outputFile.exists()).isTrue();
        assertThat(outputFile.delete()).isTrue();
    }

    @Test
    void emptyConfig() {
        ConsoleApplication.main(new String[] {"-c", EMPTY_CONFIG_FILE_PATH,
            "-a", AML_FILE_PATH});
        assertThat(getPrinted()).contains("Loaded config version null");
        assertThat(getPrinted()).contains("Wrote AAS file");

        String outputFileName = classUnderTest.deriveOutputFileName(AML_FILE_PATH);
        File outputFile = Paths.get(outputFileName).toFile();

        assertTrue(outputFile.exists());
        assertThat(outputFile.delete()).isTrue();
    }

    @Test
    void getPlaceholderList() {
        ConsoleApplication.main(new String[] {"-p", "-c", "src/test/resources/config/minimal_placeholder.json"});
        assertThat(getPrinted()).contains("2 placeholders are expected");
        assertThat(getPrinted()).contains("assetName: Name of the asset");
        assertThat(getPrinted()).contains("submodelName: Name of the submodel");
    }

    @Test
    void getSinglePlaceholderList() {
        ConsoleApplication.main(new String[] {"-c", CONFIG_FILE_PATH, "--print-placeholders"});
        assertThat(getPrinted()).contains("1 placeholders");
        assertThat(getPrinted()).contains("genericPlaceholder: A generic placeholder");
    }

    @Test
    void replacePlaceholdersGeneric() throws Exception {
        File genericInputFile = new File(GENERIC_FILE_PATH);
        String genericInputFileDir = genericInputFile.getAbsolutePath();
        String expectedValue = "expectedValue";
        String placeholderMap = "{\"genericPlaceholder\":\"" + expectedValue + "\"}";

        ConsoleApplication.main(new String[] {"-c", CONFIG_FILE_PATH, "-xml", genericInputFileDir, "-p", "-P", placeholderMap});

        String outputFileName = classUnderTest.deriveOutputFileName(genericInputFileDir);
        Path outputFilePath = Paths.get(outputFileName);
        File outputFile = outputFilePath.toFile();
        String outputJson = Files.readString(outputFilePath);
        AssetAdministrationShellEnvironment assetShellEnv = new JsonDeserializer().read(outputJson);
        Submodel firstSubmodel = assetShellEnv.getSubmodels().get(0);
        Property firstProperty = (Property) firstSubmodel.getSubmodelElements().get(0);
        assertThat(firstProperty.getValue()).isEqualTo(expectedValue);
        assertThat(outputFile.delete()).isTrue();
    }

    @Test
    void missingPlaceholders() throws Exception {
        File genericInputFile = new File(GENERIC_FILE_PATH);
        String genericInputFileDir = genericInputFile.getAbsolutePath();
        String expectedValue = "expectedValue";
        String placeholderMap = "{\"notTheExpectedPlaceholder\":\"" + expectedValue + "\"}";

        ConsoleApplication.main(new String[] {"-c", CONFIG_FILE_PATH, "-xml", genericInputFileDir, "-p", "-P", placeholderMap});

        assertThat(getPrinted()).contains("No value for placeholder");
    }

    @Test
    void validConfigAndAmlx() throws Exception {
        String amlxDir = "src/test/resources/amlx/minimal_AutomationMLComponent_WithDocuments";
        File amlxFile = AmlxPackageCreator.compressFolder(amlxDir);

        ConsoleApplication.main(new String[] {"-c", CONFIG_FILE_PATH, "-amlx", amlxFile.getAbsolutePath()});
        assertThat(getPrinted()).contains("Loaded config version");
        assertThat(getPrinted()).contains("Wrote AAS file");

        String outputFileName = classUnderTest.deriveOutputFileName(amlxDir);

        List<File> outputFiles = Arrays.asList(amlxFile,
            Paths.get(outputFileName).toFile(),
            Paths.get("minimal_AutomationMLComponent_WithDocuments/files/TestPDFDeviceManual.pdf").toFile(),
            Paths.get("minimal_AutomationMLComponent_WithDocuments/files/TestTXTDeviceManual.txt").toFile(),
            Paths.get("minimal_AutomationMLComponent_WithDocuments/files/TestTXTWarranty.txt").toFile());

        outputFiles.stream()
            .peek(file -> assertThat(file.exists()).isTrue())
            .forEach(file -> assertThat(file.delete()).isTrue());

        // Delete created directories
        Files.delete(Paths.get("minimal_AutomationMLComponent_WithDocuments/files/"));
        Files.delete(Paths.get("minimal_AutomationMLComponent_WithDocuments/"));
    }

    @Test
    void validConfigAndGeneric() throws Exception {
        File genericFile = new File(GENERIC_FILE_PATH);
        String genericFileDir = genericFile.getAbsolutePath();
        String outputFileName = classUnderTest.deriveOutputFileName(genericFileDir);
        File outputFile = new File(outputFileName);

        ConsoleApplication.main(new String[] {"-c", CONFIG_FILE_PATH, "-xml", genericFileDir});

        assertThat(getPrinted()).contains("Loaded config version");
        assertThat(getPrinted()).contains("Wrote AAS file");
        assertTrue(outputFile.delete());;
    }
}
