/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.dsc.aas.lib.exceptions.TransformationException;
import com.sap.dsc.aas.lib.mapping.TemplateTransformer;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

public class MappingSpecificationDocumentTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<Consumer<AssetAdministrationShellEnvironment>> postProcessors = new ArrayList<>();

    public MappingSpecificationDocumentTransformer() {}

    /**
     * Map document based on the mapping configuration into one flat AAS env.
     *
     * @param document The XML document
     * @param mappings The mapping configuration
     * @return Flat AAS env
     * @throws TransformationException If something goes wrong during transformation
     */
    protected AssetAdministrationShellEnvironment createShellEnv(Document document, MappingSpecification mappings,
        Map<String, String> initialVars)
        throws TransformationException {
        if (mappings.getAasEnvironmentMapping() != null) {

            LOGGER.info("Transforming AAS Environment...");

            AssetAdministrationShellEnvironment transformedEnvironment = new TemplateTransformer().transform(mappings,
                document, initialVars);
            executePostProcessors(transformedEnvironment);

            return transformedEnvironment;
        } else {
            throw new IllegalArgumentException("No AAS Environment specified!");
        }
    }

    private void executePostProcessors(AssetAdministrationShellEnvironment transformedEnvironment) {
        postProcessors.stream().forEach(c -> c.accept(transformedEnvironment));
    }

    /**
     * adds a function which gets called with the AssetAdministrationShellEnvironment result after the
     * transformation process
     */
    public void addPostProcessor(Consumer<AssetAdministrationShellEnvironment> postProcessor) {
        postProcessors.add(postProcessor);
    }

    public void setNamespaces(Map<String, String> namespaces) {
        XPathHelper.getInstance().addNamespaceBindings(namespaces);
    }

}
