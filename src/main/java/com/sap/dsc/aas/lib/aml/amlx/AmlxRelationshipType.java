/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.amlx;

import java.util.Arrays;

//@formatter:off
/**
 * Used in the /_rels/.rels file contained in an OPC container to identify types of files.
 *
 * Sample relationship for Root:
 *
 * <pre>
 * &#60;Relationship
 *      Type="http://schemas.automationml.org/container/relationship/RootDocument"
 *      Target="/rootDocument.aml"
 *      Id="RelationshipID1" /&#62;
 * </pre>
 *
 * Sample relationship for a file lying in /files:
 *
 * <pre>
 * &#60;Relationship
 *      Type="http://schemas.automationml.org/container/relationship/AnyContent"
 *      Target="/files/document.pdf"
 *      Id="RelationshipID2" /&#62;
 * </pre>
 *
 */
//@formatter:on
public enum AmlxRelationshipType {
    ROOT("http://schemas.automationml.org/container/relationship/RootDocument"), //
    COLLADA("http://schemas.automationml.org/container/relationship/Collada"), //
    PLC_OPEN_XML("http://schemas.automationml.org/container/relationship/PLCOpenXML"), //
    LIBRARY("http://schemas.automationml.org/container/relationship/Library"), //
    CAEX_SCHEMA("http://schemas.automationml.org/container/relationship/CAEXSchema"), //
    COLLADA_SCHEMA("http://schemas.automationml.org/container/relationship/ColladaSchema"), //
    PLC_OPEN_XML_SCHEMA("http://schemas.automationml.org/container/relationship/PLCOpenXMLSchema"), //
    ANY_CONTENT("http://schemas.automationml.org/container/relationship/AnyContent");

    private final String relationshipTypeUri;

    private static final AmlxRelationshipType[] amlFileTypes = {ROOT, LIBRARY, CAEX_SCHEMA, COLLADA_SCHEMA, PLC_OPEN_XML_SCHEMA};

    public String getURI() {
        return this.relationshipTypeUri;
    }

    AmlxRelationshipType(String relationshipTypeUri) {
        this.relationshipTypeUri = relationshipTypeUri;
    }

    /**
     * Checks whether a given relationship type URI (defined in _rels/.rels of an AMLX package) is an
     * AML type.
     *
     * @param relationshipTypeUri The URI of the "Type" attribute, for example
     *        "http://schemas.automationml.org/container/relationship/Library"
     * @return True if the URI is an AML file type.
     */
    public static boolean isAmlType(String relationshipTypeUri) {
        return Arrays.stream(amlFileTypes).anyMatch(amlFileType -> amlFileType.getURI().equals(relationshipTypeUri));
    }
}
