/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.transform;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.eclipse.digitaltwin.aas4j.exceptions.NoResultByXPathException;
import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.junit.After;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class XPathHelperTest extends AbstractTransformerTest {

    private XPathHelper classUnderTest;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private static Stream<Arguments> stringValues() {
        return Stream.of(
            arguments("'Hello world'", Arrays.asList("Hello world")),
            arguments("//caex:DoesNotExist", Arrays.asList()),
            arguments("//caex:Attribute[@Name='MySubAttribute']//caex:Value", Arrays.asList("MyValue")),
            arguments("//caex:Attribute//caex:Value", Arrays.asList("MyValue", "Not Searched")),
            arguments("//caex:Attribute//caex:Value", Arrays.asList("MyValue", "Not Searched")),
            arguments("//caex:Attribute", Arrays.asList("Not SearchedMyValueOtherValue", "Not Searched", "MyValueOtherValue", "")),
            arguments("count(//caex:Value)", Arrays.asList("2.0")),
            arguments("//caex:Value", Arrays.asList("MyValue", "Not Searched")));
    }

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.classUnderTest = XPathHelper.getInstance();
        classUnderTest.setNamespaceBinding("caex", "http://www.dke.de/CAEX");
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Get a string value based on a given XPath")
    void getStringValue() throws TransformationException {
        assertEquals("MyAttribute", classUnderTest.getStringValue(attribute, "@Name"));
        assertEquals("Hello World", classUnderTest.getStringValue(attribute, "'Hello World'"));
        assertThrows(NoResultByXPathException.class, () -> classUnderTest.getStringValue(attribute, "DoesNotExist"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stringValues")
    @DisplayName("Get multiple string values based on a given XPath")
    void getStringValues(String xPath, List<String> expectedValues) {
        assertThat(classUnderTest.getStringValues(unitClass, xPath)).containsExactlyElementsIn(expectedValues);
    }

    @Test
    void doubleNamespaceBinding() {
        classUnderTest.setNamespaceBinding("caex", "http://www.dke.de/CAEX");
        assertThat(outContent.toString().contains("already set"));
        assertThat(!outContent.toString().contains("will be overriden"));
    }

    @Test
    void overrideNamespaceBinding() {
        classUnderTest.setNamespaceBinding("caex", "http://overrideURI");
        assertThat(outContent.toString().contains("already set"));
        assertThat(outContent.toString().contains("will be overriden"));
    }

    @Test
    void illegalNamespaceBinding() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> classUnderTest.setNamespaceBinding("", "http://www.dke.de/CAEX"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> classUnderTest.setNamespaceBinding("myprefix", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> classUnderTest.setNamespaceBinding("myprefix", null));
    }

    @Test
    void testAddNamespaceBindings() {
        Map<String, String> bindings = new HashMap<>();
        bindings.put("a", "http://a.org/ns");
        bindings.put("b", "http://b.org/ns");
        classUnderTest.addNamespaceBindings(bindings);
        Map<String, String> namespaceBindings = classUnderTest.getNamespaceBindings();
        assertThat(namespaceBindings).containsAtLeastEntriesIn(bindings);
    }

    @Test
    void testXPathWithNamespaceBindings() throws Exception {
        InputStream testInput = Files.newInputStream(Paths.get("src/test/resources/ua/aasfull.xml"));
        Document testDoc = new SAXReader().read(testInput);

        XPathHelper.getInstance().setNamespaceBinding("opc", "http://opcfoundation.org/UA/2011/03/UANodeSet.xsd");
        XPathHelper.getInstance().setNamespaceBinding("uax", "http://opcfoundation.org/UA/2008/02/Types.xsd");
        XPathHelper.getInstance().setNamespaceBinding("bla", "http://opcfoundation.org/UA/I4AAS/V3/Types.xsd");

        Object evaluate = XPathHelper.getInstance().createXPath(testDoc,
            "string(/opc:UANodeSet/opc:UAVariable/opc:Value/uax:ListOfExtensionObject/uax:ExtensionObject/uax:Body/bla:AASKeyDataType/bla:Type)")
            .evaluate(testDoc);
        assertNotNull(evaluate);

    }

}
