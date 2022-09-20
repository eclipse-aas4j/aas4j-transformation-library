/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.amlx;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.aml.helper.AmlxPackageCreator;
import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.exceptions.ValidationException;

public class AmlxPackageReaderTest {

    private File amlxFile;

    @BeforeEach
    void setup() throws Exception {
        TestUtils.resetBindings();
        this.amlxFile = AmlxPackageCreator.compressFolder(AmlxPackageCreator.PATH_TO_MINIMAL_AMLX_DIR);
    }

    @AfterEach
    void teardown() throws IOException {
        Files.deleteIfExists(this.amlxFile.toPath());
    }

    @Test
    void readInvalidAmlxFile() throws Exception {
        AmlxValidator mockAmlxValidator = Mockito.mock(AmlxValidator.class);
        AmlxPackageReader classUnderTest = new AmlxPackageReader(mockAmlxValidator);
        when(mockAmlxValidator.validateAmlx(any(File.class)))
            .thenThrow(new ValidationException("Exception during unit test"));

        assertThrows(TransformationException.class, () -> classUnderTest.readAmlxPackage(amlxFile));
    }

    @Test
    void readValidAmlxFile() throws Exception {
        AmlxPackageReader classUnderTest = new AmlxPackageReader();
        AmlxPackage amlxPackage = classUnderTest.readAmlxPackage(amlxFile);
        assertNotNull(amlxPackage.getOpcPackage());
    }

    @Test
    void analyzeValidAmlx() throws TransformationException {
        AmlxPackageReader classUnderTest = new AmlxPackageReader();
        List<AmlxPackagePart> amlxPackageParts = classUnderTest.analyzeAmlx(amlxFile);

        assertThat(amlxPackageParts).isNotNull();
        assertThat(amlxPackageParts).isNotEmpty();
        assertThat(amlxPackageParts).hasSize(3);
        assertThat(amlxPackageParts.get(0).getFileName()).isEqualTo(".rels");
        assertThat(amlxPackageParts.get(2).getPackagePart()).isNotNull();
        assertThat(amlxPackageParts.get(2).getFileName()).isEqualTo("minimal_AutomationMLComponent.aml");
        assertThat(amlxPackageParts.get(2).getContentType()).isEqualTo("model/vnd.automationml+xml");
    }

    @Test
    void analyzeInvalidAmlxFile() throws ValidationException {
        AmlxPackageReader classUnderTest = new AmlxPackageReader();
        File invalidFile = new File("aas/sampleAAS_v2.json");

        assertThrows(ValidationException.class, () -> classUnderTest.analyzeAmlx(invalidFile));
    }
}
