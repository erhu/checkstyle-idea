package org.infernus.idea.checkstyle.ignore;

import com.intellij.openapi.project.Project;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IgnoreHookHelper
 * <p>
 * Created by lisper on 15/11/6.
 */
public class IgnoreHookHelper {

    private static final String PRJ_CONFIG_DIR = ".idea";
    private static final String PRJ_CONFIG_MODULES_FILE = "modules.xml";
    private static final String NODE_MODULE = "module";
    private static final String ATTR_FILE_PATH = "filepath";
    private static final String CHECKSTYLE_IGNORE_FILE = ".checkstyleignore";

    public static IIgnoreHook getHookFromProject(Project project) {
        return new IgnoreHookImpl(checkDirItem(ignoreFiles(project)));
    }

    /**
     * Read All '.checkstyleignore' files to get ignored files.
     */
    private static Set<String> ignoreFiles(Project project) {
        Set<String> ignoredFiles = new HashSet<>();

        // Read '.checkstyleignore' in project's basePath
        String projectIgnoreConfigFile = project.getBasePath() + File.separator + CHECKSTYLE_IGNORE_FILE;
        readIgnoreFile(ignoredFiles, projectIgnoreConfigFile);

        // Read every '.checkstyleignore' in modules
        List<String> modulesIgnoreConfigFiles = configsOfModule(project);
        readIgnoreFile(ignoredFiles, modulesIgnoreConfigFiles);

        return ignoredFiles;
    }

    /**
     * Check ignoredFiles if it's a directory.
     *
     * @param ignoredFiles the ignored files in .checkstyleignore
     */
    private static Set<String> checkDirItem(final Set<String> ignoredFiles) {
        Set<String> result = new HashSet<>();
        ignoredFiles.forEach(it -> {
            File file = new File(it);
            if (file.exists()) {
                if (file.isDirectory()) {
                    try {
                        Files.walkFileTree(Paths.get(it), new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                result.add(file.toFile().getAbsolutePath());
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    result.add(it);
                }
            }
        });
        return result;
    }

    /**
     * Read configFile into ignoredFiles
     *
     * @param ignoredFiles the ignored files in .checkstyleignore
     * @param configFile
     */
    private static void readIgnoreFile(Set<String> ignoredFiles, String configFile) {
        Path path = Paths.get(configFile);
        try {
            Files.readAllLines(path, StandardCharsets.UTF_8)
                    .forEach(s -> ignoredFiles.add(path.toFile().getParent() + File.separator + s.trim()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read configFiles into ignoredFiles
     */
    private static void readIgnoreFile(Set<String> ignoredFiles, List<String> configFiles) {
        configFiles.forEach(it -> readIgnoreFile(ignoredFiles, it));
    }

    /**
     * Get all module's '.checkstyleignore' file's path from {project_dir}/.idea/modules.xml
     */
    private static List<String> configsOfModule(Project project) {
        List<String> modulesPaths = new ArrayList<>();

        final String bathPath = project.getBasePath();
        String modulesConfigFile = bathPath + File.separator + PRJ_CONFIG_DIR + File.separator + PRJ_CONFIG_MODULES_FILE;

        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attr) throws SAXException {
                    if (qName.equalsIgnoreCase(NODE_MODULE)) {
                        if (attr != null) {
                            for (int i = 0; i < attr.getLength(); i++) {
                                if (ATTR_FILE_PATH.equalsIgnoreCase(attr.getQName(i))) {
                                    String path = String.format("%s/%s", modulePath(attr.getValue(i)), CHECKSTYLE_IGNORE_FILE);
                                    modulesPaths.add(path);
                                }
                            }
                        }
                    }
                }

                private String modulePath(String path) {
                    int firstSlashIndex = path.indexOf('/');
                    int lastSlashIndex = path.lastIndexOf('/');
                    if (firstSlashIndex == lastSlashIndex) {
                        return bathPath;
                    }
                    return bathPath + File.separator + path.substring(firstSlashIndex + 1, lastSlashIndex);
                }
            };

            parser.parse(modulesConfigFile, handler);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return modulesPaths;
    }
}
