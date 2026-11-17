package ru.mevgeniy.malinaticket.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

final class YamlFiles {
    private final Logger logger;
    private final File backupDirectory;

    YamlFiles(Logger logger, File backupDirectory) {
        this.logger = logger;
        this.backupDirectory = backupDirectory;
    }

    YamlConfiguration load(File file, String description) {
        if (!Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            logger.warning("Ожидался обычный YAML-файл: " + file.getPath());
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            return config;
        } catch (IOException | InvalidConfigurationException exception) {
            logger.log(Level.WARNING, "Не удалось прочитать " + description + ": " + file.getPath(), exception);
            return null;
        }
    }

    boolean saveSafely(YamlConfiguration config, File target, String description) {
        Path temp = null;
        Path backupTemp = null;
        try {
            Path path = target.toPath();
            Files.createDirectories(path.getParent());
            temp = Files.createTempFile(path.getParent(), target.getName(), ".tmp");
            config.save(temp.toFile());
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Ожидался обычный YAML-файл: " + target);
                }
                Path backups = backupDirectory.toPath();
                Files.createDirectories(backups);
                backupTemp = Files.createTempFile(backups, target.getName(), ".tmp");
                Files.copy(path, backupTemp, StandardCopyOption.REPLACE_EXISTING);
                moveReplacing(backupTemp, backupPath(target));
            }
            moveReplacing(temp, path);
            return true;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Не удалось сохранить " + description, exception);
            return false;
        } finally {
            removeTemporaryFile(temp);
            removeTemporaryFile(backupTemp);
        }
    }

    private void removeTemporaryFile(Path temp) {
        if (temp != null) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException exception) {
                logger.warning("Не удалось удалить временный файл: " + temp);
            }
        }
    }

    private Path backupPath(File target) {
        return new File(backupDirectory, target.getName() + ".bak").toPath();
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
