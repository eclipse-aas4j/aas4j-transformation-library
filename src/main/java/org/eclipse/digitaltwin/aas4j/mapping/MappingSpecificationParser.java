/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j.mapping;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.module.SimpleValueInstantiators;
import org.eclipse.digitaltwin.aas4j.exceptions.InvalidBindingException;
import org.eclipse.digitaltwin.aas4j.expressions.Expression;
import org.eclipse.digitaltwin.aas4j.mapping.jackson.BindingSpecificationDeserializer;
import org.eclipse.digitaltwin.aas4j.mapping.jackson.ExpressionDeserializer;
import org.eclipse.digitaltwin.aas4j.mapping.model.BindSpecification;
import org.eclipse.digitaltwin.aas4j.mapping.model.LangStringTemplate;
import org.eclipse.digitaltwin.aas4j.mapping.model.MappingSpecification;
import org.eclipse.digitaltwin.aas4j.mapping.model.Template;
import org.eclipse.digitaltwin.aas4j.mapping.model.TemplateSupport;

import io.adminshell.aas.v3.dataformat.core.ReflectionHelper;
import io.adminshell.aas.v3.dataformat.core.deserialization.EmbeddedDataSpecificationDeserializer;
import io.adminshell.aas.v3.dataformat.core.deserialization.EnumDeserializer;
import io.adminshell.aas.v3.dataformat.json.ReflectionAnnotationIntrospector;
import io.adminshell.aas.v3.model.EmbeddedDataSpecification;
import io.adminshell.aas.v3.model.LangString;

/**
 * Class for parsing mapping specifications containing AAS JSON templates.
 */
public class MappingSpecificationParser {

	private static Map<Class<?>, com.fasterxml.jackson.databind.JsonDeserializer> customDeserializers = Map.of(
        EmbeddedDataSpecification.class, new EmbeddedDataSpecificationDeserializer(),
        BindSpecification.class, new BindingSpecificationDeserializer(),
        Expression.class, new ExpressionDeserializer());

	private JsonMapper mapper;
	private SimpleAbstractTypeResolver typeResolver;

    public MappingSpecificationParser() {
        initTypeResolver();
        buildMapper();
    }

