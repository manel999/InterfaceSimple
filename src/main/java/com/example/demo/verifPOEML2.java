package com.example.demo;


import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class verifPOEML2 {

    private static boolean hasErrors;
    private static final String ERROR_FILE_PATH = "C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML\\validation_POEML.txt";

    public static String validateXML(File xmlFile) throws Exception {
        String result = "";
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList tasks = doc.getElementsByTagName("Task");
            checkTasks(tasks);

            Set<String> referencableIds = gatherReferencableIds(doc);

            checkReferences(doc, referencableIds);
            checkTaskParameter(doc);
            checkTaskPerformances(doc);
            checkProducts(doc);
            checkRoles(doc);
            checkKind(doc);
            checkTaskPrecedence(doc);
            validateTags(doc);

            if (!hasErrors) {
                System.out.println("XML file " + xmlFile + " is correct");
                result = "La traduction est CORRECT !";
            }
        } catch (Exception e) {
            hasErrors = true;
            System.err.println("Error detected: " + e.getMessage());
            result = "Le modéle contient des erreurs";
            writeErrorToFile(xmlFile.getName()+" : "+e.getMessage());
        }
        return result;
    }

    private static void writeErrorToFile(String errorMessage) {
        try (FileWriter fw = new FileWriter(ERROR_FILE_PATH, false);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(errorMessage);
        } catch (IOException e) {
            System.err.println("Unable to write error to file: " + e.getMessage());
        }
    }
    public static void validateTags(Document doc) throws Exception {
        // Define the set of allowed tags (array2)
        List<String> allowedTags = List.of("TaskParameters", "TaskParameter", "LinkedTask", "Product",
                "TaskPerformance", "TaskPerformances", "Role", "Roles", "successor", "predecessor", "TaskPrecedence",
                "Aggregation", "Aggregations", "ProductImpact", "product", "Products", "ProductsImpact","sous-tasks","sous-task",
                "Tasks", "Task", "POEMLModel", "performer", "performedTask", "TaskPrecedences","ImpactedElement","ImpactingElement","aggregate","component");

        // Create a list to store all tags found in the document
        List<String> foundTags = new ArrayList<>();

        // Collect all tags from the document
        NodeList allElements = doc.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            foundTags.add(element.getTagName());
        }

        // Check each found tag if it is allowed
        List<String> unknownTags = new ArrayList<>();
        for (String tag : foundTags) {
            if (!allowedTags.contains(tag)) {
                unknownTags.add(tag);
            }
        }

        if (!unknownTags.isEmpty()) {
            throw new Exception("Unknown tags found: " + unknownTags);
        }
    }



    private static void checkTasks(NodeList tasks) throws Exception {
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            if (!task.hasAttribute("name")) {
                throw new Exception("Task element number:"+i+" missing 'name' attribute.");
            }
        }
    }

    //Verifier

    private static void checkTaskParameter(Document doc) throws Exception {
        NodeList taskParameters = doc.getElementsByTagName("TaskParameter");

        for (int i = 0; i < taskParameters.getLength(); i++) {
            Element taskParameter = (Element) taskParameters.item(i);

            // Check for the attribute direction
            if (!taskParameter.hasAttribute("direction")) {
                throw new Exception("TaskParameter element number: " + i + " missing 'direction' attribute.");
            }

            // Retrieve the direction attribute and validate its value
            String direction = taskParameter.getAttribute("direction");
            if (!(direction.equals("in") || direction.equals("out") || direction.equals("inout"))) {
                throw new Exception("Invalid direction value: '" + direction + "' in TaskParameter at index: " + i);
            }

            // Check for exactly one 'LinkedTask' tag
            NodeList linkedTasks = taskParameter.getElementsByTagName("LinkedTask");
            if (linkedTasks.getLength() != 1) {
                throw new Exception("There must be exactly one 'LinkedTask' tag in TaskParameter at index: " + i + ", found: " + linkedTasks.getLength());
            }

            // Check for exactly one 'product' tag
            NodeList products = taskParameter.getElementsByTagName("product");
            if (products.getLength() != 1) {
                throw new Exception("There must be exactly one 'product' tag in TaskParameter at index: " + i + ", found: " + products.getLength());
            }
        }
    }


    private static void checkTaskPerformances(Document doc) throws Exception {
        NodeList taskPerformers = doc.getElementsByTagName("TaskPerformance");

        for (int i = 0; i < taskPerformers.getLength(); i++) {
            Element taskPerformer = (Element) taskPerformers.item(i);
            NodeList roles = taskPerformer.getElementsByTagName("performer");
            NodeList linkedTasks = taskPerformer.getElementsByTagName("performedTask");
            String type = taskPerformer.getAttribute("Type");
            if (!type.equals("Performer") && !type.equals("Assistant")) {
                throw new Exception("TaskPerformer tag does not have the correct 'type' attribute at Task index: " + i);
            }
            System.out.println("TaskPerformance index " + (i+1) + " performer count: " + roles.getLength());
            System.out.println("TaskPerformance index " + (i+1) + " performedTask count: " + linkedTasks.getLength());

            if (roles.getLength() != 1) {
                throw new Exception("There must be exactly one 'Performer' tag in TaskPerformance at index: " + (i + 1) + ", found: " + roles.getLength());
            }
            if (linkedTasks.getLength() != 1) {
                throw new Exception("There must be exactly one 'performedtask' tag in TaskPerformance at index: " + (i + 1) + ", found: " + linkedTasks.getLength());
            }
        }
    }

    private static boolean checkForNonEmptyProduct(NodeList nodeList) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            NodeList products = ((Element) nodeList.item(i)).getElementsByTagName("component");
            for (int j = 0; j < products.getLength(); j++) {
                Element product = (Element) products.item(j);
                if (product.hasAttributes()) {
                    return true; // Found a non-empty product by attribute
                }
            }
        }
        return false;
    }

    private static void checkProducts(Document doc) throws Exception {
        NodeList products = doc.getElementsByTagName("Product");
        for (int i = 0; i < products.getLength(); i++) {
            Element product = (Element) products.item(i);
            if (!product.hasAttribute("name")) {
                throw new Exception("Product tag missing 'name' attribute.");
            }
            boolean isComposite = Boolean.parseBoolean(product.getAttribute("isComposite"));

            // Check for non-empty product tags by attributes, not just any child node
            NodeList aggregations = product.getElementsByTagName("Aggregation");
            NodeList impactedProducts = product.getElementsByTagName("ImpactedProduct");

            boolean hasNonEmptyProduct = checkForNonEmptyProduct(aggregations) || checkForNonEmptyProduct(impactedProducts);

            if ((isComposite && !hasNonEmptyProduct)) {
                throw new Exception("Product composition doesn't exist even though 'isComposite' is true attribute");
            }else if((!isComposite && hasNonEmptyProduct)){
                throw new Exception("Product composition exists even though 'isComposite' is false attribute for product named '" + product.getAttribute("name") + "'.");
            }
        }
    }




    private static void checkRoles(Document doc) throws Exception {
        // Find the 'roles' parent element first
        NodeList rolesList = doc.getElementsByTagName("Roles");
        if (rolesList.getLength() == 0) {
            throw new Exception("No 'roles' element found in the document.");
        }
        for (int i = 0; i < rolesList.getLength(); i++) {
            Node roleNode = rolesList.item(i);
            if (roleNode.getNodeType() == Node.ELEMENT_NODE) {
                Element roleElement = (Element) roleNode;
                NodeList roleElements = roleElement.getElementsByTagName("Role");
                for (int j = 0; j < roleElements.getLength(); j++) {
                    Element role = (Element) roleElements.item(j);
                    if (!role.hasAttribute("name")) {
                        throw new Exception("Role tag missing 'name' attribute within 'roles'.");
                    }
                }
            }
        }
    }

    private static void checkTaskPrecedence(Document doc) throws Exception {
        NodeList taskPrecedences = doc.getElementsByTagName("TaskPrecedence");
        for (int i = 0; i < taskPrecedences.getLength(); i++) {
            Element taskPrecedence = (Element) taskPrecedences.item(i);

            // Check for exactly one 'LinkedTask' tag
            NodeList successor = taskPrecedence.getElementsByTagName("successor");
            if (successor.getLength() != 1) {
                throw new Exception("There must be exactly one 'successor' tag in TaskPrecedence at index: " + i + ", found: " + successor.getLength());
            }

            // Check for exactly one 'direction' tag
            NodeList predecessor = taskPrecedence.getElementsByTagName("predecessor");
            if (predecessor.getLength() != 1) {
                throw new Exception("There must be exactly one 'predecessor' tag in TaskPrecedence at index: " + i + ", found: " + predecessor.getLength());            }

        }
    }


    /*private static void checkDirections(Document doc) throws Exception {
        NodeList taskParameters = doc.getElementsByTagName("TaskParameter");
        for (int i = 0; i < taskParameters.getLength(); i++) {
            Element taskParameter = (Element) taskParameters.item(i);
            NodeList directionNodes = taskParameter.getElementsByTagName("direction");
            if (directionNodes.getLength() == 0) {
                // If there is no <direction> tag at all
                throw new Exception("Missing direction tag in TaskParameter at index: " + i);
            }

            String direction = directionNodes.item(0).getTextContent().trim(); // Trim to avoid whitespace issues
            if (!(direction.equals("in") || direction.equals("out") || direction.equals("inout"))) {
                throw new Exception("Invalid direction value: '" + direction + "' in TaskParameter at index: " + i);
            }
        }
    }*/


    private static void checkKind(Document doc) throws Exception {
        NodeList taskPrecedences = doc.getElementsByTagName("TaskPrecedence");
        for (int i = 0; i < taskPrecedences.getLength(); i++) {
            Element taskPrecedence = (Element) taskPrecedences.item(i);

            // Check if the WorkSequenceKind attribute exists and is not empty
            if (!taskPrecedence.hasAttribute("kind") /*|| taskPrecedence.getAttribute("kind").trim().isEmpty()*/) {
                throw new Exception("Missing or empty kind attribute in TaskPrecedence at index: " + i);
            }

            // Retrieve and trim the WorkSequenceKind attribute
            String workSequenceKind = taskPrecedence.getAttribute("kind").trim();
            if (!(workSequenceKind.equals("FinishToStart") || workSequenceKind.equals("StartToFinish") ||
                    workSequenceKind.equals("FinishToFinish") || workSequenceKind.equals("StartToStart")|| workSequenceKind.equals(""))) {
                throw new Exception("Invalid kind value: '" + workSequenceKind + "' in TaskPrecedence at index: " + i);
            }
        }
    }


    private static Set<String> gatherReferencableIds(Document doc) {
        Set<String> ids = new HashSet<>();
        // Collect all Task names
        NodeList tasks = doc.getElementsByTagName("Task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            if (task.hasAttribute("name")) {
                ids.add(task.getAttribute("name"));
            }
        }
        // Collect all Product names
        NodeList products = doc.getElementsByTagName("Product");
        for (int i = 0; i < products.getLength(); i++) {
            Element product = (Element) products.item(i);
            if (product.hasAttribute("name")) {
                ids.add(product.getAttribute("name"));
            }
        }
        // Collect all Role names
        NodeList roles = doc.getElementsByTagName("Role");
        for (int i = 0; i < roles.getLength(); i++) {
            Element role = (Element) roles.item(i);
            if (role.hasAttribute("name")) {
                ids.add(role.getAttribute("name"));
            }
        }
        return ids;
    }

    private static void checkReferences(Document doc, Set<String> referencableIds) throws Exception {
        // Check all 'ref' attributes in the document to see if they refer to an existing id
        NodeList refElements = doc.getElementsByTagName("*");
        for (int i = 0; i < refElements.getLength(); i++) {
            //System.out.println(refElements.getLength());
            Element element = (Element) refElements.item(i);
            if (element.hasAttribute("ref")) {
                String refValue = element.getAttribute("ref");
                if (!referencableIds.contains(refValue)) {
                    throw new Exception("Reference error:NO element with name '" + refValue + "' found in tag <" + element.getNodeName() + "> at index " + i);
                }
            }
        }
    }
}
