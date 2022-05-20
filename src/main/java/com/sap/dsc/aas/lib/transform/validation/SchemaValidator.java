/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform.validation;

import java.net.URL;

import org.dom4j.Document;

import com.sap.dsc.aas.lib.exceptions.TransformationException;

public abstract class SchemaValidator {

    private URL schemaUrl;

    public SchemaValidator(URL schemaUrl) {
        this.schemaUrl = schemaUrl;
    }

    public abstract void validate(Document document) throws TransformationException;

    public URL getSchemaURL() {
        return schemaUrl;
    }

}
