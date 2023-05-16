/* 
  SPDX-FileCopyrightText: (C)2021 SAP SE or an affiliate company and aas-transformation-library contributors. All rights reserved. 

  SPDX-License-Identifier: Apache-2.0 
 */
package org.eclipse.digitaltwin.aas4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.xmlbeans.impl.common.IOUtil;
import org.eclipse.digitaltwin.aas4j.aml.amlx.AmlxPackage;
import org.eclipse.digitaltwin.aas4j.aml.amlx.AmlxPackagePart;
import org.eclipse.digitaltwin.aas4j.aml.amlx.AmlxPackageReader;
import org.eclipse.digitaltwin.aas4j.aml.transform.AmlTransformer;
import org.eclipse.digitaltwin.aas4j.exceptions.InvalidConfigException;
import org.eclipse.digitaltwin.aas4j.exceptions.TransformationException;
import org.eclipse.digitaltwin.aas4j.mapping.MappingSpecificationParser;
import org.eclipse.digitaltwin.aas4j.mapping.model.Header;
import org.eclipse.digitaltwin.aas4j.mapping.model.MappingSpecification;
import org.eclipse.digitaltwin.aas4j.mapping.model.Parameter;
import org.eclipse.digitaltwin.aas4j.placeholder.exceptions.PlaceholderValueMissingException;
import org.eclipse.digitaltwin.aas4j.transform.GenericDocumentTransformer;
import org.eclipse.digitaltwin.aas4j.transform.validation.PlaceholdersCheck;
import org.eclipse.digitaltwin.aas4j.ua.transform.UANodeSetTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.adminshell.aas.v3.dataformat.Serializer;
import io.adminshell.aas.v3.dataformat.json.JsonSerializer;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;

