/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.placeholder.exceptions;

public class PlaceholderValueMissingException extends IllegalStateException {

    private static final long serialVersionUID = 8550109938648946314L;

    public PlaceholderValueMissingException(String placeholderName) {
        super("No value for placeholder '" + placeholderName + "' was given.");
    }
}
