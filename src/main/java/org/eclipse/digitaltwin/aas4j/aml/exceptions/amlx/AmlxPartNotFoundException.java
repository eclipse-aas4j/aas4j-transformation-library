/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx;

public class AmlxPartNotFoundException extends AmlxValidationException {

    private static final long serialVersionUID = -6315650802869967321L;

    /**
     *
     * @param pathInAmlx The path within the amlx to the file
     */
    public AmlxPartNotFoundException(String pathInAmlx) {
        super("A file was defined (via relationship in /_rels/.rels) but could not be found within the AMLX container "
            + "(relationship file path='" + pathInAmlx + "')");
    }

}
