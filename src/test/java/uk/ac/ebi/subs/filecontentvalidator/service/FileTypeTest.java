package uk.ac.ebi.subs.filecontentvalidator.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class FileTypeTest {

    @Test
    public void whenFileTypeIsNotSupported_thenReturnsFalse() {
        String notSupportedFileType = "jpeg";

        assertFalse(FileType.forName(notSupportedFileType));
    }

    @Test
    public void whenFileTypeIsSupported_ThenReturnsTrue() {
        String supportedFileType = "cram";

        assertTrue(FileType.forName(supportedFileType));
    }
}
