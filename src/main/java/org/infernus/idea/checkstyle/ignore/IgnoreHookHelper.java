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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        List<String> fileListIgnored = new ArrayList<>();

        // Read '.checkstyleignore' in project's basePath
        String projectBasePath = project.getBasePath();
        String configOfProject = projectBasePath + File.separator + CHECKSTYLE_IGNORE_FILE;
        readIgnoreFile(fileListIgnored, configOfProject);

        // Read every '.checkstyleignore' in modules
        List<String> configsOfModule = configsOfModule(project);
        readIgnoreFile(fileListIgnored, configsOfModule);
        return new IgnoreHookImpl(projectBasePath, fileListIgnored);
    }

    /**
     * Read configFile into ignoredFiles
     *
     * @param ignoredFiles
     * @param configFile
     */
    private static void readIgnoreFile(List<String> ignoredFiles, String configFile) {
        Path path = Paths.get(configFile);
        try {
            ignoredFiles.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Read configFiles into ignoredFiles
     */
    private static void readIgnoreFile(List<String> ignoredFiles, List<String> configFiles) {
        for (String path : configFiles) {
            readIgnoreFile(ignoredFiles, path);
        }
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
            // ignore
        }
        return modulesPaths;
    }
}
