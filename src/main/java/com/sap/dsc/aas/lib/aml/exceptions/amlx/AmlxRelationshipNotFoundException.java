/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.exceptions.amlx;

public class AmlxRelationshipNotFoundException extends AmlxValidationException {

    private static final long serialVersionUID = -3354519491697204287L;

    /**
     *
     * @param pathInAmlx The path within the amlx to the file
     */
    public AmlxRelationshipNotFoundException(String pathInAmlx) {
        super("A file was found within the AMLX container but no relationship was defined in /_rels/.rels "
            + "(file path='" + pathInAmlx + "')");
    }

}
