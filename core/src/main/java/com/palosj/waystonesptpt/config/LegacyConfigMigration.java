package com.palosj.waystonesptpt.config;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LegacyConfigMigration {
    private LegacyConfigMigration() {
    }

    public static boolean copyIfCurrentMissing(Path directory, String legacyFileName, String currentFileName)
            throws IOException {
        Path legacyFile = directory.resolve(legacyFileName);
        Path currentFile = directory.resolve(currentFileName);
        if (Files.exists(currentFile) || !Files.isRegularFile(legacyFile)) {
            return false;
        }
        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "." + currentFileName + ".", ".tmp");
        try {
            Files.copy(legacyFile, temporaryFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            try {
                Files.move(temporaryFile, currentFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, currentFile);
            } catch (FileAlreadyExistsException exception) {
                return false;
            }
            return true;
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
