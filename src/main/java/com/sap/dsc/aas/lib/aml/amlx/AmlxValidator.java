/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.amlx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.dom4j.Document;
import org.dom4j.Element;

import com.sap.dsc.aas.lib.aml.exceptions.amlx.AmlxInvalidRootAmlDocumentException;
import com.sap.dsc.aas.lib.aml.exceptions.amlx.AmlxMultipleRootDocumentsDefinedException;
import com.sap.dsc.aas.lib.aml.exceptions.amlx.AmlxNoRootDocumentDefinedException;
import com.sap.dsc.aas.lib.aml.exceptions.amlx.AmlxPartNotFoundException;
import com.sap.dsc.aas.lib.aml.exceptions.amlx.AmlxRelationshipNotFoundException;
import com.sap.dsc.aas.lib.aml.exceptions.amlx.AmlxValidationException;
import com.sap.dsc.aas.lib.aml.transform.AmlTransformer;
import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.exceptions.ValidationException;

public class AmlxValidator {

    private final AmlTransformer amlTransformer;
    private static final String PATH_TO_RELS = "/_rels/.rels";

    public AmlxValidator() {
        this(new AmlTransformer());
    }

    public AmlxValidator(AmlTransformer amlTransformer) {
        this.amlTransformer = amlTransformer;
    }

    // @formatter:off
    /**
     * .amlx files follow the file structure of ECMA OPC (Open Packaging Conventions) .amlx files are compressed using
     * zip. <br>
     * Files within a .amlx file are called "parts".
     *
     * A valid .amlx file must have:
     *
     * <pre>
     * - Exactly one relationship of type http://schemas.automationml.org/container/relationship/RootDocument
     * - The target (an .aml file) of this relationship must exist
     * - The target aml file of this relationship must be valid (its schema must be valid with regard to CAEX_ClassModel_V3.0.xsd)
     * </pre>
     *
     * See https://www.automationml.org/o.red/uploads/dateien/1505772393-BPR%20008E_BPR_Container_V1.0.0.zip for the
     * best practice reference on the construction of amlx files, which also includes a C# implementation
     *
     * @param amlxFile The AML input file to validate
     * @throws TransformationException If the file is invalid or an IOException occurs
     * @return The OPCPackage if it is a valid AMLX file
     */
    // @formatter:on
    public OPCPackage validateAmlx(File amlxFile) throws TransformationException {
        try {
            OPCPackage opcPackage = OPCPackage.open(amlxFile);
            this.validateAmlx(opcPackage);
            return opcPackage;
        } catch (InvalidFormatException e) {
            throw new AmlxValidationException(e.getMessage(), e);
        }
    }

    private void validateAmlx(OPCPackage opcPackage) throws TransformationException, InvalidFormatException {
        try {
            // Check whether each document defined in /_rels/.rels exists
            StreamSupport.stream(opcPackage.getRelationships().spliterator(), false)
                .forEach(checkPackagePartExists(opcPackage));

            // Check whether each file in the AMLX file (a .zip file) is defined in /_rels/.rels
            opcPackage.getParts().stream()
                .forEach(checkRelationshipExists(opcPackage));

            // Check that
            // 1) The root document (and all other documents) exist
            // 2) There is exactly one root document
            PackageRelationship rootRelation = StreamSupport
                .stream(opcPackage.getRelationships().spliterator(), false)
                .peek(checkPackagePartExists(opcPackage))
                .filter(relationship -> AmlxRelationshipType.ROOT.getURI().equals(relationship.getRelationshipType()))
                .reduce(checkOnlyOneRootElementIsPresent())
                .orElseThrow(AmlxNoRootDocumentDefinedException::new);

            String rootDocumentTarget = rootRelation.getTargetURI().toString();
            PackagePart rootDocumentPart = opcPackage.getPart(rootRelation);

            // Check whether root document is valid AML
            validateRootAmlFile(rootDocumentTarget, rootDocumentPart);

            // Check that all external references in the root AML exist
            for (String externalReference : getExternalReferences(rootDocumentTarget, rootDocumentPart)) {
                boolean exists = false;
                for (PackageRelationship rel : opcPackage.getRelationships()) {
                    if (rel.getTargetURI().toString().equals(externalReference)) {
                        exists = true;
                    }
                }
                if (!exists) {
                    throw new ValidationException(
                        "Root AML contains an ExternalInterface with missing matching document: " + externalReference);
                }
            }
        } catch (IOException ioException) {
            // May occur if amlx file is deleted during validation, for example
            throw new ValidationException("Unexpected issue occured during validation.", ioException);
        } catch (IllegalStateException illegalStateException) {
            if (illegalStateException.getCause() instanceof ValidationException) {
                throw (ValidationException) illegalStateException.getCause();
            }
            throw illegalStateException;
        }
    }

