/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.exceptions;

public abstract class InvalidConfigException extends IllegalStateException {

    private static final long serialVersionUID = 1675163389134334330L;

    public InvalidConfigException(String message) {
        super(message);
    }

}
