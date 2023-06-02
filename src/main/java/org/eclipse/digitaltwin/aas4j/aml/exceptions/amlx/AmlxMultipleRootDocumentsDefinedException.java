/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx;

public class AmlxMultipleRootDocumentsDefinedException extends AmlxValidationException {

    private static final long serialVersionUID = 8010818651610754595L;

    public AmlxMultipleRootDocumentsDefinedException() {
        super("The .rels file in the container has multiple <Relationship> of type RootDocument");
    }

}
