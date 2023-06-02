/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx;

public class AmlxInvalidRootAmlDocumentException extends AmlxValidationException {

    private static final long serialVersionUID = 4770499485042439860L;

    public AmlxInvalidRootAmlDocumentException(String targetUri, final Throwable rootCause) {
        super("Root document (targetUri='" + targetUri + "') exists but is not a valid .aml file", rootCause);
    }

}
