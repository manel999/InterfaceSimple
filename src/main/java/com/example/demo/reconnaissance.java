package com.example.demo;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class reconnaissance {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please provide the XML file path as an argument.");
            return;
        }

        String filePath = args[0];
        String result = detectLanguage(filePath);
        System.out.println(result);
    }

    public static String detectLanguage(String filePath) {
        String[] ecpmlTags = {"taskParameters", "taskParameter", "linkedTask", "WorkProduct", "linkToSuccessors",
                "taskPerformer", "taskPerformers", "Role", "Roles", "successor", "predecessor", "linkToPredecessors",
                "linkToPredecessor", "linkToSuccessor", "linkToPredecessor", "ImpactedProduct", "product", "ImpactedProducts",
                "Tasks", "Task", "ECPMLModel", "performer", "toolsDefinition", "toolDefinition", "uses", "use", "managedTask", "tool",
                "WorkProduct", "nestedProducts", "nestedProduct", "impactedProducts", "impactedProduct", "WorkProducts"};
        String[] poemlTags = {"TaskParameters", "TaskParameter", "LinkedTask", "Product",
                "TaskPerformance", "TaskPerformances", "Role", "Roles", "successor", "predecessor", "TaskPrecedence",
                "Aggregation", "Aggregations", "ProductImpact", "product", "Products", "ProductsImpact", "sous-tasks", "sous-task",
                "Tasks", "Task", "POEMLModel", "performer", "performedTask", "TaskPrecedences", "ImpactedElement", "ImpactingElement", "aggregate", "component"};

        Map<String, Integer> tagCounts = new HashMap<>();
        tagCounts.put("ECPML", 0);
        tagCounts.put("POEML", 0);
        tagCounts.put("UNKNOWN", 0);

        Set<String> encounteredTags = new HashSet<>();
        StringBuilder exceptions = new StringBuilder();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    String tagName = qName;
                    boolean found = false;

                    if (isTagInArray(tagName, ecpmlTags)) {
                        tagCounts.put("ECPML", tagCounts.get("ECPML") + 1);
                        found = true;
                    }
                    if (isTagInArray(tagName, poemlTags)) {
                        tagCounts.put("POEML", tagCounts.get("POEML") + 1);
                        found = true;
                    }
                    if (!found) {
                        tagCounts.put("UNKNOWN", tagCounts.get("UNKNOWN") + 1);
                    }
                    encounteredTags.add(tagName);
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    // Log error and continue
                    exceptions.append("Error: ").append(e.getMessage()).append("\n");
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    // Log fatal error and continue
                    exceptions.append("Fatal Error: ").append(e.getMessage()).append("\n");
                    // Skip the erroneous part and continue parsing
                    // This will log and continue but may not correctly skip the content.
                }
            };

            saxParser.parse(new File(filePath), handler);

        } catch (Exception e) {
            exceptions.append("Exception: ").append(e.getMessage()).append("\n");
        }

        String detectedLanguage = getMaxFrequencyKey(tagCounts);
        Set<String> irrelevantTags = new HashSet<>(encounteredTags);
        if (detectedLanguage.equals("ECPML")) {
            for (String tag : ecpmlTags) {
                irrelevantTags.remove(tag);
            }
        } else if (detectedLanguage.equals("POEML")) {
            for (String tag : poemlTags) {
                irrelevantTags.remove(tag);
            }
        }

        String result;
        if (irrelevantTags.isEmpty()) {
            result = "The language detected: " + detectedLanguage + "!\n";
        } else {
            result = "The language detected: " + detectedLanguage + "!\nBut it contains wrong Tags: " + irrelevantTags;
        }

        System.out.println("Tag counts: " + tagCounts);
        System.out.println("Exceptions: " + exceptions.toString());
        System.out.println(result);

        return result;
    }

    private static boolean isTagInArray(String tag, String[] array) {
        for (String element : array) {
            if (element.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    private static String getMaxFrequencyKey(Map<String, Integer> tagCounts) {
        return tagCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
    }
}
