/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package com.sap.dsc.aas.lib.transform;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import com.sap.dsc.aas.lib.TestUtils;
import com.sap.dsc.aas.lib.expressions.ConstantExpr;
import com.sap.dsc.aas.lib.expressions.XPathExpr;
import com.sap.dsc.aas.lib.mapping.model.BindSpecification;
import com.sap.dsc.aas.lib.mapping.model.Header;
import com.sap.dsc.aas.lib.mapping.model.MappingSpecification;
import com.sap.dsc.aas.lib.mapping.model.Template;
import com.sap.dsc.aas.lib.mapping.model.TemplateSupport;

import io.adminshell.aas.v3.dataformat.core.ReflectionHelper;
import io.adminshell.aas.v3.model.AssetAdministrationShell;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.AssetInformation;
import io.adminshell.aas.v3.model.Key;
import io.adminshell.aas.v3.model.KeyElements;
import io.adminshell.aas.v3.model.KeyType;
import io.adminshell.aas.v3.model.Property;
import io.adminshell.aas.v3.model.Reference;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.SubmodelElementCollection;
import io.adminshell.aas.v3.model.impl.DefaultKey;
import io.adminshell.aas.v3.model.impl.DefaultReference;

public abstract class AbstractTransformerTest {

    public static final String ID_VALUE = "1234";
    public static final String VALUE_CONTENT = "MyValue";
    public static final String DEFAULT_VALUE_CONTENT = "OtherValue";
    public static final String DEFAULT_CONFIG_ELEMENT_ID = "myConfigElementId";

    protected Document document;
    protected Element unitClass;
    protected Element attribute;
    protected Element subAttribute;

    protected MappingSpecification mapping;


    protected void setUp() throws Exception {
        TestUtils.resetBindings();
        // sample AML document
        this.document = createDocument();
        // sample mapping specification
        this.mapping = createMappingSpecification();
    }