    public static Optional<Method> getMethod(Class<?> clazz, String name, Class<?>... arg) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(m -> name.equals(m.getName()) && Arrays.equals(m.getParameterTypes(), arg))
            .findAny();
    }

    public MappingSpecification loadMappingSpecification(String filePath) throws IOException {
        JsonParser parser = mapper.getFactory().createParser(new File(filePath));
        JsonParserDelegate wrapper = new JsonParserDelegate(parser) {
            boolean skipObject = false;

            @Override
            public JsonToken nextToken() throws IOException {
                if (skipObject) {
                    skipObject = false;
                    // skip the current object in case of { "modelType: { "name" : "TheType" } }
                    while (super.nextToken() != JsonToken.END_OBJECT) {
                        super.nextToken();
                    }
                }
                // handle case { "modelType: { "name" : "TheType" } }
                if (super.currentToken() == JsonToken.FIELD_NAME && "modelType".equals(getText())) {
                    JsonToken modelTypeValue = super.nextToken();
                    if (modelTypeValue == JsonToken.START_OBJECT) {
                        modelTypeValue = super.nextValue();
                        skipObject = true;
                    }
                    return modelTypeValue;
                }
                return super.nextToken();
            }
        };
        return mapper.readValue(wrapper, MappingSpecification.class);
    }

    private void buildMapper() {
        mapper = JsonMapper.builder()
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            // fail on unknown properties for now
            // .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .annotationIntrospector(new ReflectionAnnotationIntrospector() {
                public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config, AnnotatedClass ac, JavaType baseType) {
                    if (ReflectionHelper.SUBTYPES.containsKey(ac.getRawType())) {
                        TypeResolverBuilder<?> result = _constructStdTypeResolverBuilder();
                        result = result.init(JsonTypeInfo.Id.NAME, null);
                        result.inclusion(JsonTypeInfo.As.PROPERTY);
                        result.typeProperty("modelType");
                        result.typeIdVisibility(false);
                        return result;
                    }
                    // for all other types null must be returned
                    return null;
                }
            })
            // disabled for now until camel case enums are used
            .addModule(buildEnumModule())
            .addModule(buildImplementationModule())
            .addModule(buildCustomDeserializerModule())
            .build();
        ReflectionHelper.JSON_MIXINS.entrySet().forEach(x -> {
            mapper.addMixIn(x.getKey(), x.getValue());
        });
    }

    private SimpleModule buildCustomDeserializerModule() {
        SimpleModule module = new SimpleModule();
        customDeserializers.forEach(module::addDeserializer);
        return module;
    }

    private void initTypeResolver() {
        typeResolver = new SimpleAbstractTypeResolver();
        ReflectionHelper.DEFAULT_IMPLEMENTATIONS.stream()
            .filter(info -> !customDeserializers.containsKey(info.getInterfaceType()))
            .forEach(x -> typeResolver.addMapping(x.getInterfaceType(), x.getImplementationType()));
    }

    private SimpleModule buildEnumModule() {
        SimpleModule module = new SimpleModule();
        ReflectionHelper.ENUMS.forEach(x -> module.addDeserializer(x, new EnumDeserializer<>(x)));
        return module;
    }

    private SimpleModule buildImplementationModule() {
        SimpleModule module = new SimpleModule();
        // module.setAbstractTypes(typeResolver);
        module.setValueInstantiators(new SimpleValueInstantiators() {
            @Override
            public ValueInstantiator findValueInstantiator(DeserializationConfig config, BeanDescription beanDesc,
                ValueInstantiator defaultInstantiator) {
                // LangString class
                if (LangString.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
                    return new ValueInstantiator.Delegating(defaultInstantiator) {
                        @Override
                        public boolean canInstantiate() {
                            return true;
                        }

                        @Override
                        public boolean canCreateUsingDefault() {
                            return true;
                        }

                        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
                            return new LangStringTemplate();
                        }
                    };
                    // support model interfaces
                } else if (ReflectionHelper.isModelInterface(beanDesc.getType().getRawClass())) {
                    JavaType modelType = typeResolver.findTypeMapping(config, beanDesc.getType());
                    return new ValueInstantiator.Delegating(defaultInstantiator) {
                        @Override
                        public boolean canInstantiate() {
                            return true;
                        }

                        @Override
                        public boolean canCreateUsingDefault() {
                            return true;
                        }

                        public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
                            Object target;
                            try {
                                target = modelType.getRawClass().getDeclaredConstructor().newInstance();
                            } catch (Exception e) {
                                throw new IOException(e);
                            }
                            if (target instanceof Template) {
                                // config interface is directly implemented
                                return target;
                            } else {
                                // create a proxy instance that implements the bean interface and the config interface
                                List<Class<?>> interfaces = new ArrayList<>();
                                interfaces.addAll(Arrays.asList(target.getClass().getInterfaces()));
                                interfaces.add(Template.class);
                                Template config = new TemplateSupport(target);
                                return Proxy.newProxyInstance(getClass().getClassLoader(),
                                    interfaces.toArray(new Class<?>[interfaces.size()]),
                                    (o, method, args) -> {
                                        try {
                                            // route to concrete object - either template definition or the underlying bean
                                            if (Template.class.isAssignableFrom(method.getDeclaringClass())) {
                                                if (method.getParameterTypes().length == 1 &&
                                                    BindSpecification.class.isAssignableFrom(method.getParameterTypes()[0])) {
                                                    // validate bind specification
                                                    BindSpecification bindSpec = (BindSpecification) args[0];
                                                    Set<String> knownProperties = ctxt.getConfig().introspect(modelType)
                                                        .findProperties()
                                                        .stream().map(p -> p.getName()).collect(Collectors.toSet());
                                                    Set<String> boundProperties = new HashSet<>(bindSpec.getBindings().keySet());
                                                    boundProperties.removeAll(knownProperties);
                                                    if (!boundProperties.isEmpty()) {
                                                        throw new InvalidBindingException(boundProperties);
                                                    }
                                                }
                                                return method.invoke(config, args);
                                            }
                                            return method.invoke(target, args);
                                        } catch (Throwable e) {
                                            // can be used to set a breakpoint if something goes wrong
                                            throw e;
                                        }
                                    });
                            }
                        }
                    };
                }
                return defaultInstantiator;
            }
        });
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public List<BeanPropertyDefinition> updateProperties(DeserializationConfig config, BeanDescription beanDesc,
                List<BeanPropertyDefinition> propDefs) {
                // include all config properties
                if (!Template.class.isAssignableFrom(beanDesc.getBeanClass()) &&
                    (ReflectionHelper.isModelInterfaceOrDefaultImplementation(beanDesc.getBeanClass()) ||
                        LangString.class.isAssignableFrom(beanDesc.getBeanClass()))) {
                    Set<String> existingProps = propDefs.stream().map(propDef -> propDef.getName()).collect(Collectors.toSet());
                    List<BeanPropertyDefinition> compoundDefs = new ArrayList<>(propDefs);
                    compoundDefs.addAll(
                        config.introspect(config.getTypeFactory().constructSimpleType(Template.class, null))
                            .findProperties().stream()
                            // filter properties that are already contained in base bean interface
                            .filter(propDef -> !existingProps.contains(propDef.getName())).collect(Collectors.toList()));
                    propDefs = compoundDefs;
                }
                return super.updateProperties(config, beanDesc, propDefs);
            }
        });
        return module;
    }
}
