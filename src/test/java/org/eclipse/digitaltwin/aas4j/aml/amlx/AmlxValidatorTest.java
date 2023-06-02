/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.amlx;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.eclipse.digitaltwin.aas4j.TestUtils;
import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.eclipse.digitaltwin.aas4j.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx.AmlxInvalidRootAmlDocumentException;
import org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx.AmlxMultipleRootDocumentsDefinedException;
import org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx.AmlxNoRootDocumentDefinedException;
import org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx.AmlxPartNotFoundException;
import org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx.AmlxRelationshipNotFoundException;
import org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx.AmlxValidationException;
import org.eclipse.digitaltwin.aas4j.aml.helper.AmlxPackageCreator;

class AmlxValidatorTest {

    private AmlxValidator classUnderTest;

    @BeforeEach
    void setup() throws Exception {
        TestUtils.resetBindings();
        this.classUnderTest = new AmlxValidator();
    }

    private static Stream<Arguments> parameterValues() {
        return Stream.of(
            arguments("Valid amlx", "minimal_AutomationMLComponent", null),
            arguments("Valid amlx with documents", "minimal_AutomationMLComponent_WithDocuments", null),
            arguments("Invalid amlx with missing referenced documents", "invalid_AutomationMLComponent_WithMissingDocuments",
                ValidationException.class),
            arguments("Invalid amlx with references to missing documents", "invalid_MissingPart", AmlxPartNotFoundException.class),
            arguments("Invalid amlx with missing root AML document", "invalid_MissingRootDocument",
                AmlxPartNotFoundException.class),
            arguments("Invalid amlx with multiple root documents", "invalid_MultipleRootDocuments",
                AmlxMultipleRootDocumentsDefinedException.class),
            arguments("Invalid amlx with no root document", "invalid_NoRootDocument", AmlxNoRootDocumentDefinedException.class),
            arguments("Invalid amlx with invalid AML root document", "invalid_InvalidRootAmlDocument",
                AmlxInvalidRootAmlDocumentException.class),
            arguments("Invalid amlx with additional document not defined in .rels", "invalid_AdditionalDocumentNotInRels",
                AmlxRelationshipNotFoundException.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterValues")
    @DisplayName("Get multiple string values based on a given XPath")
    void validateFile(String name, String amlxDirName, Class<? extends AmlxValidationException> expectedRootCauseThrowable)
        throws IOException {

        String pathToAmlxDir = "src/test/resources/amlx/" + amlxDirName;
        File amlxFile = AmlxPackageCreator.compressFolder(pathToAmlxDir);

        try {
            classUnderTest.validateAmlx(amlxFile);
            if (expectedRootCauseThrowable != null) {
                fail("Should have failed: Exception of class'" + expectedRootCauseThrowable + "' expected but none was thrown.");
            }
        } catch (TransformationException e) {
            assertThat(e).isInstanceOf(expectedRootCauseThrowable);
        } finally {
            Files.deleteIfExists(amlxFile.toPath());
        }
    }

}
