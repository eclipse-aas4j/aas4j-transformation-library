/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.aml.exceptions.amlx;

public class AmlxNoRootDocumentDefinedException extends AmlxValidationException {

    private static final long serialVersionUID = -8734891952476361125L;

    public AmlxNoRootDocumentDefinedException() {
        super(
            "The .rels file in the .amlx container does not contain a <Relationship> with Type=\"http://schemas.automationml.org/container/relationship/RootDocument\"");
    }

}