public class ConsoleApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final MappingSpecificationParser mappingParser;

    private static final String OPTION_NAME_CONFIG = "config";

    private static final String OPTION_NAME_AML_INPUT_FILE = "aml";
    private static final String OPTION_NAME_AMLX_INPUT_FILE = "amlx";
    private static final String OPTION_NAME_NODESET_INPUT_FILE = "ua";
    private static final String OPTION_NAME_GENERIC_INPUT_FILE = "xml";

    private static final String OPTION_NAME_PRINT_PLACEHOLDERS = "print-placeholders";
    private static final String OPTION_NAME_PLACEHOLDER_VALUES = "placeholder-values";

    private CommandLine commandLine;
    protected MappingSpecification mapping;
    private Map<String, String> placeholderMap;

    public ConsoleApplication(CommandLine commandLine) {
        this.commandLine = commandLine;
        this.mappingParser = new MappingSpecificationParser();
    }

    protected void loadConfig() throws IOException {
        loadConfig(commandLine.getOptionValue(OPTION_NAME_CONFIG));
    }

    protected void loadConfig(String configFileName) throws IOException {
        mapping = this.mappingParser.loadMappingSpecification(configFileName);
    }

    protected AssetAdministrationShellEnvironment transformAmlFile(String amlFilePath)
        throws IOException, TransformationException {
        try (InputStream amlStream = Files.newInputStream(Paths.get(amlFilePath))) {
            logHeaderInfo();
            return new AmlTransformer().execute(amlStream, mapping, placeholderMap);
        }
    }

    protected AssetAdministrationShellEnvironment transformAml(InputStream amlStream)
        throws IOException, TransformationException {
        logHeaderInfo();
        return new AmlTransformer().execute(amlStream, mapping, placeholderMap);
    }

    private AssetAdministrationShellEnvironment transformAmlx(String amlxInputFileName)
        throws TransformationException, IOException {
        AmlxPackage amlxPackage = new AmlxPackageReader().readAmlxPackage(Paths.get(amlxInputFileName).toFile());
        try (InputStream amlInputStream = amlxPackage.getRootAmlFile().getInputStream()) {
            logHeaderInfo();
            return transformAml(amlInputStream);
        }
    }

    private AssetAdministrationShellEnvironment transformNodeSet(String nodesetInputFileName) throws IOException, TransformationException {
        try (InputStream nodesetStream = Files.newInputStream(Paths.get(nodesetInputFileName))) {
            logHeaderInfo();
            return new UANodeSetTransformer().execute(nodesetStream, mapping, placeholderMap);
        }
    }

    private AssetAdministrationShellEnvironment transformGeneric(String genericInputFileName) throws IOException, TransformationException {
        try (InputStream genericStream = Files.newInputStream(Paths.get(genericInputFileName))) {
            logHeaderInfo();
            return new GenericDocumentTransformer().execute(genericStream, mapping, placeholderMap);
        }
    }

    private void logHeaderInfo() {
        // fix missing header
        if (mapping.getHeader() == null) {
            mapping.setHeader(new Header());
        }
        LOGGER.info("Loaded config version {}, aas version {}", mapping.getHeader().getVersion(), mapping.getHeader().getAasVersion());
    }

    private void writeAasToFile(String aasOutputFileName, AssetAdministrationShellEnvironment aasEnv) {
        try (OutputStream fileOutputStream = Files.newOutputStream(Paths.get(aasOutputFileName))) {
            Serializer serializer = new JsonSerializer();
            fileOutputStream.write(serializer.write(aasEnv).getBytes());
            fileOutputStream.flush();
            LOGGER.info("Wrote AAS file to {}", aasOutputFileName);
        } catch (Exception e) {
            LOGGER.error("Writing AAS file failed!", e);
        }
    }

    protected String deriveOutputFileName(String inputFileName) {
        return com.google.common.io.Files.getNameWithoutExtension(inputFileName) + ".json";
    }

    @SuppressWarnings("unchecked")
    protected void readPlaceholders() {
        if (commandLine.hasOption(OPTION_NAME_PLACEHOLDER_VALUES)) {
            LOGGER.info("Reading placeholder values for AAS transformation");
            ObjectMapper mapper = new ObjectMapper();
            try {
                placeholderMap = mapper
                    .readValue(commandLine.getOptionValue(OPTION_NAME_PLACEHOLDER_VALUES), Map.class);
                new PlaceholdersCheck(mapping.getHeader().getParameters(), placeholderMap).execute();
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
                LOGGER.error("Failed to read placeholders, continuing with orginial AAS...");
            } catch (PlaceholderValueMissingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static void main(String[] args) {
        final Options options = new Options();

        options.addOption(Option.builder("c").desc("Mapping config file").longOpt(OPTION_NAME_CONFIG).hasArg()
            .argName("CONFIG_FILE").required().build());

        options.addOption(Option.builder("P").desc("Map of placeholder values in JSON format")
            .longOpt(OPTION_NAME_PLACEHOLDER_VALUES).hasArg().argName("PLACEHOLDER_VALUES_JSON").build());

        OptionGroup optionGroup = new OptionGroup();
        optionGroup.addOption(Option.builder("a").desc("AML input file").longOpt(OPTION_NAME_AML_INPUT_FILE).hasArg()
            .argName("AML_INPUT_FILE").build());
        optionGroup.addOption(Option.builder("amlx").desc("AMLX input file").longOpt(OPTION_NAME_AMLX_INPUT_FILE)
            .hasArg().argName("AMLX_INPUT_FILE").build());
        optionGroup.addOption(Option.builder("ua").desc("UA NodeSet input file").longOpt(OPTION_NAME_NODESET_INPUT_FILE)
            .hasArg().argName("NODESET_INPUT_FILE").build());
        optionGroup.addOption(Option.builder("xml").desc("Generic input file").longOpt(OPTION_NAME_GENERIC_INPUT_FILE)
            .hasArg().argName("GENERIC_INPUT_FILE").build());

        options.addOptionGroup(optionGroup);

        options.addOption(Option.builder("p").desc("Print placeholders with description")
            .longOpt(OPTION_NAME_PRINT_PLACEHOLDERS).build());

        final CommandLineParser parser = new DefaultParser();
        ConsoleApplication application = null;
        try {
            CommandLine commandLine = parser.parse(options, args);
            application = new ConsoleApplication(commandLine);
        } catch (ParseException e) {
            final String header = "Transform XML file into an AAS structured file\n\n";
            final String footer = "\n" + e.getMessage();
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("transform", header, options, footer, true);
            return;
        }

        try {
            application.loadConfig();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        application.printPlaceholders();
        application.readPlaceholders();

        AssetAdministrationShellEnvironment intermediateAAS = application.transform();
        application.writeAasToFile(intermediateAAS);

    }

    private void writeAasToFile(AssetAdministrationShellEnvironment aasEnvReplaced) {
        try {
            String aasOutputFileName = null;
            if (commandLine.hasOption(OPTION_NAME_AMLX_INPUT_FILE)) {
                String amlxInputFileName = commandLine.getOptionValue(OPTION_NAME_AMLX_INPUT_FILE);
                aasOutputFileName = deriveOutputFileName(amlxInputFileName);
                writeAasToFile(aasOutputFileName, aasEnvReplaced);
                copyDocumentsFromAMLX(amlxInputFileName);
            } else if (commandLine.hasOption(OPTION_NAME_AML_INPUT_FILE)) {
                String amlInputFileName = commandLine.getOptionValue(OPTION_NAME_AML_INPUT_FILE);
                aasOutputFileName = deriveOutputFileName(amlInputFileName);
                writeAasToFile(aasOutputFileName, aasEnvReplaced);
            } else if (commandLine.hasOption(OPTION_NAME_NODESET_INPUT_FILE)) {
                String nodesetInputFileName = commandLine.getOptionValue(OPTION_NAME_NODESET_INPUT_FILE);
                aasOutputFileName = deriveOutputFileName(nodesetInputFileName);
                writeAasToFile(aasOutputFileName, aasEnvReplaced);
            } else if (commandLine.hasOption(OPTION_NAME_GENERIC_INPUT_FILE)) {
                String genericInputFileName = commandLine.getOptionValue(OPTION_NAME_GENERIC_INPUT_FILE);
                aasOutputFileName = deriveOutputFileName(genericInputFileName);
                writeAasToFile(aasOutputFileName, aasEnvReplaced);
            }
        } catch (InvalidConfigException | TransformationException | IOException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    private void copyDocumentsFromAMLX(String amlxInputFileName) throws TransformationException, IOException {
        String amlxDir = com.google.common.io.Files.getNameWithoutExtension(amlxInputFileName);
        AmlxPackage amlxPackage = new AmlxPackageReader().readAmlxPackage(Paths.get(amlxInputFileName).toFile());

        for (AmlxPackagePart packagePart : amlxPackage.getNonAmlFiles()) {
            Path pathToFile = Paths.get(amlxDir, packagePart.getPathInAmlx());
            LOGGER.info("Writing to: {}", pathToFile);

            Files.createDirectories(pathToFile.getParent());
            try (InputStream packagePartStream = packagePart.getInputStream();
                OutputStream partOutputStream = Files.newOutputStream(pathToFile)) {
                IOUtil.copyCompletely(packagePartStream, partOutputStream);
            }
        }
    }

    private AssetAdministrationShellEnvironment transform() {
        try {
            if (commandLine.hasOption(OPTION_NAME_AMLX_INPUT_FILE)) {
                String amlxInputFileName = commandLine.getOptionValue(OPTION_NAME_AMLX_INPUT_FILE);
                return transformAmlx(amlxInputFileName);
            } else if (commandLine.hasOption(OPTION_NAME_AML_INPUT_FILE)) {
                String amlInputFileName = commandLine.getOptionValue(OPTION_NAME_AML_INPUT_FILE);
                return transformAmlFile(amlInputFileName);
            } else if (commandLine.hasOption(OPTION_NAME_NODESET_INPUT_FILE)) {
                String nodesetInputFileName = commandLine.getOptionValue(OPTION_NAME_NODESET_INPUT_FILE);
                return transformNodeSet(nodesetInputFileName);
            } else if (commandLine.hasOption(OPTION_NAME_GENERIC_INPUT_FILE)) {
                String genericInputFileName = commandLine.getOptionValue(OPTION_NAME_GENERIC_INPUT_FILE);
                return transformGeneric(genericInputFileName);
            }
        } catch (IOException | TransformationException | InvalidConfigException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        LOGGER.error("No Transformation executed!");
        return null;
    }

    private void printPlaceholders() {
        if (commandLine.hasOption(OPTION_NAME_PRINT_PLACEHOLDERS)) {
            logHeaderInfo();
            List<Parameter> placeholders = mapping.getHeader().getParameters();

            LOGGER.info("{} placeholders are expected according to header:", placeholders.size());
            placeholders
                .forEach(placeholder -> LOGGER.info("{}: {}", placeholder.getName(), placeholder.getDescription()));
        }
    }

}
