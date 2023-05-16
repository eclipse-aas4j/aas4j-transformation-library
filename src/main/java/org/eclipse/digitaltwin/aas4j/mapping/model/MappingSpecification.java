/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

public class MappingSpecification {

    private AssetAdministrationShellEnvironment aasEnvironmentMapping;
    private Header header;

    public AssetAdministrationShellEnvironment getAasEnvironmentMapping() {
        return aasEnvironmentMapping;
    }

    public void setAasEnvironmentMapping(AssetAdministrationShellEnvironment mapping) {
        this.aasEnvironmentMapping = mapping;
    }

    public Header getHeader() {
        return header;
    }

    @JsonProperty("@header")
    public void setHeader(Header header) {
        this.header = header;
    }
}
