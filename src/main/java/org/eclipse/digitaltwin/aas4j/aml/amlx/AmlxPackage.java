/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.amlx;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;

public class AmlxPackage {

    /**
     * The amlx file is an OPC (open packaging conventions) file
     */
    private final OPCPackage opcPackage;
    private final List<AmlxPackagePart> listOfNonAmlFiles;

    public AmlxPackage(OPCPackage opcPackage) throws InvalidFormatException {
        this.opcPackage = opcPackage;
        this.listOfNonAmlFiles = this.loadNonAmlFiles();
    }

    // @formatter:off
    /**
     * Searches the /_rels/.rels file for a list of files that are not any of the following types: .aml .xsd.
     *
     * For filtering, the relationship type URI in the .rels file is used. For example, consider a .amlx package with the following structure:
     *
     * <pre>
     * _rels/.rels                  --&gt; Not returned
     * root.aml                     --&gt; Not returned (this file can be retrieved by {@link #getRootAmlFile() getRootAmlFile}).
     * CAEX_ClassModel_V.3.0.xsd    --&gt; Not returned
     * files/document.pdf           --&gt; Returned
     * files/textFile.txt           --&gt; Returned
     * lib/amlLibrary.aml           --&gt; Not returned
     * </pre>
     *
     * @return List of content files, e.g. PDF, CAD, Step5
     */
    // @formatter:off
    public List<AmlxPackagePart> getNonAmlFiles() {
        return this.listOfNonAmlFiles;
    }

    /**
     * Gets the root .aml file as defined by the /_rels/.rels file
     *
     * @return The root element of the AMLX package
     */
    public AmlxPackagePart getRootAmlFile() {
        PackagePart rootPart = opcPackage.getPartsByRelationshipType(AmlxRelationshipType.ROOT.getURI()).get(0);
        return AmlxPackagePart.fromPackagePart(rootPart);
    }

    protected OPCPackage getOpcPackage() {
        return opcPackage;
    }

    protected final List<AmlxPackagePart> loadNonAmlFiles() throws InvalidFormatException {
        try {
            return StreamSupport.stream(opcPackage.getRelationships().spliterator(), false)
                    .filter(relationship -> !AmlxRelationshipType.isAmlType(relationship.getRelationshipType()))
                    .map(this::getAmlxPackagePartByRelationship)
                    .collect(Collectors.toList());
        } catch (IllegalStateException ex) {
            final Throwable throwable = ex.getCause();
            if (throwable instanceof InvalidFormatException)
                throw (InvalidFormatException) throwable;
            throw ex;
        }
    }

    protected AmlxPackagePart getAmlxPackagePartByRelationship(PackageRelationship relationship) {
        // We can't use opcPackage.getPart(relationship):
        // That function only checks the relationship type and returns the first match
        // Instead, check the complete URI
        // Target URI example: "/files/document.pdf"
        final String relationshipTargetUri = relationship.getTargetURI().toString();

        try {
            return opcPackage.getParts().stream() //
                .filter(part -> part.getPartName().getName().equals(relationshipTargetUri)) //
                .findFirst() //
                .map(AmlxPackagePart::fromPackagePart).orElse(null);
        } catch (InvalidFormatException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
