/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.exceptions;

public class NoResultByXPathException extends TransformationException {

    private static final long serialVersionUID = 5614753714042520764L;

    public NoResultByXPathException(String xpath) {
        super("Unable to find a single result for XPath " + xpath);
    }
}
