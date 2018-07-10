package uk.ac.ebi.subs.filecontentvalidator.service;

import lombok.Data;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.filecontentvalidator.config.CommandLineParams;
import uk.ac.ebi.subs.filecontentvalidator.exception.FileNotFoundException;
import uk.ac.ebi.subs.filecontentvalidator.exception.NotSupportedFileTypeException;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;
import uk.ac.ebi.subs.validator.data.structures.SingleValidationResultStatus;
import uk.ac.ebi.subs.validator.data.structures.ValidationAuthor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

@Data
@Service
public class FileContentValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileContentValidator.class);

    @NonNull
    private CommandLineParams commandLineParams;

    private static final String ERROR_RESULT_BEGINNING = "ERROR:";
    private static final String OK_RESULT_BEGINNING = "OK";

    private String validationError;

    @Value("${fileContentValidator.fastQ.validatorPath}")
    private String validatorPath;

    CommandLineParams getCommandLineParams() {
        return this.commandLineParams;
    }

    public SingleValidationResult validateFileContent() throws IOException, InterruptedException {
        validateParameters();

        String output = executeValidationAndGetResult();
        String resultMessage;

        int last = output.lastIndexOf("\n");
        if (last >= 0) {
            resultMessage = output.substring(last, output.length());
            if (resultMessage.contains(ERROR_RESULT_BEGINNING)) {
                validationError = resultMessage.replace(ERROR_RESULT_BEGINNING, "").trim();
            }
        }

        return buildSingleValidationResult();
    }

    String executeValidationAndGetResult() throws IOException, InterruptedException {
        String commandToExecuteValidation = assembleValidatorCommand();

        StringBuilder output = new StringBuilder();

        Runtime rt = Runtime.getRuntime();
        Process process = rt.exec(commandToExecuteValidation);
        process.waitFor();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine())!= null) {
            if (output.length() > 0) {
                output.append("\n");
            }
            output.append(line);
        }

        return output.toString();
    }

    private String assembleValidatorCommand() {
        return String.join(" ", validatorPath, getCommandLineParams().getFilePath());
    }

    boolean validateParameters() {
        validateFileExistence();

        validateFileType();

        return true;
    }

    private void validateFileExistence() {
        File file = new File(getCommandLineParams().getFilePath());
        if(!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException(getCommandLineParams().getFilePath());
        }
    }

    private void validateFileType() {
        if (!FileType.forName(getCommandLineParams().getFileType())) {
            throw new NotSupportedFileTypeException(getCommandLineParams().getFileType());
        }
    }

    SingleValidationResult buildSingleValidationResult() {
        SingleValidationResult singleValidationResult =
                new SingleValidationResult(ValidationAuthor.FileContent, getCommandLineParams().getFileUUID());
        if (validationError != null) {
            singleValidationResult.setMessage(validationError);
            singleValidationResult.setValidationStatus(SingleValidationResultStatus.Error);
        } else {
            singleValidationResult.setValidationStatus(SingleValidationResultStatus.Pass);
        }

        return singleValidationResult;
    }
}