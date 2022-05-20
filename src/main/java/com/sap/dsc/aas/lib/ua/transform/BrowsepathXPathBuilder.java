/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.ua.transform;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.dsc.aas.lib.transform.XPathBuilder;
import com.sap.dsc.aas.lib.transform.XPathHelper;

public class BrowsepathXPathBuilder implements XPathBuilder {

    private final XPathHelper xpathHelper;
    private final List<String> namespaceURIs;
    private final Set<String> hierarchyReferences;
    private String hierarchyIsConstraint;
    private Document root;
    private static BrowsepathXPathBuilder instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private BrowsepathXPathBuilder(Document root) {
        this.root = root;
        String DEFAULT_NS = "http://opcfoundation.org/UA/";
        xpathHelper = XPathHelper.getInstance();
        hierarchyReferences = new HashSet<>();
        hierarchyReferences.addAll(Hierarchy.hierarchyReferences());
        namespaceURIs = new ArrayList<>();
        namespaceURIs.add(DEFAULT_NS);
        namespaceURIs.addAll(root.getRootElement().element("NamespaceUris").elements("Uri")
            .stream().map(Element::getText).collect(Collectors.toList()));
        hierarchyReferences.addAll(root.getRootElement().element("Aliases").elements("Alias")
            .stream().filter(e -> Hierarchy.hierarchyReferences().contains(e.getText()))
            .map(e -> e.attributeValue("Alias"))
            .collect(Collectors.toSet()));
        hierarchyIsConstraint = "(";
        for (String ref : hierarchyReferences) {
            hierarchyIsConstraint += "@ReferenceType=\"" + ref + "\" or ";
        }
        hierarchyIsConstraint = hierarchyIsConstraint.substring(0, hierarchyIsConstraint.length() - 3) + ") and @IsForward= \"false\"";
        instance = this;
    }

    static void updateInstance(Document doc) {
        instance = new BrowsepathXPathBuilder(doc);
    }

    public static BrowsepathXPathBuilder getInstance() {
        if (instance != null) {
            return instance;
        }
        throw new IllegalArgumentException("BrowsepathBuilder is not set yet");
    }

    @Override
    public String pathExpression(String[] browsePath) {
        if (browsePath == null || browsePath.length == 0 || browsePath[0] == null || browsePath[0].replace("/", "").trim().isEmpty()) {
            return null;
        }
        String exp = "/UANodeSet/*[@BrowseName=\"" + browsePath[0] + "\"]/@NodeId";
        if (browsePath.length == 1) {
            return exp;
        }
        List<Node> parentIDs = xpathHelper.getNodes(root, exp);
        if (parentIDs == null || parentIDs.isEmpty()) {
            return null;
        }
        return getExpForBrowsePath(Arrays.copyOfRange(browsePath, 1, browsePath.length), parentIDs.get(0).getParent());
    }

    private String getExpForBrowsePath(String[] browsePath, Element parentNode) {
        if (parentNode == null || browsePath[0] == null || browsePath[0].replace("/", "").trim().isEmpty()) {
            return null;
        }
        // get exp that evaluated to all nodes whose BrowseName given by first parameter and reference the
        // second parameter as parent (isConstraint)
        String exp = computeIsExpression(browsePath[0], parentNode.attributeValue("NodeId"));
        List<Node> parents = xpathHelper.getNodes(root, exp);
        if (browsePath.length == 1 && parents != null && !parents.isEmpty()) {
            return exp;
        }
        if (parents == null || parents.isEmpty()) {
            exp = computeHasExpression(browsePath[0], parentNode);
            parents = xpathHelper.getNodes(root, exp);
        }
        if (parents == null || parents.isEmpty()) {
            return null;
        }
        if (browsePath.length == 1) {
            return exp;
        }
        String[] newPath = Arrays.copyOfRange(browsePath, 1, browsePath.length);
        return getExpForBrowsePath(newPath, (Element) parents.get(0));
    }

    public String getNodeIdFromBrowsePath(String[] browsePath) {
        Node node = getNodeFromBrowsePath(browsePath);
        if (node instanceof Element) {
            return ((Element) node).attributeValue("NodeId");
        }
        return null;
    }

