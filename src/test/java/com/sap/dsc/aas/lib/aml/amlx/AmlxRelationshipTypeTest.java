/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.aml.amlx;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AmlxRelationshipTypeTest {

    private static Stream<Arguments> parameterValues() {
        return Stream.of(
            arguments("Null value", null, false),
            arguments("Empty value", "", false),
            arguments("Root AML document", "http://schemas.automationml.org/container/relationship/RootDocument", true),
            arguments("Any document", "http://schemas.automationml.org/container/relationship/AnyContent", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterValues")
    @DisplayName("Get multiple string values based on a given XPath")
    void isRelationshipTypeAml(String name, String relationshipTypeUri, boolean expectedResult) {
        assertThat(AmlxRelationshipType.isAmlType(relationshipTypeUri)).isEqualTo(expectedResult);
    }
}
