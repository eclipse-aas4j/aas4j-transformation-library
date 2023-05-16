/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.transform;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import org.dom4j.Node;
import org.dom4j.XPath;
import org.eclipse.digitaltwin.aas4j.exceptions.NoResultByXPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class XPathHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private XPathHelper() {}

    private static XPathHelper instance;

    public static XPathHelper getInstance() {
        if (instance == null) {
            instance = new XPathHelper();
        }
        return instance;
    }

    /**
     * Based on the given XPath expression, get a list of {@link org.dom4j.Node}. Filtered for Namespace
     * CAEX.
     *
     * @param parentNode Base node to start the XPath search
     * @param xpathExpression The XPath expression to match
     * @return List of child nodes matching the expression
     */
    public List<Node> getNodes(Node parentNode, String xpathExpression) {
        return createXPath(parentNode, xpathExpression).selectNodes(parentNode);
    }

    public String getStringValueOrNull(Node node, String xPath) {
        Object result = createXPath(node, xPath).evaluate(node);
        if (result instanceof String) {
            return (String) result;
        }
        if (result instanceof Node) {
            return ((Node) result).getStringValue();
        }

        return null;
    }

    public String getStringValue(Node node, String xPath) throws NoResultByXPathException {
        String value = getStringValueOrNull(node, xPath);
        if (value != null) {
            return value;
        }

        throw new NoResultByXPathException(xPath);
    }

    public List<String> getStringValues(Node rootNode, String xPath) {
        Object result = createXPath(rootNode, xPath).evaluate(rootNode);
        if (result instanceof String) {
            return Collections.singletonList((String) result);
        }
        if (result instanceof Node) {
            return Collections.singletonList(((Node) result).getStringValue());
        }
        if (result instanceof Number) {
            return Collections.singletonList(String.valueOf(result));
        }
        // Per .evaluate documentation the result object is never null
        return ((List<?>) result).stream().map(Node.class::cast).map(Node::getStringValue).collect(Collectors.toList());
    }

    /**
     * Create a XPath, with the namespace bindings set.
     *
     * @param parentNode Base node to start the search
     * @param xpathExpression The XPath expression to match
     * @return The searched Node if found, otherwise null
     */
    protected XPath createXPath(Node parentNode, String xpathExpression) {
        XPath xpath = parentNode.createXPath(xpathExpression);

        xpath.setNamespaceURIs(namespaces);

        return xpath;
    }

    private Map<String, String> namespaces = new TreeMap<>();

    /**
     * @param nsPrefix prefix to use
     * @param nsUri namespace bound to the prefix
     */
    public void setNamespaceBinding(String nsPrefix, String nsUri) {
        if (Strings.isNullOrEmpty(nsPrefix)) {
            throw new IllegalArgumentException("No valid Prefix (null or empty).");
        }
        String validNsUri = null;
        try {
            validNsUri = URI.create(nsUri).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("No valid Namespace.", e);
        }

        String currentUri = namespaces.get(nsPrefix);
        if (currentUri == null) {
            namespaces.put(nsPrefix, validNsUri);
        } else if (currentUri != validNsUri) {
            LOGGER.warn(String.format("Prefix '%s' already set to '%s', will be overriden with '%s'", nsPrefix,
                currentUri, validNsUri));
            namespaces.put(nsPrefix, validNsUri);
        } else {
            LOGGER.info(String.format("Prefix '%s' already set to '%s'.", nsPrefix, currentUri));
        }
    }

    /**
     * @return Returns an unmodifiable view of the current namespace bindings.
     */
    public Map<String, String> getNamespaceBindings() {
        return Collections.unmodifiableMap(namespaces);
    }

    /**
     * @param bindingsToAdd
     */
    void addNamespaceBindings(Map<String, String> bindingsToAdd) {
        if (bindingsToAdd != null) {
            bindingsToAdd.forEach((k, v) -> setNamespaceBinding(k, v));
        }
    }

}