    private void validateRootAmlFile(String rootDocumentTarget, PackagePart rootDocumentPart) throws TransformationException, IOException {
        try (InputStream rootDocument = rootDocumentPart.getInputStream()) {
            Document readXmlDocument = amlTransformer.readXmlDocument(rootDocument);
            amlTransformer.validateDocument(readXmlDocument);
        } catch (TransformationException exception) {
            throw new AmlxInvalidRootAmlDocumentException(rootDocumentTarget, exception);
        }
    }

    private List<String> getExternalReferences(String rootDocumentTarget, PackagePart rootDocumentPart)
        throws TransformationException, IOException {
        try (InputStream rootDocument = rootDocumentPart.getInputStream()) {
            Document document = this.amlTransformer.readXmlDocument(rootDocument);
            Element root = document.getRootElement();

            List<String> externalReferences = new ArrayList<>();
            return getElementList(root, externalReferences);
        } catch (TransformationException exception) {
            throw new AmlxInvalidRootAmlDocumentException(rootDocumentTarget, exception);
        }
    }

    private List<String> getElementList(Element element, List<String> elemList) {
        List<Element> elements = element.elements();
        if (!elements.isEmpty()) {
            // Iterate over child elements
            for (Element elem : elements) {
                if (elem.attributeValue("RefBaseClassPath") != null
                    && elem.attributeValue("RefBaseClassPath").equals("AutomationMLBPRInterfaceClassLib/ExternalDataReference")) {
                    // Element is ExternalInterface node with known structure and 2 children: MIMEType and refURI
                    // cf.
                    // https://www.automationml.org/wp-content/uploads/2021/06/BPR_005E_ExternalDataReference_Jul2016.zip
                    for (Element elem2 : elem.elements()) {
                        if (elem2.attributeValue("Name") != null && elem2.attributeValue("Name").equals("refURI")) {
                            // There should only be one child left, Value
                            for (Element elem3 : elem2.elements()) {
                                // Check whether the value is an URL or local resource locator
                                try {
                                    new URL(elem3.getTextTrim());
                                } catch (MalformedURLException e) {
                                    elemList.add(elem3.getTextTrim());
                                }
                            }
                        }
                    }
                } else {
                    // Recursive traversal
                    getElementList(elem, elemList);
                }
            }
        }
        return elemList;
    }

    private BinaryOperator<PackageRelationship> checkOnlyOneRootElementIsPresent() {
        return (u, v) -> {
            throw new IllegalStateException(new AmlxMultipleRootDocumentsDefinedException());
        };
    }

    private Consumer<PackageRelationship> checkPackagePartExists(OPCPackage opcPackage) {
        return relation -> {
            if (opcPackage.getPart(relation) == null) {
                throw new IllegalStateException(new AmlxPartNotFoundException(relation.getTargetURI().getPath()));
            }
        };
    }

    private Consumer<PackagePart> checkRelationshipExists(OPCPackage opcPackage) {
        return packagePart -> {
            if (PATH_TO_RELS.equals(packagePart.getPartName().getName())) {
                return;
            }

            boolean exists = StreamSupport.stream(opcPackage.getRelationships().spliterator(), false)
                .anyMatch(relationship -> relationship.getTargetURI().equals(packagePart.getPartName().getURI()));
            if (!exists) {
                throw new IllegalStateException(new AmlxRelationshipNotFoundException(packagePart.getPartName().getURI().getPath()));
            }
        };
    }

}
