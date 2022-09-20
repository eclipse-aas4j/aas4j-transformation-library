# API

For the end user, there are three relevant classes, all of which extend the [DocumentTransformer Class](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/java/com/sap/dsc/aas/lib/transform/DocumentTransformer.java)

```java
public AssetAdministrationShellEnvironment transform(InputStream inStream, MappingSpecification mapping, Map<String, String> initialVars);
```
This requires three inputs for transformation:
- A source xml-based document as an InputStream (inStream).
- A MappingSpecification Object, that holds a parsed json-config file. This can be obtained from the MappingSpecificationParser:
  `
  new MappingSpecificationParser().loadMappingSpecification(PATH_TO_CONFIG_JSON)
  `
- Optionally, the user can set the parameters declared by the @parameters section in the config file as a Map<String, String>.
  If the config does not define such parameters `null` can be passed which is equivalent to a second transform-method taking only
  the first two arguments.

Depending on what kind of document shall be transformed, different classes should be used:

## AML files
AML files can be transformed by calling the [AmlTransformer](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/java/com/sap/dsc/aas/lib/aml/transform/AmlTransformer.java):

```java
AmlTransformer amlTransformer = new AmlTransformer();
shellEnv = amlTransformer.execute(amlInputStream, mapping);
```
AML file validation includes the following steps (cf. [_AmlValidator.java_](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/java/com/sap/dsc/aas/lib/aml/transform/validation/AmlValidator.java)):
- Check that the AML file is a valid XML file
- Check that the AML file is valid according to the [CAEX 3.0 class model](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/resources/aml/CAEX_ClassModel_V.3.0.xsd)

## AMLX files

AMLX contains a AML-file at its core that needs to be unpackaged first
```java
AmlxPackage amlxPackage = new AmlxPackageReader().readAmlxPackage(Paths.get(amlxInputFileName).toFile());
InputStream amlInputStream = amlxPackage.getRootAmlFile().getInputStream()

```
After that the tranformation is equivalent to that of an AML-file.

AMLX file validation includes the following steps (cf. [_AmlxValidator.java_](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/java/com/sap/dsc/aas/lib/aml/amlx/AmlxValidator.java)):
- Check whether each document defined in */_rels/.rels* exists
- Check whether each file in the AMLX file (a ZIP archive) is defined in */_rels/.rels*
- Check that the root document exists
- Check that there is exactly one root document
- Check that the root document is a valid AML file

## OPC UA Nodeset files
Due to the [UANodeSetTransformer](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/java/com/sap/dsc/aas/lib/ua/transform/UANodeSetTransformer.java)
inheriting methods from the DocumentTransformer, transforming UA is very similar to AML.
```java
UANodeSetTransformer uaTransformer = new UANodeSetTransformer();
shellEnv = uaTransformer.execute(amlInputStream, mapping);
```

The OPC UA Nodeset validation includes the following (cf.
[_UANodeSetSchemaValidator.java_](https://github.com/admin-shell-io/aas-transformation-library/blob/main/src/main/java/com/sap/dsc/aas/lib/ua/transform/validation/UANodeSetSchemaValidator.java)):

- Check that the nodeset xml file is valid xml file.
- Check that the nodeset xml file is valid according
  to [UANodeSet.xsd](https://github.com/OPCFoundation/UA-Nodeset/blob/v1.04/Schema/UANodeSet.xsd) V1.04 schema.

__Please note__: The nodeset [EntType.xml](https://github.com/admin-shell-io/aas-transformation-library/tree/main/src/test/resources/ua/EntType.xml)
is taken from OPC UA information models published by [Equinor](https://github.com/equinor/opc-ua-information-models/tree/test).


### Plain XML files
Plain XML files can be tranformed using the [GenericDocumentTransformer.](https://github.com/admin-shell-io/aas-transformation-library/tree/main/src/main/java/com/sap/dsc/aas/lib/transform/GenericDocumentTransformer.java)
This transformer does not trigger any validation but also prohibits users from accessing meta-model specific [expressions](#expressions)
such as `@caexAttributeName` or `@uaChildren`. 
