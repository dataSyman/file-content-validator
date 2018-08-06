package uk.ac.ebi.subs.filecontentvalidator.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.filecontentvalidator.config.CommandLineParams;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@RequiredArgsConstructor
@Component
public class VcfFileValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfFileValidator.class);

    @NonNull
    private CommandLineParams commandLineParams;

    @NonNull
    private SingleValidationResultBuilder singleValidationResultBuilder;

    @Value("${fileContentValidator.vcf.validatorPath}")
    @Setter
    private String validatorPath;

    private static final String ERROR_PREFIX = "Error: ";
    private static final String WARNING_PREFIX = "Warning: ";

    public List<SingleValidationResult> validateFileContent() throws IOException, InterruptedException {

        Path outputDirectory = createOutputDir();
        LOGGER.debug("output will be written to {}", outputDirectory);

        Scanner processOutputScanner = executeVcfValidator(outputDirectory);

        while (processOutputScanner.hasNext()){
            LOGGER.debug("VCF validator output: {}",processOutputScanner.next());
        }

        File summaryFile = findOutputFile(outputDirectory);

        List<SingleValidationResult> results = parseOutputFile(summaryFile);

        summaryFile.delete();

        return results;
    }

    Path createOutputDir() throws IOException {
        Path tempDirectory = Files.createTempDirectory("usi-vcf-validation");
        PosixFileAttributeView attributes = Files.getFileAttributeView(tempDirectory, PosixFileAttributeView.class);
        LOGGER.debug("created dir: {} with attributes: {}", tempDirectory, attributes.readAttributes().permissions());
        tempDirectory.toFile().deleteOnExit();
        return tempDirectory;
    }

    List<SingleValidationResult> parseOutputFile(File summaryFile) throws FileNotFoundException {
        List<SingleValidationResult> results = new ArrayList<>();

        Scanner scanner = new Scanner(summaryFile);
        scanner.useDelimiter("\n");

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.startsWith(ERROR_PREFIX)) {
                String trimmedMessage = line.replace(ERROR_PREFIX, "");
                results.add(
                        singleValidationResultBuilder.buildSingleValidationResultWithErrorStatus(trimmedMessage)
                );
            }
            if (line.startsWith(WARNING_PREFIX)) {
                String trimmedMessage = line.replace(WARNING_PREFIX, "");
                results.add(
                        singleValidationResultBuilder.buildSingleValidationResultWithWarningStatus(trimmedMessage)
                );
            }
        }

        if (results.isEmpty()) {
            results.add(singleValidationResultBuilder.buildSingleValidationResultWithPassStatus());
        }
        LOGGER.debug("results: {}", results);

        return results;
    }

    File findOutputFile(Path outputDirectory) {
        File[] outputFiles = outputDirectory.toFile().listFiles();
        LOGGER.debug("contents of output dir: {}", outputFiles);
        if (outputFiles.length != 1) {
            throw new IllegalStateException();
        }

        return outputFiles[0];
    }

    Scanner executeVcfValidator(Path outputDirectory) throws IOException, InterruptedException {
        String command = vcfValidatorCommandLine(outputDirectory);
        LOGGER.debug("command: {}", command);
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\n");
        return scanner;
    }

    String vcfValidatorCommandLine(Path outputDirectory) {
        return String.join(" ",
                validatorPath,
                "-i", commandLineParams.getFilePath(),
                "-o", outputDirectory.toString(),
                "-r", "summary"
        );
    }
}