    protected <T> T createTemplate(Class<T> modelClass) {
        T target;
        try {
            target = ReflectionHelper.getDefaultImplementation(modelClass).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException();
        }

        // create a proxy instance that implements the bean interface and the config interface
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.addAll(Arrays.asList(target.getClass().getInterfaces()));
        interfaces.add(Template.class);
        Template config = new TemplateSupport(target);
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
            interfaces.toArray(new Class<?>[interfaces.size()]),
            (o, method, args) -> {
                try {
                    // route to concrete object - either template definition or the underlying bean
                    if (Template.class.isAssignableFrom(method.getDeclaringClass())) {
                        return method.invoke(config, args);
                    }
                    return method.invoke(target, args);
                } catch (Throwable e) {
                    // can be used to set a breakpoint if something goes wrong
                    throw e;
                }
            });
    }

    protected Reference createReference(String value, KeyType idType, KeyElements type) {
        List<Key> keys = new ArrayList<>();
        Key key = new DefaultKey();
        key.setIdType(idType);
        key.setType(type);
        key.setValue(value);
        keys.add(key);
        Reference reference = new DefaultReference();
        reference.setKeys(keys);
        return reference;
    }

    protected Reference createSemanticId(String semanticIdStr) {
        return createReference(semanticIdStr, KeyType.IRDI, KeyElements.CONCEPT_DESCRIPTION);
    }

    protected MappingSpecification createMappingSpecification() {
        this.mapping = new MappingSpecification();
        mapping.setHeader(new Header());

        AssetAdministrationShellEnvironment aasEnv = createTemplate(AssetAdministrationShellEnvironment.class);
        mapping.setAasEnvironmentMapping(aasEnv);
        ((Template) aasEnv).setForeachExpression(new XPathExpr(Arrays.asList(
            new ConstantExpr(
                "//caex:SystemUnitClass[caex:SupportedRoleClass/"
                    + "@RefRoleClassPath='AutomationMLComponentStandardRCL/AutomationComponent']"))));
        BindSpecification bindSpec = new BindSpecification();
        bindSpec.setBinding("idShort", new XPathExpr(Arrays.asList(new ConstantExpr("@Name"))));

        // aasEnv.setIdGeneration(createSimpleIdGeneration(ID_VALUE));
        // aasEnv.setConfigElementId(DEFAULT_CONFIG_ELEMENT_ID);

        AssetAdministrationShell assetShell = createTemplate(AssetAdministrationShell.class);
        bindSpec = new BindSpecification();
        bindSpec.setBinding("idShort", new XPathExpr(Arrays.asList(new ConstantExpr("'ShellIdShort'"))));
        // assetShell.setIdGeneration(createSimpleIdGeneration("5678"));

        aasEnv.setAssetAdministrationShells(Arrays.asList(assetShell));

        AssetInformation assetInformation = createTemplate(AssetInformation.class);
        assetInformation.setGlobalAssetId(createReference("SAMPLE_ID_MUST_COME_FROM_XPATH", KeyType.CUSTOM, KeyElements.ASSET));
        assetShell.setAssetInformation(assetInformation);

        Submodel subModel = createTemplate(Submodel.class);
        ((Template) subModel).setForeachExpression(new XPathExpr(Arrays.asList(new ConstantExpr("caex:Attribute[@Name='MyAttribute']"))));
        subModel.setSemanticId(createSemanticId("mySemanticId"));
        // subModel.setIdGeneration(createSimpleIdGeneration(ID_VALUE + "_submodel"));

        Property submodelElementProperty = createTemplate(Property.class);
        ((Template) subModel)
            .setForeachExpression(new XPathExpr(Arrays.asList(new ConstantExpr("caex:Attribute[@Name='MySubAttribute']"))));
        submodelElementProperty.setValueType("String");

        subModel.setSubmodelElements(Arrays.asList(submodelElementProperty));

        Submodel submodelMultipleAttrs = createTemplate(Submodel.class);
        ((Template) submodelMultipleAttrs).setForeachExpression(new XPathExpr(Arrays.asList(new ConstantExpr("caex:Attribute"))));

        Property submodelElementMultiple = createTemplate(Property.class);
        ((Template) submodelElementMultiple).setForeachExpression(new XPathExpr(Arrays.asList(new ConstantExpr("caex:Attribute"))));
        submodelElementProperty.setValueType("String");

        submodelMultipleAttrs.setSubmodelElements(Arrays.asList(submodelElementMultiple));

        SubmodelElementCollection submodelElementCollection = createTemplate(SubmodelElementCollection.class);
        // ((Template) submodelElementCollection).setForeachExpression(new XPathExpr(Arrays.asList(new
        // ConstantExpr(""));
        submodelElementCollection.setValues(Arrays.asList(submodelElementProperty, submodelElementMultiple));

        aasEnv.setSubmodels(Arrays.asList(subModel, submodelMultipleAttrs));

        return mapping;
    }

    // Create a sample AML document (xml) for use in testing
    protected Document createDocument() {
        Document document = DocumentHelper.createDocument();

        Namespace caexNs = new Namespace("caex", "http://www.dke.de/CAEX");

        Element rootNode = document.addElement(new QName("CAEXFile", caexNs));
        Element unitLib = rootNode.addElement(new QName("SystemUnitClassLib", caexNs))
            .addAttribute("Name", "SystemUnitClassLib");
        // Create a system unit class representing one asset
        unitClass = unitLib.addElement(new QName("SystemUnitClass", caexNs))
            .addAttribute("Name", "MyClass")
            .addAttribute("ID", "MyClassId");
        unitClass.addElement(new QName("SupportedRoleClass", caexNs))
            .addAttribute("RefRoleClassPath", "AutomationMLComponentStandardRCL/AutomationComponent");

        // Create an attribute representing a submodel
        attribute = unitClass.addElement(new QName("Attribute", caexNs))
            .addAttribute("Name", "MyAttribute");
        unitClass.addElement(new QName("Attribute", caexNs))
            .addAttribute("Name", "MyAttribute2");

        // Create an attribute representing a submodel element
        attribute.addElement(new QName("Attribute", caexNs))
            .addAttribute("Name", "MyFirstNotSearchedSubAttribute")
            .addElement(new QName("Value", caexNs)).addText("Not Searched");
        subAttribute = attribute.addElement(new QName("Attribute", caexNs))
            .addAttribute("Name", "MySubAttribute");
        subAttribute.addElement(new QName("Value", caexNs))
            .addText(VALUE_CONTENT);
        subAttribute.addElement(new QName("DefaultValue", caexNs))
            .addText(DEFAULT_VALUE_CONTENT);

        unitLib.addElement(new QName("SystemUnitClass", caexNs))
            .addAttribute("Name", "SecondComponent")
            .addAttribute("ID", "SecondComponentId")
            .addElement(new QName("SupportedRoleClass", caexNs))
            .addAttribute("RefRoleClassPath", "AutomationMLComponentStandardRCL/AutomationComponent");

        unitLib.addElement(new QName("SystemUnitClass", caexNs))
            .addAttribute("Name", "ThirdAssetShortId")
            .addAttribute("ID", "ThirdAssetId")
            .addElement(new QName("SupportedRoleClass", caexNs))
            .addAttribute("RefRoleClassPath", "AutomationMLStandardRCL/Asset");

        return document;
    }

}
