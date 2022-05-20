/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform.validation;

import java.util.List;
import java.util.Map;

import com.sap.dsc.aas.lib.mapping.model.Parameter;
import com.sap.dsc.aas.lib.placeholder.exceptions.PlaceholderValueMissingException;

public class PlaceholdersCheck {

    private List<Parameter> parameters;
    private Map<String, String> initialVars;

    public PlaceholdersCheck(List<Parameter> parameters, Map<String, String> initialVars) {
        this.parameters = parameters;
        this.initialVars = initialVars;
    }

    public void execute() throws PlaceholderValueMissingException {
        for (Parameter parameter : parameters) {
            boolean containsKey = initialVars.containsKey(parameter.getName());
            if (!containsKey) {
                throw new PlaceholderValueMissingException(parameter.getName());
            }
        }

    }

}
