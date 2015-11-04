package com.sourcegraph.toolchain.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;

/**
 * File scan utilities
 */
public class ScanUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanUtil.class);

    /**
     * Recursively finds matching files in a given source directory
     * @param rootDir source directory to scan for files
     * @param extensions list of extensions
     * @return list of found java files
     */
    public static List<String> scanFiles(String rootDir, String extensions[]) throws IOException {
        final List<String> files = new LinkedList<>();

        if (Files.exists(Paths.get(rootDir))) {
            Files.walkFileTree(Paths.get(rootDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filename = file.toString();
                    for (String extension : extensions) {
                        if (filename.endsWith(extension)) {
                            filename = PathUtil.normalize(filename);
                            if (filename.startsWith("./"))
                                filename = filename.substring(2);
                            files.add(filename);
                            break;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            LOGGER.warn("{} does not exist, skipping", rootDir);
        }

        return files;
    }
}
