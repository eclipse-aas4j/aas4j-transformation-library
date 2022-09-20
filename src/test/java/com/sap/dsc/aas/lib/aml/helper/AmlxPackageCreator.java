/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.util.IOUtils;

public class AmlxPackageCreator {

    public static File compressFolder(String sourceDir) throws IOException {
        return compressFolder(sourceDir, sourceDir + ".amlx");
    }

    private static File compressFolder(String sourceDir, String outputFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile))) {
            compressDirectoryToZipFile((new File(sourceDir)).toURI(), new File(sourceDir), zipOutputStream);
            return Paths.get(outputFile).toFile();
        }
    }

    private static void compressDirectoryToZipFile(URI basePath, File dir, ZipOutputStream out) throws IOException {
        try (Stream<Path> fileListStream = Files.list(Paths.get(dir.getAbsolutePath()))) {
            List<File> fileList = fileListStream
                .map(Path::toFile)
                .collect(Collectors.toList());
            for (File file : fileList) {
                if (file.isDirectory()) {
                    compressDirectoryToZipFile(basePath, file, out);
                } else {
                    // Relativize example:
                    // basePath = /path/to/directory/
                    // filePath = /path/to/directory/files/doc.pdf
                    // result: files/doc.pdf
                    String pathInsideZip = basePath.relativize(file.toURI()).getPath();
                    out.putNextEntry(new ZipEntry(pathInsideZip));
                    try (FileInputStream in = new FileInputStream(file)) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }

    public static final String PATH_TO_MINIMAL_AMLX = AmlxPackageCreator.PATH_TO_MINIMAL_AMLX_DIR + ".amlx";
    public static final String PATH_TO_MINIMAL_AMLX_DIR = "src/test/resources/amlx/minimal_AutomationMLComponent";

}
