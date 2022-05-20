# Importing the AAS Transformation Library

## Usage as a dependency

Currently, builds are exclusively released to GitHub Packages. They can be imported into gradle and maven projects with adjusted versioning, for example:

```xml
<dependency>
    <groupId>com.sap.dsc.aas.lib</groupId>
    <artifactId>aas-transformation-library</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Local Usage

We rely on [SapMachine 11](https://sap.github.io/SapMachine/) and use [Gradle](https://gradle.org/).

You can download and build the repository by yourself by following these steps:

- Clone the GitHub repository:

```sh
        git clone https://github.com/admin-shell-io/aas-transformation-library
```

Use the repository on the commandline. Adjust the name of the jar according to your build.
```sh
$ ./gradlew build

$ java -jar build/distributions/aas-transformation-library-shadow-0.0.1-SNAPSHOT.jar
usage: transform [-a <AML_INPUT_FILE> | -amlx <AMLX_INPUT_FILE> | -ua
       <NODESET_INPUT_FILE> | -xml <GENERIC_INPUT_FILE>]  -c <CONFIG_FILE>
       [-P <PLACEHOLDER_VALUES_JSON>] [-p]
Transform XML file into an AAS structured file

 -a,--aml <AML_INPUT_FILE>                           AML input file
 -amlx,--amlx <AMLX_INPUT_FILE>                      AMLX input file
 -c,--config <CONFIG_FILE>                           Mapping config file
 -P,--placeholder-values <PLACEHOLDER_VALUES_JSON>   Map of placeholder
                                                     values in JSON format
 -p,--print-placeholders                             Print placeholders
                                                     with description
 -ua,--ua <NODESET_INPUT_FILE>                       UA NodeSet input file
 -xml,--xml <GENERIC_INPUT_FILE>                     Generic input file

Missing required option: c


$ java -jar ./build/distributions/aas-transformation-library-shadow-0.0.1-SNAPSHOT.jar -c src/test/resources/config/simpleConfig.json -a src/test/resources/aml/full_AutomationComponent.aml
[main] INFO com.sap.dsc.aas.lib.aml.ConsoleApplication - Loaded config version 1.0.0, aas version 2.0.1
[main] INFO com.sap.dsc.aas.lib.aml.transform.AmlTransformer - Loaded config version 1.0.0, AAS version 2.0.1
[main] INFO com.sap.dsc.aas.lib.aml.transform.AssetAdministrationShellEnvTransformer - Transforming 1 config assets...
[main] INFO com.sap.dsc.aas.lib.aml.ConsoleApplication - Wrote AAS file to full_AutomationComponent.json

$  cd src/test/resources/amlx/minimal_AutomationMLComponent_WithDocuments

$ zip -r minimal_AutomationMLComponent_WithDocuments.amlx . -x "*.DS_Store"
adding: [Content_Types].xml (deflated 52%)
adding: _rels/ (stored 0%)
adding: _rels/.rels (deflated 68%)
adding: lib/ (stored 0%)
adding: lib/AutomationComponentLibrary_v1_0_0_Full_CAEX3_BETA.aml (deflated 85%)
adding: files/ (stored 0%)
adding: files/TestPDFDeviceManual.pdf (deflated 14%)
adding: files/TestTXTDeviceManual.txt (stored 0%)
adding: files/TestTXTWarranty.txt (stored 0%)
adding: CAEX_ClassModel_V.3.0.xsd (deflated 90%)
adding: minimal_AutomationMLComponent_WithDocuments.aml (deflated 80%)

$ cd ../../../../../

$ java -jar ./build/distributions/aas-transformation-library-shadow-0.0.1-SNAPSHOT.jar -c src/test/resources/config/simpleConfig.json -amlx src/test/resources/amlx/minimal_AutomationMLComponent_WithDocuments/minimal_AutomationMLComponent_WithDocuments.amlx
[main] INFO com.sap.dsc.aas.lib.aml.ConsoleApplication - Loaded config version 1.0.0, aas version 2.0.1
[main] INFO com.sap.dsc.aas.lib.aml.transform.AmlTransformer - Loaded config version 1.0.0, AAS version 2.0.1
[main] INFO com.sap.dsc.aas.lib.aml.transform.AssetAdministrationShellEnvTransformer - Transforming 1 config assets...
[main] INFO com.sap.dsc.aas.lib.aml.ConsoleApplication - Wrote AAS file to minimal_AutomationMLComponent_WithDocuments.json
Writing to: minimal_AutomationMLComponent_WithDocuments/files/TestTXTDeviceManual.txt
Writing to: minimal_AutomationMLComponent_WithDocuments/files/TestPDFDeviceManual.pdf
Writing to: minimal_AutomationMLComponent_WithDocuments/files/TestTXTWarranty.txt
```
Output was shortened for increased readability.


## Versioning

We version using **semantic versioning** (e.g., `1.0.4`). The first position indicates the major release. Different major
releases canvas contain breaking changes and are not necessarily compliant. The second number indicates the minor release
or revision, which contains new features compared to an older revision. The last position is used for hotfixes or bugfixes.

Note, that the versioning scheme of this project is not directly aligned with the release process of the metamodel or the java-model library!
When revisions of the meta-model are released, the [java-model](github.com/admin-shell-io/java-model) will change and
those changes will be integrated in this library eventually.