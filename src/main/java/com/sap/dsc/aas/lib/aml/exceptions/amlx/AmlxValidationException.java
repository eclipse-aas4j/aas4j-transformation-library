/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.exceptions.amlx;

import com.sap.dsc.aas.lib.exceptions.ValidationException;

public class AmlxValidationException extends ValidationException {

    private static final long serialVersionUID = 496721678881430420L;

    public AmlxValidationException(final String message) {
        super("Validation error for amlx file: " + message);
    }

    public AmlxValidationException(final String message, final Throwable rootCause) {
        super("Validation error for amlx file: " + message, rootCause);
    }

}
