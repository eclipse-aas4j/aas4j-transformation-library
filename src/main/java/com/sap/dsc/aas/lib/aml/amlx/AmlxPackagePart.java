/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.amlx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.opc.PackagePart;

public class AmlxPackagePart {

    private final PackagePart packagePart;
    private final String pathInAmlx;

    /**
     * @param packagePart The package part that can be used to retrieve the file from within the amlx
     *        package
     * @param pathInAmlx The path within the AMLX, for example "/files/document.pdf"
     */
    public AmlxPackagePart(String pathInAmlx, PackagePart packagePart) {
        this.packagePart = packagePart;
        this.pathInAmlx = pathInAmlx;
    }

    public PackagePart getPackagePart() {
        return packagePart;
    }

    /**
     * Returns the full path within the AMLX, for example "/files/document.pdf"
     *
     * @return The path of the file within the AMLX
     */
    public String getPathInAmlx() {
        return pathInAmlx;
    }

    /**
     * Returns the name of the file, for example "document.pdf"
     *
     * @return The name of the file
     */
    public String getFileName() {
        return Paths.get(getPathInAmlx()).getFileName().toString(); // will be document.pdf
    }

    /**
     * Returns the content type of the file, for example "application/pdf"
     *
     * @return The content type of the file
     */
    public String getContentType() {
        return packagePart.getContentType();
    }

    /**
     * Returns the input stream of this PackagePart.
     *
     * @return The input stream of this package part.
     * @throws IOException If something goes wrong while reading the file
     */
    public InputStream getInputStream() throws IOException {
        return packagePart.getInputStream();
    }

    /**
     * Factory method for constructing an AmlxPackagePart
     *
     * @param packagePart The OPC package part
     * @return The AMLX package part as wrapper of the package part
     */
    public static AmlxPackagePart fromPackagePart(PackagePart packagePart) {
        // Example : "/files/document.pdf"
        String pathInAmlx = packagePart.getPartName().getName().replace(File.separator, "/");

        // final String pathToFile = path.getParent().toString(); // will be /files
        return new AmlxPackagePart(pathInAmlx, packagePart);
    }

}
