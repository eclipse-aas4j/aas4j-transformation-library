/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.amlx;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.opc.OPCPackage;

import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.eclipse.digitaltwin.aas4j.exceptions.ValidationException;

public class AmlxPackageReader {

    private final AmlxValidator amlxValidator;

    public AmlxPackageReader() {
        this(new AmlxValidator());
    }

    public AmlxPackageReader(AmlxValidator amlxValidator) {
        this.amlxValidator = amlxValidator;
    }

    /**
     * Validates and reads a .amlx file.
     *
     * For an explanation of the .amlx structure see
     * {@link AmlxValidator#validateAmlx(File) AmlxValidator.validateAmlx}
     *
     * Note that we use a File instead of an InputStream because this is the recommended approach by
     * ApachePOI (the library we use for AMLX files). A File supports native operations whereas an
     * InputStream must be held in memory. For more information see OPCPackage.open(File) vs
     * OPCPackage.open(InputStream)
     *
     * @param amlxFile
     * @return
     * @throws TransformationException If the AMLX file is invalid or something goes wrong while reading
     *         the file. Use .getCause() to get the root cause.
     */
    public AmlxPackage readAmlxPackage(File amlxFile) throws TransformationException {
        OPCPackage opcPackage = amlxValidator.validateAmlx(amlxFile);
        try {
            return new AmlxPackage(opcPackage);
        } catch (InvalidFormatException e) {
            throw new ValidationException("There was a format error while reading the parts of the AMLX package", e);
        }
    }

    public List<AmlxPackagePart> analyzeAmlx(File amlxFile) throws ValidationException {
        try (OPCPackage opcPackage = OPCPackage.open(amlxFile)) {
            return opcPackage.getParts().stream()
                .map(AmlxPackagePart::fromPackagePart)
                .collect(Collectors.toList());
        } catch (InvalidOperationException | InvalidFormatException | IOException e) {
            throw new ValidationException("There was a format error while reading the parts of the AMLX package", e);
        }
    }
}
