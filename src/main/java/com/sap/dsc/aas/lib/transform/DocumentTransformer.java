/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform;

import java.io.InputStream;
import java.util.Map;

import org.dom4j.Document;

import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.mapping.model.Header;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import com.sap.dsc.aas.lib.transform.postprocessor.AutoWireSubmodels;
import com.sap.dsc.aas.lib.transform.validation.SchemaValidator;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

public abstract class DocumentTransformer extends MappingSpecificationDocumentTransformer {
    {
        // ensure that references to the subModels are created within each AAS
        addPostProcessor(new AutoWireSubmodels());
    }

    /**
     * Transforms an XML file to AAS. We expect the XML file to be UTF-8 encoded.
     *
     * @param inStream
     * @param mapping
     * @return
     * @throws TransformationException
     */
    public AssetAdministrationShellEnvironment execute(InputStream inStream, MappingSpecification mapping,
        Map<String, String> initialVars)
        throws TransformationException {
        if (mapping.getHeader() == null) {
            mapping.setHeader(new Header());
        }
        setNamespaces(mapping.getHeader().getNamespaces());
        Document readXmlDocument = readXmlDocument(inStream);
        validateDocument(readXmlDocument);
        afterValidation(readXmlDocument, mapping);
        return createShellEnv(readXmlDocument, mapping, initialVars);
    }

    /**
     * Transforms an XML file to AAS. We expect the XML file to be UTF-8 encoded.
     *
     * @param inStream
     * @param mapping
     * @return
     * @throws TransformationException
     */
    public AssetAdministrationShellEnvironment execute(InputStream inStream, MappingSpecification mapping)
        throws TransformationException {
        return execute(inStream, mapping, null);
    }

    /**
     * Function called after document is validated and before shell environment gets created.
     *
     * @param readXmlDocument
     * @param mapping
     */
    protected abstract void afterValidation(Document readXmlDocument, MappingSpecification mapping);

    /**
     * actual transformation done after a successful XML read and validation action
     *
     * @param readXmlDocument
     * @param mapping
     * @return
     * @throws TransformationException
     */

    /**
     * Parses and XML Document from InputStream.
     *
     * Note that the input stream will be read and closed by this method.
     *
     * The SAXReader is unable to parse the XML file if it uses the wrong encoding to read an input
     * stream. We expect files in UTF-8 format only. Otherwise SAXReader relies on the System Charset,
     * which i.e. in the case of Docker containers (sapmachine:11) can be: US-ASCII if the Locale is not
     * set and `locale` output is: LANG= LANGUAGE= LC_CTYPE="POSIX" LC_NUMERIC="POSIX" LC_TIME="POSIX"
     * LC_COLLATE="POSIX" LC_MONETARY="POSIX" LC_MESSAGES="POSIX" LC_PAPER="POSIX" LC_NAME="POSIX"
     * LC_ADDRESS="POSIX" LC_TELEPHONE="POSIX" LC_MEASUREMENT="POSIX" LC_IDENTIFICATION="POSIX" LC_ALL=
     *
     * @param inStream
     * @return
     * @throws TransformationException
     */
    protected abstract Document readXmlDocument(InputStream inStream) throws TransformationException;

    /**
     * Validates a given XML file. We expect the XML file to be UTF-8 encoded.
     *
     *
     * @param document read org.dom4j.Document
     * @throws TransformationException If the input stream is not valid
     */
    protected void validateDocument(Document document) throws TransformationException {
        getSchemaValidator().validate(document);
    }

    protected abstract SchemaValidator getSchemaValidator();

}