    public Node getNodeFromBrowsePath(String[] browsePath) {
        return getNodeFromBrowsePath(browsePath, null);
    }

    private Node getNodeFromBrowsePath(String[] browsePath, Node prev) {
        if (browsePath == null || browsePath.length == 0 || browsePath[0] == null
            || browsePath[0].replace("/", "").trim().isEmpty()) {
            return prev;
        }
        List<Node> parents = xpathHelper.getNodes(root, "/UANodeSet/*[@BrowseName=\"" + browsePath[0] + "\"]");
        if (parents == null || parents.size() == 0 || parents.get(0) == null) {
            return prev;
        }
        // start node in browse path (when prev == null) should be unique in NodeSet
        // if prev is null then we are on start node in browse path therefore parents should have only one
        // parent, else chose the parent that it's parent is prev
        Node parent = null;
        if (prev == null) {
            if (parents.size() != 1)
                return null;
            parent = parents.get(0);
        } else {
            for (int p = 1; p < parents.size(); p++) {
                if (isParent((Element) prev, (Element) parents.get(p))) {
                    parent = parents.get((p));
                    break;
                }
            }
        }
        if (parent == null || (parent instanceof Element && ((Element) parent).attributeCount() == 0)) {
            return prev;
        }
        Element parentElem = (Element) parent;
        String parentId = parentElem.attributeValue("NodeId");
        if (browsePath.length == 1 && parentElem.attributeCount() > 0)
            return parentElem;
        // considering references: "HasComponent", "HasProperty" and "Organizes" to determine children of a
        // node
        // Child and Parent relationships are in context of these references
        List<Node> childrenNodeId = xpathHelper.getNodes(root, "/UANodeSet/*[@BrowseName=\"" + browsePath[1] + "\"]");
        Node childNode = findReferencedChild(parent, childrenNodeId);
        if (childNode != null) {
            return getNodeFromBrowsePath(Arrays.copyOfRange(browsePath, 2, browsePath.length), childNode);
        }

        // considering references: "IsComponent", "IsProperty" and "OrganizedBy" to determine children of a
        // node
        String exp = computeIsExpression(browsePath[1], parentId);
        List<Node> nodes = xpathHelper.getNodes(root, exp);
        if (nodes == null || nodes.size() == 0 || !(nodes.get(0) instanceof Element)) {
            return prev;
        }
        String[] newPath = Arrays.copyOfRange(browsePath, 2, browsePath.length);
        // Node nodeId = ((Element)nodes.get(0)).attribute("NodeId");
        return getNodeFromBrowsePath(newPath, nodes.get(0));
    }

    /*
     * parent and child relationships don't have the meaning of xml nodes relationships theses
     * relationships are in context of UANode set (in context of "HasComponent","HasProperty",
     * "Organizes"
     */
    private boolean isParent(Element parent, Element child) {
        if (parent == null || child == null ||
            child.getNodeType() != Node.ELEMENT_NODE || child.attributeCount() == 0 ||
            parent.getNodeType() != Node.ELEMENT_NODE || parent.attributeCount() == 0) {
            return false;
        }
        return isParent(parent, child, true) || isParent(child, parent, false);
    }

    private boolean isParent(Element parent, Element child, boolean isForward) {
        Element reference = parent.element("References");
        if (reference == null)
            return false;
        List<Element> refs = reference.elements("Reference");
        if (refs == null || refs.isEmpty())
            return false;
        for (Element ref : refs) {
            String isChild = isForward ? "true" : "false";
            String refType = ref.attributeValue("ReferenceType");
            String childId = child.attributeValue("NodeId");
            if (Objects.equals(ref.attributeValue("IsForward"), isChild) &&
                hierarchyReferences.contains(refType) && ref.getText().equals(childId)) {
                return true;
            }
        }
        return false;
    }

    private Node findReferencedChild(Node parent, List<Node> childrenNodes) {
        return ((Element) parent).element("References").elements("Reference")
            .stream().filter(ref -> hierarchyReferences.contains(ref.attributeValue("ReferenceType"))
                && (ref.attributeValue("IsForward") == null || Objects.equals(ref.attributeValue("IsForward"), "true")))
            .map(n -> getNodeIdSet(n.getText(), childrenNodes)).findFirst().orElse(null);
    }

