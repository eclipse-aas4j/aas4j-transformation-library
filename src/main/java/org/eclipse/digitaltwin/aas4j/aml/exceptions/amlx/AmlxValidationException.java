/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx;

import org.eclipse.digitaltwin.aas4j.exceptions.ValidationException;

public class AmlxValidationException extends ValidationException {

    private static final long serialVersionUID = 496721678881430420L;

    public AmlxValidationException(final String message) {
        super("Validation error for amlx file: " + message);
    }

    public AmlxValidationException(final String message, final Throwable rootCause) {
        super("Validation error for amlx file: " + message, rootCause);
    }

}
