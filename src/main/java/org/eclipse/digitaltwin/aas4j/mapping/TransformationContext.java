/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.digitaltwin.aas4j.expressions.Expression;
import org.eclipse.digitaltwin.aas4j.mapping.model.Template;

/**
 * Context Object which provides a Context Item and Defintions and Variables defines so far.
 *
 * @author br-iosb
 *
 */
public class TransformationContext {

    private Map<String, Expression> definitions = new HashMap<>();
    private Map<String, String> variables = new HashMap<>();
    private Object ctxItem;

    private TransformationContext(Object ctxItem) {
        this.ctxItem = ctxItem;
    }

    public static TransformationContext emptyContext() {
        return new TransformationContext(null);
    }

    /**
     * creates a new TransformationContext, inherits from an already existing parentCtx and add Template
     * specific definitions
     *
     * @param parentCtx TransformationContext to inherit Variables and Definitions from, might be null
     *        if nothing is to inherit from
     * @param ctxItem usually the current scope (e.g. result of @foreach) in which Expressions will be
     *        executed
     * @param template Template which might contain new Variables and Definitions, might be null
     * @return the newly created TransformationContext
     */
    static TransformationContext buildContext(TransformationContext parentCtx, Object ctxItem,
        Template template) {
        TransformationContext build = new TransformationContext(ctxItem);
        // take over parent ctx
        if (parentCtx != null) {
            if (parentCtx.getDefinitions() != null) {
                build.definitions.putAll(parentCtx.getDefinitions());
            }
            if (parentCtx.getVariables() != null) {
                build.variables.putAll(parentCtx.getVariables());
            }
        }
        // add and/or override with template context
        if (template != null) {
            if (template.getDefinitions() != null) {
                build.definitions.putAll(template.getDefinitions());
            }
            if (template.getVariables() != null) {
                template.getVariables().forEach((key, expr) -> {
                    String value = expr.evaluateAsString(build);
                    build.variables.put(key, value);
                });
            }
        }
        return build;
    }

    /**
     * creates a new TransformationContext, inherits from an already existing parentCtx and add Template
     * specific definitions
     *
     * @param parentCtx TransformationContext to inherit Variables and Definitions from, might be null
     *        if nothing is to inherit from
     * @param ctxItem usually the current scope (e.g. result of @foreach) in which Expressions will be
     *        executed
     * @param template Template which might contain new Variables and Definitions, might be null
     * @param placeholderVars additional or initial vars
     * @return the newly created TransformationContext
     */
    static TransformationContext buildContext(TransformationContext parentCtx, Object ctxItem, Template template,
        Map<String, String> placeholderVars) {
        TransformationContext buildContext = buildContext(parentCtx, ctxItem, template);
        if (placeholderVars != null) {
            buildContext.variables.putAll(placeholderVars);
        }
        return buildContext;
    }

    /**
     * @return usually the current scope (e.g. result of @foreach) in which Expressions will be executed
     */
    public Object getContextItem() {
        return ctxItem;
    }

    /**
     * @return A map of definition name and definition expression defined for that Context
     */
    public Map<String, Expression> getDefinitions() {
        return definitions;
    }

    /**
     * @return A map of variable name and variable values defined for that Context
     */
    public Map<String, String> getVariables() {
        return variables;
    }

}