    private Node getNodeIdSet(String id, List<Node> childrenNodes) {
        if (childrenNodes == null || childrenNodes.size() == 0)
            return null;
        return childrenNodes.stream().filter(ch -> ch instanceof Element
            && Objects.equals(((Element) ch).attributeValue("NodeId").replaceAll("\"", ""), id))
            .findFirst().orElse(null);
    }

    String getNamespace(String browseName) {
        if (browseName == null || browseName.trim().isEmpty()) {
            return namespaceURIs.get(0);
        }
        String[] bn = browseName.split(":");
        if (bn.length == 1) {
            return namespaceURIs.get(0);
        } else if (bn.length > 1) {
            try {
                int index = Integer.parseInt(bn[0]);
                if (index >= 0 && index < namespaceURIs.size())
                    return namespaceURIs.get(index);
            } catch (NumberFormatException e) {
                return namespaceURIs.get(0);
            }
        }
        return null;
    }

    private String computeIsExpression(String browseName, String text) {
        return "/UANodeSet/*[@BrowseName=\"" + browseName + "\"]/*[name()=\"References\"]/*[name()=\"Reference\" and " +
            "text()= \"" + text + "\" and (" +
            hierarchyIsConstraint +
            ")]/parent::*/parent::*";
    }

    private String computeHasExpression(String browseName, Element parentNode) {
        List<String> ids = parentNode.element("References").elements("Reference").stream()
            .filter(ref -> hierarchyReferences.contains(ref.attributeValue("ReferenceType"))
                && (ref.attributeValue("IsForward") == null || ref.attributeValue("IsForward").equals("true")))
            .map(Element::getText).collect(Collectors.toList());
        StringBuilder idExpBuilder = new StringBuilder(" and ( ");
        for (String id : ids) {
            idExpBuilder.append(" @NodeId=\"").append(id).append("\" or");
        }
        String idExp = idExpBuilder.toString();
        if (idExp.length() > 7) {
            idExp = idExp.substring(0, idExp.length() - 2) + " )";
        } else {
            idExp = "";
        }
        return "/UANodeSet/*[@BrowseName=\"" + browseName + "\"" + idExp + "]";
    }

    public List<Node> getUaChildren(Element uaNode) {
        List<Node> children;
        try {
            children = new ArrayList<>();
            List<String> hasChildren = uaNode.element("References").elements("Reference")
                .stream().filter(r -> hierarchyReferences.contains(r.attributeValue("ReferenceType"))
                    && (r.attributeValue("IsForward") == null || r.attributeValue("IsForward").equals("true")))
                .map(Element::getText).collect(Collectors.toList());
            for (String nodeId : hasChildren) {
                children.addAll(xpathHelper.getNodes(root, "/UANodeSet/*[@NodeId=\"" + nodeId + "\"]"));
            }
            // add isChildren
            children.addAll(root.getRootElement().elements().stream()
                .filter(e -> e.element("References") != null
                    && e.element("References").elements("Reference").stream()
                        .anyMatch(r -> r.getText().equals(uaNode.attributeValue("NodeId").replaceAll("\"", ""))
                            && hierarchyReferences.contains(r.attributeValue("ReferenceType"))
                            && Objects.equals(r.attributeValue("IsForward"), "false")))
                .collect(Collectors.toSet()));
            // for example return null if uaNode doesn't have the subnode "References", this will be interpreted
            // in the Expr @uaChildren as mal argument Exception
        } catch (Exception e) {
            return null;
        }
        return children;
    }

    enum Hierarchy {
        HAS_COMPONENT("i=47"), HAS_PROPERTY("i=46"), ORGANIZES("i=35"), HAS_CHILD("i=34"), AGGREGATES("i=44"), HAS_SUBTYPE("i=45");
        String nodeId;

        Hierarchy(String nodeId) {
            this.nodeId = nodeId;
        }

        public static Set<String> hierarchyReferences() {
            return new HashSet<>(Arrays.asList(HAS_COMPONENT.nodeId, HAS_PROPERTY.nodeId, ORGANIZES.nodeId, HAS_CHILD.nodeId,
                AGGREGATES.nodeId, HAS_SUBTYPE.nodeId));
        }
    }

}
