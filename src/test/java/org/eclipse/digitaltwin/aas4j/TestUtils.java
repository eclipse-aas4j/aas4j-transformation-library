/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j;

import org.eclipse.digitaltwin.aas4j.transform.XPathHelper;

public class TestUtils {

    /**
     * should reset all singletons to avoid state to creep into each others unit tests
     *
     * see https://stackoverflow.com/questions/54035180/reset-singleton-for-each-unit-test-java
     *
     * @throws Exception
     */
    public static void resetBindings() throws Exception {
        java.lang.reflect.Field instance = XPathHelper.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    public static void setAMLBindings() throws Exception {
        XPathHelper.getInstance().setNamespaceBinding("caex", "http://www.dke.de/CAEX");
    }

}
