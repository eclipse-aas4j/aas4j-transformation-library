/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.exceptions;

import java.util.Collection;
import java.util.stream.Collectors;

public class InvalidBindingException extends InvalidConfigException {

    private final Collection<String> fields;

    public InvalidBindingException(Collection<String> fields) {
        super("Bound fields '" +
            fields.stream().collect(Collectors.joining(",")) + "' are unknown.");
        this.fields = fields;
    }

    public Collection<String> getFields() {
        return fields;
    }
}
