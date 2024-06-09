package com.example.demo;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLParser {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java XMLProcessor <file-path>");
            return;
        }

        File inputFile = new File(args[0]);
        processFile(inputFile);
    }

    public static void processFile(File inputFile) {
        try {
            Map<String, String> tagReplacements = new HashMap<>();
            tagReplacements.put("taskParameters", "TaskParameters");
            tagReplacements.put("taskParameter", "TaskParameter");
            tagReplacements.put("linkedTask", "LinkedTask");
            tagReplacements.put("taskPerformer", "TaskPerformance");
            tagReplacements.put("Role", "Role");
            tagReplacements.put("successor", "successor");
            tagReplacements.put("predecessor", "predecessor");
            tagReplacements.put("linkToPredecessor", "TaskPrecedence");
            tagReplacements.put("linkToSuccessor", "TaskPrecedence");
            tagReplacements.put("WorkProduct", "Product");
            tagReplacements.put("nestedProduct", "Aggregation");
            tagReplacements.put("nestedProducts", "Aggregations");
            tagReplacements.put("impactedProduct", "ProductImpact");
            tagReplacements.put("WorkProducts", "Products");
            tagReplacements.put("taskPerformers", "TaskPerformances");
            tagReplacements.put("impactedProducts", "ProductsImpact");
            tagReplacements.put("Tasks", "Tasks");
            tagReplacements.put("Task", "Task");
            tagReplacements.put("ECPMLModel", "POEMLModel");

            File outputFile = new File(inputFile.getParent(), "output.xml");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputFile);



            //verif_ECPML.validateTags(document);
            performTagReplacements(document.getDocumentElement(), tagReplacements);
            removeUnusedLinkTags(document);
            wrapTaskPrecedences(document);
            replaceProductTagsWithImpactingElements(document);
            replaceProductTagsWithnestedElements(document);


            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(outputFile));

            document.getDocumentElement().normalize();

            // Pretty print the document
            prettyPrintDocument(document, outputFile);


            System.out.println("Processed file saved to " + outputFile.getPath());

        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFile.getPath());
            e.printStackTrace();
        }
    }

    private static void prettyPrintDocument(Document document, File outputFile) {
        try {
            DOMImplementationLS domImplementation = (DOMImplementationLS) document.getImplementation();
            LSSerializer lsSerializer = domImplementation.createLSSerializer();
            lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            LSOutput lsOutput = domImplementation.createLSOutput();
            lsOutput.setEncoding("UTF-8");

            try (FileWriter fileWriter = new FileWriter(outputFile)) {
                lsOutput.setCharacterStream(fileWriter);
                lsSerializer.write(document, lsOutput);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing document", e);
        }
    }

    private static Element findTaskByName(Document document, String taskName) {
        NodeList taskNodes = document.getElementsByTagName("Task");
        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node taskNode = taskNodes.item(i);
            if (taskNode.getNodeType() == Node.ELEMENT_NODE) {
                Element taskElement = (Element) taskNode;
                String nameAttribute = taskElement.getAttribute("name");
                if (nameAttribute.equals(taskName)) {
                    return taskElement;
                }
            }
        }
        return null;
    }

    private static void removeUnusedLinkTags(Document document) {
        String[] tagsToRemove = {"linkToSuccessors", "linkToPredecessors"};

        for (String tag : tagsToRemove) {
            NodeList elements = document.getElementsByTagName(tag);
            while (elements.getLength() > 0) {
                Node node = elements.item(0);
                Node parent = node.getParentNode();

                while (node.hasChildNodes()) {
                    parent.insertBefore(node.getFirstChild(), node);
                }

                parent.removeChild(node);
            }
        }
    }

    private static void wrapTaskPrecedences(Document document) {
        NodeList tasks = document.getElementsByTagName("Task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            wrapPrecedencesForNode(task);
        }
    }

    private static void wrapPrecedencesForNode(Element taskElement) {
        NodeList existingPrecedences = taskElement.getElementsByTagName("TaskPrecedences");
        if (existingPrecedences.getLength() > 0) {
            taskElement.removeChild(existingPrecedences.item(0));
        }

        Document doc = taskElement.getOwnerDocument();
        Element taskPrecedencesElement = doc.createElement("TaskPrecedences");

        NodeList taskPrecedences = taskElement.getElementsByTagName("TaskPrecedence");
        List<Element> precedences = new ArrayList<>();

        for (int i = 0; i < taskPrecedences.getLength(); i++) {
            Element tp = (Element) taskPrecedences.item(i);
            if (tp.getParentNode() == taskElement) {
                precedences.add(tp);
            }
        }

        for (Element tp : precedences) {
            if (tp.hasAttribute("linkKind")) {
                String kindValue = tp.getAttribute("linkKind");
                tp.removeAttribute("linkKind");
                tp.setAttribute("kind", kindValue);
            }
            taskPrecedencesElement.appendChild(tp);
        }

        if (taskPrecedencesElement.hasChildNodes()) {
            taskElement.appendChild(taskPrecedencesElement);
        }
    }

    public static boolean isCollaborativeTask(Element taskElement) {
        if (taskElement == null) {
            return false;
        }

        NodeList performersList = taskElement.getElementsByTagName("taskPerformers");
        return performersList.getLength() > 0;
    }

    private static void replaceProductTagsWithImpactingElements(Document doc) {
        NodeList productList = doc.getElementsByTagName("Product");

        for (int i = 0; i < productList.getLength(); i++) {
            Element productElement = (Element) productList.item(i);
            String productName = productElement.getAttribute("name");

            NodeList productImpactList = productElement.getElementsByTagName("ProductImpact");
            for (int j = 0; j < productImpactList.getLength(); j++) {
                Element productImpactElement = (Element) productImpactList.item(j);

                NodeList productTags = productImpactElement.getElementsByTagName("product");
                List<Node> productsToModify = new ArrayList<>();
                for (int k = 0; k < productTags.getLength(); k++) {
                    productsToModify.add(productTags.item(k));
                }

                for (Node productTag : productsToModify) {
                    if (productTag.getNodeType() == Node.ELEMENT_NODE) {
                        Element impactedElement = doc.createElement("ImpactedElement");

                        NamedNodeMap attributes = productTag.getAttributes();
                        for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {
                            impactedElement.setAttributeNode((Attr) attributes.item(attrIndex).cloneNode(true));
                        }

                        while (productTag.hasChildNodes()) {
                            impactedElement.appendChild(productTag.getFirstChild());
                        }

                        productImpactElement.replaceChild(impactedElement, productTag);
                    }
                }

                Element impactingElement = doc.createElement("ImpactingElement");
                impactingElement.setAttribute("ref", productName);
                productImpactElement.appendChild(impactingElement);
            }
        }
    }

    private static void replaceProductTagsWithnestedElements(Document doc) {
        NodeList productList = doc.getElementsByTagName("Product");

        for (int i = 0; i < productList.getLength(); i++) {
            Element productElement = (Element) productList.item(i);
            String productName = productElement.getAttribute("name");

            NodeList productImpactList = productElement.getElementsByTagName("Aggregation");
            for (int j = 0; j < productImpactList.getLength(); j++) {
                Element productImpactElement = (Element) productImpactList.item(j);

                NodeList productTags = productImpactElement.getElementsByTagName("product");
                List<Node> productsToModify = new ArrayList<>();
                for (int k = 0; k < productTags.getLength(); k++) {
                    productsToModify.add(productTags.item(k));
                }

                for (Node productTag : productsToModify) {
                    if (productTag.getNodeType() == Node.ELEMENT_NODE) {
                        Element impactedElement = doc.createElement("component");

                        NamedNodeMap attributes = productTag.getAttributes();
                        for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {
                            impactedElement.setAttributeNode((Attr) attributes.item(attrIndex).cloneNode(true));
                        }

                        while (productTag.hasChildNodes()) {
                            impactedElement.appendChild(productTag.getFirstChild());
                        }

                        productImpactElement.replaceChild(impactedElement, productTag);
                    }
                }

                Element impactingElement = doc.createElement("aggregate");
                impactingElement.setAttribute("ref", productName);
                productImpactElement.appendChild(impactingElement);
            }
        }
    }

    private static void performTagReplacements(Element element, Map<String, String> tagReplacements) {
        String tagName = element.getTagName();

        if (!tagName.equals("toolDefinition") && tagReplacements.containsKey(tagName)) {
            Document document = element.getOwnerDocument();
            Element newElement = document.createElement(tagReplacements.get(tagName));

            if (tagName.equals("taskPerformer")) {
                newElement.setAttribute("Type", "Performer");

                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        Element childElement = (Element) child;
                        String childTagName = childElement.getTagName();
                        if (childTagName.equals("linkedTask")) {
                            Element translatedChild = document.createElement("performedTask");
                            translatedChild.setAttribute("ref", childElement.getAttribute("ref"));
                            newElement.appendChild(translatedChild);
                        } else if (childTagName.equals("Role")) {
                            Element translatedChild = document.createElement("performer");
                            translatedChild.setAttribute("ref", childElement.getAttribute("ref"));
                            newElement.appendChild(translatedChild);
                        }
                    }
                }
            }

            NodeList useNodes = element.getElementsByTagName("use");
            for (int i = 0; i < useNodes.getLength(); i++) {
                Node useNode = useNodes.item(i);
                if (useNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element useElement = (Element) useNode;
                    String toolRef = useElement.getElementsByTagName("tool").item(0).getAttributes().getNamedItem("ref").getNodeValue();
                    String referencedTaskName = useElement.getElementsByTagName("managedTask").item(0).getAttributes().getNamedItem("ref").getNodeValue();
                    Element referencedTaskElement = findTaskByName(document, referencedTaskName);
                    if (referencedTaskElement != null) {
                        referencedTaskElement.setAttribute("Description", toolRef);
                    }
                }
            }

            if (tagName.equals("Task") && isCollaborativeTask(element)) {
                NamedNodeMap attributes1 = element.getAttributes();
                for (int i = 0; i < attributes1.getLength(); i++) {
                    Attr attr = (Attr) attributes1.item(i);
                    if (!attr.getName().equals("type")) {
                        newElement.setAttribute(attr.getName(), attr.getValue());
                    }
                }
                NodeList taskPerformerNodes = element.getElementsByTagName("taskPerformer");
                int numberOfTaskPerformers = taskPerformerNodes.getLength();
                System.out.println(numberOfTaskPerformers);
                Element sousTasksElement = document.createElement("sous-tasks");
                for (int i = 0; i < taskPerformerNodes.getLength(); i++) {
                    Node taskPerformerNode = taskPerformerNodes.item(i);
                    if (taskPerformerNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element aggregationElement = document.createElement("sous-task");
                        Element taskElement = document.createElement("Task");
                        taskElement.setAttribute("name", element.getAttribute("name") + " instance " + (i + 1));

                        if (element.hasAttribute("Description")) {
                            taskElement.setAttribute("Description", element.getAttribute("Description"));
                        }

                        NamedNodeMap attributes = element.getAttributes();
                        for (int j = 0; j < attributes.getLength(); j++) {
                            Attr attr = (Attr) attributes.item(j);
                            if (!attr.getName().equals("type") && !attr.getName().equals("name")) {
                                taskElement.setAttribute(attr.getName(), attr.getValue());
                            }
                        }

                        NodeList childNodes = element.getChildNodes();
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node childNode = childNodes.item(j);
                            if (!(childNode instanceof Element) || !((Element) childNode).getTagName().equals("taskPerformers")) {
                                Node importedNode = document.importNode(childNode, true);
                                taskElement.appendChild(importedNode);
                            }
                        }

                        Element performerElement = document.createElement("taskPerformer");
                        NodeList performerChildren = taskPerformerNode.getChildNodes();
                        for (int k = 0; k < performerChildren.getLength(); k++) {
                            Node performerChild = performerChildren.item(k);
                            if (performerChild instanceof Element) {
                                Node importedNode = document.importNode(performerChild, true);
                                performerElement.appendChild(importedNode);
                            }
                        }

                        taskElement.appendChild(performerElement);
                        aggregationElement.appendChild(taskElement);
                        sousTasksElement.appendChild(aggregationElement);
                    }
                }
                newElement.appendChild(sousTasksElement);
            } else {
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Attr attr = (Attr) attributes.item(i);
                    if (!tagName.equals("Task") || !attr.getName().equals("type")) {
                        newElement.setAttribute(attr.getName(), attr.getValue());
                    }
                }
                NodeList childNodes = element.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    String childTagName = (childNode instanceof Element) ? ((Element) childNode).getTagName() : null;

                    if (!(childTagName != null && ((childTagName.equals("Role") && element.getTagName().equals("taskPerformer"))
                            || (childTagName.equals("linkedTask") && element.getTagName().equals("taskPerformer"))
                            || (childTagName.equals("toolDefinition") || childTagName.equals("toolsDefinition")
                            || childTagName.equals("use") || childTagName.equals("uses"))))) {
                        Node importedNode = document.importNode(childNode, true);
                        newElement.appendChild(importedNode);
                    }
                }
            }

            element.getParentNode().replaceChild(newElement, element);
            element = newElement;
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                performTagReplacements((Element) child, tagReplacements);
            }
        }
    }
    public static String prettyPrintByTransformer(String xmlString, int indent, boolean ignoreDeclaration) {

        try {
            InputSource src = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, ignoreDeclaration ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
        }
    }
}

