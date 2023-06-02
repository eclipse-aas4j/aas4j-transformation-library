/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Header extends TemplateSupport {

	private Map<String, String> namespaces;
    private String version;
    private String aasVersion;
    private List<Parameter> parameters = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAasVersion() {
        return aasVersion;
    }

    public void setAasVersion(String aasVersion) {
        this.aasVersion = aasVersion;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    @JsonProperty("@namespaces")
    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public void setBindSpecification(BindSpecification bindSpecification) {
        throw new UnsupportedOperationException("@bind ist not allowed in header");
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    @JsonProperty("@parameters")
    public void setParameters(Map<String, String> parameterMap) {
        this.parameters.addAll(
            parameterMap.entrySet().stream().map(e -> new Parameter(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }
}
