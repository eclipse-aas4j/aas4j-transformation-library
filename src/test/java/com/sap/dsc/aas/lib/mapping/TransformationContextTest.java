/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.mapping;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.dsc.aas.lib.expressions.Expression;
import com.sap.dsc.aas.lib.expressions.Expressions;
import com.sap.dsc.aas.lib.mapping.model.Template;

class TransformationContextTest {

    private Template mockTemplate1;
    private Template mockTemplate2;

    @BeforeEach
    void setup() {
        HashMap<String, Expression> mockDefs1 = new HashMap<>();
        mockDefs1.put("myDef", Expressions.getConstantByName("pi"));
        mockTemplate1 = Mockito.mock(Template.class);
        Mockito.when(mockTemplate1.getVariables()).thenReturn(null);
        Mockito.when(mockTemplate1.getDefinitions()).thenReturn(mockDefs1);

        HashMap<String, Expression> mockDefs2 = new HashMap<>();
        mockDefs2.put("myDef", Expressions.getConstantByName("NaN"));
        mockDefs2.put("myDef2", Expressions.getConstantByName("e"));
        mockTemplate2 = Mockito.mock(Template.class);
        Mockito.when(mockTemplate2.getVariables()).thenReturn(null);
        Mockito.when(mockTemplate2.getDefinitions()).thenReturn(mockDefs2);

    }

    @Test
    void testBuildContext() {
        TransformationContext emtpyCtx = TransformationContext.buildContext(null, null, null);
        Assertions.assertFalse(emtpyCtx.getContextItem() != null);
        Assertions.assertTrue(emtpyCtx.getDefinitions().size() == 0);
        Assertions.assertTrue(emtpyCtx.getVariables().size() == 0);

        TransformationContext buildContext = TransformationContext.buildContext(emtpyCtx, "TestCtx", mockTemplate1);
        Assertions.assertTrue(buildContext.getContextItem().equals("TestCtx"));
        Assertions.assertEquals(Expressions.getConstantByName("pi"), buildContext.getDefinitions().get("myDef"));

        // test for update
        TransformationContext buildContext2 = TransformationContext.buildContext(buildContext, "NewCtx", mockTemplate2);
        Assertions.assertTrue(buildContext2.getContextItem().equals("NewCtx"));
        Assertions.assertEquals(Expressions.getConstantByName("NaN"), buildContext2.getDefinitions().get("myDef"));
    }

}
