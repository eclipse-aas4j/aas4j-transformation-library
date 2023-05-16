/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.exceptions;

public abstract class TransformationException extends Exception {

    private static final long serialVersionUID = -1635935178564746046L;

    public TransformationException(String message) {
        super(message);
    }

    public TransformationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
