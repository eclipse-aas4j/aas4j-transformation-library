/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.amlx;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.aml.helper.AmlxPackageCreator;

public class AmlxPackagePartTest {

    public static final String PATH_TO_MINIMAL_AMLX_DIR = "src/test/resources/amlx/minimal_AutomationMLComponent_WithDocuments";

    private OPCPackage opcPackage;

    @BeforeEach
    void setup() throws Exception {
        TestUtils.resetBindings();
        opcPackage = getOpcPackage(PATH_TO_MINIMAL_AMLX_DIR);
    }

    private OPCPackage getOpcPackage(String amlxDirPath) throws IOException, InvalidFormatException {
        File amlxFile = AmlxPackageCreator.compressFolder(amlxDirPath);
        return OPCPackage.open(amlxFile);
    }

    @AfterEach
    void teardown() throws IOException {
        Files.deleteIfExists(Paths.get(PATH_TO_MINIMAL_AMLX_DIR + ".amlx"));
    }

    private static Stream<Arguments> fromPackagePartInput() {
        return Stream.of(
            arguments("Sample file 1", "/files/TestPDFDeviceManual.pdf", "application/pdf"),
            arguments("Sample file 2", "/CAEX_ClassModel_V.3.0.xsd", "text/xml"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fromPackagePartInput")
    @DisplayName("Get multiple string values based on a given XPath")
    void fromPackagePart(String name, String pathToFile, String contentType) throws Exception {
        List<PackagePart> parts = opcPackage.getPartsByName(Pattern.compile(pathToFile));

        assertThat(parts).hasSize(1);

        AmlxPackagePart part = AmlxPackagePart.fromPackagePart(parts.get(0));

        assertThat(part.getFileName()).isEqualTo(pathToFile.split("/")[pathToFile.split("/").length - 1]);
        assertThat(part.getContentType()).isEqualTo(contentType);
        assertThat(part.getPackagePart()).isNotNull();
        assertThat(part.getPathInAmlx()).isEqualTo(pathToFile);
    }

}
