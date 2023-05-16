/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.amlx;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.aml.helper.AmlxPackageCreator;

public class AmlxPackageTest {

    private AmlxPackage amlxPackage;

    @BeforeEach
    void setup() throws Exception {
        TestUtils.resetBindings();
        amlxPackage = getAmlxPackage(AmlxPackageCreator.PATH_TO_MINIMAL_AMLX_DIR);
    }

    private AmlxPackage getAmlxPackage(String amlxDirPath) throws IOException, InvalidFormatException {
        File amlxFile = AmlxPackageCreator.compressFolder(amlxDirPath);
        OPCPackage opcPackage = OPCPackage.open(amlxFile);
        return new AmlxPackage(opcPackage);
    }

    @AfterEach
    void teardown() throws IOException {
        Files.deleteIfExists(Paths.get(AmlxPackageCreator.PATH_TO_MINIMAL_AMLX));
    }

    @Test
    void getRootAmlFile() {
        AmlxPackagePart rootAml = amlxPackage.getRootAmlFile();
        assertThat(rootAml).isNotNull();
        assertThat(rootAml.getPathInAmlx()).isEqualTo("/minimal_AutomationMLComponent.aml");
    }

    private static Stream<Arguments> getListOfNonAmlFilesValues() {
        return Stream.of(
            arguments("No documents", AmlxPackageCreator.PATH_TO_MINIMAL_AMLX_DIR, new ArrayList<String>()),
            arguments("AMLX with documents", "src/test/resources/amlx/minimal_AutomationMLComponent_WithDocuments",
                Arrays.asList("/files/TestPDFDeviceManual.pdf",
                    "/files/TestTXTDeviceManual.txt", "/files/TestTXTWarranty.txt")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getListOfNonAmlFilesValues")
    @DisplayName("Get multiple string values based on a given XPath")
    void isRelationshipTypeAml(String name, String pathToAmlxDir, List<String> expectedDocumentPaths) throws Exception {
        AmlxPackage amlxPackage = getAmlxPackage(pathToAmlxDir);
        List<AmlxPackagePart> nonAmlFiles = amlxPackage.getNonAmlFiles();
        assertThat(nonAmlFiles).hasSize(expectedDocumentPaths.size());

        List<String> nonAmlFilePaths = nonAmlFiles.stream()
            .map(part -> part.getPathInAmlx()).collect(Collectors.toList());
        assertThat(nonAmlFilePaths).containsExactlyElementsIn(expectedDocumentPaths);
    }

    private AmlxPackage getAmlxPackageWithMockedOpc() throws Exception {
        OPCPackage mockOpcPackage = Mockito.mock(OPCPackage.class);
        PackageRelationship mockRelationship = Mockito.mock(PackageRelationship.class);
        when(mockRelationship.getId()).thenReturn("mockRelationshipId");
        when(mockRelationship.getRelationshipType()).thenReturn("mockRelationshipType");
        when(mockRelationship.getTargetURI()).thenReturn(URI.create("http://sap.com"));

        PackageRelationshipCollection collection = new PackageRelationshipCollection(mockOpcPackage);
        collection.addRelationship(mockRelationship);
        when(mockOpcPackage.getRelationships()).thenReturn(collection);

        return new AmlxPackage(mockOpcPackage);
    }

    @Test
    void getPartByRelationshipWithException() throws Exception {
        AmlxPackage amlxPackage = getAmlxPackageWithMockedOpc();
        when(amlxPackage.getOpcPackage().getParts()).thenThrow(new InvalidFormatException("Thrown during unit test"));

        PackageRelationship mockRelationship = Mockito.mock(PackageRelationship.class);
        when(mockRelationship.getTargetURI()).thenReturn(URI.create("http://sap.com"));

        assertThrows(IllegalStateException.class, () -> amlxPackage.getAmlxPackagePartByRelationship(mockRelationship));
    }

    @Test
    void loadNonAmlFilesWithException() throws Exception {
        AmlxPackage amlxPackage = getAmlxPackageWithMockedOpc();
        when(amlxPackage.getOpcPackage().getParts()).thenThrow(new InvalidFormatException("Thrown during unit test"));

        assertThrows(InvalidFormatException.class, () -> amlxPackage.loadNonAmlFiles());
    }

}
