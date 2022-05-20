/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.transform.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sap.dsc.aas.lib.exceptions.UnableToReadXmlException;
import com.sap.dsc.aas.lib.transform.AbstractTransformerTest;

public class AmlSchemaValidatorTest extends AbstractTransformerTest {

    public static final Path VALID_AML_FILE_PATH = Paths.get("src/test/resources/aml/minimal_IdentificationData.aml");
    private AmlSchemaValidator classUnderTest;

    @BeforeEach
    void setup() throws Exception {
        super.setUp();
        this.classUnderTest = new AmlSchemaValidator();
    }

    @Test
    @DisplayName("CAEX schema exists")
    void loadCaexSchema() {
        assertNotNull(this.classUnderTest.getSchemaURL());
    }

    @Test
    @DisplayName("Load valid AML file")
    void loadValidAmlFile() {
        assertDoesNotThrow(() -> classUnderTest.validate(getAmlDocument(Files.newInputStream(VALID_AML_FILE_PATH))));
    }

    @Test
    @DisplayName("Loading invalid AML file throws exception")
    void loadInvalidAmlFile() {
        String initialString = "<?xml version=\"1.0\" encoding=\"utf-8\"?><CustomXmlElement>Text</CustomXmlElement>";
        InputStream inputStream = new ByteArrayInputStream(initialString.getBytes());
        assertThrows(UnableToReadXmlException.class, () -> classUnderTest.validate(getAmlDocument(inputStream)));
    }

    private Document getAmlDocument(InputStream amlStream) throws Exception {
        SAXReader reader = new SAXReader();
        return reader.read(amlStream);
    }

}
