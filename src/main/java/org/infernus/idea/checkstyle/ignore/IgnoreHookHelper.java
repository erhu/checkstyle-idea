package org.infernus.idea.checkstyle.ignore;

import com.intellij.openapi.project.Project;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IgnoreHookHelper
 * <p>
 * Created by lisper on 15/11/6.
 */
public class IgnoreHookHelper {

    public static IIgnoreHook getHookFromProject(Project project) {
        String basePath = project.getBasePath();
        return new IgnoreHookImpl(basePath, getIgnoredFileList(basePath));
    }

    private static List<String> getIgnoredFileList(String basePath) {
        List<String> ignoredFileList = new ArrayList<>();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                final String keyPath = "match-pattern";
                final String keyIgnore = "include-pattern";

                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("file-match-pattern")) {
                        if (attributes != null) {
                            String filePath = null;
                            boolean ignore = true;
                            for (int i = 0; i < attributes.getLength(); i++) {
                                String key1 = attributes.getQName(i);
                                if (keyPath.equals(key1)) {
                                    filePath = attributes.getValue(key1);
                                } else if (keyIgnore.equals(key1)) {
                                    ignore = !Boolean.parseBoolean(attributes.getValue(key1));
                                }
                            }
                            if (filePath != null && ignore) {
                                ignoredFileList.add(filePath);
                            }
                        }
                    }
                }

                public void endElement(String uri, String localName, String qName) throws SAXException {
                }

                public void characters(char ch[], int start, int length) throws SAXException {
                }

            };

            saxParser.parse(String.format("%s/.checkstyle", basePath), handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return ignoredFileList;
    }
}
