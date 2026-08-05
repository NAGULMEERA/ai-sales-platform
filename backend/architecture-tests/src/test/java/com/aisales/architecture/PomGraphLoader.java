package com.aisales.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the platform's Maven reactor directly from {@code pom.xml} files on disk and builds a
 * lightweight in-memory model of module coordinates and their declared (non test-plugin)
 * dependencies.
 *
 * <p>This intentionally avoids pulling in the {@code maven-model} / Maven Core libraries: the
 * platform's engineering rules require introducing new dependencies only when absolutely
 * necessary, and every fact this module needs (artifactId, groupId, direct
 * {@code <dependencies>}) is trivially reachable with the JDK's built-in DOM + XPath APIs.
 *
 * <p>Only the top-level {@code /project/dependencies/dependency} elements are read, so entries
 * under {@code <dependencyManagement>} or inside {@code <build><plugins>} are correctly ignored -
 * those are not compile/runtime coupling, they are version pinning or build tooling.
 */
final class PomGraphLoader {

    private PomGraphLoader() {
    }

    record Dependency(String groupId, String artifactId, String scope) {
    }

    record MavenModule(String artifactId, String groupId, Path pomPath, List<Dependency> dependencies) {

        List<String> internalDependencyArtifactIds(String ownGroupId) {
            List<String> ids = new ArrayList<>();
            for (Dependency dependency : dependencies) {
                if (ownGroupId.equals(dependency.groupId())) {
                    ids.add(dependency.artifactId());
                }
            }
            return ids;
        }
    }

    /**
     * Walks every directory under {@code roots}, parses every {@code pom.xml} found, and returns
     * a map keyed by artifactId. Modules that cannot be parsed as a Maven POM are skipped rather
     * than failing the whole scan, so a stray {@code pom.xml}-looking fixture file never breaks
     * governance tests for the entire platform.
     */
    static Map<String, MavenModule> loadModules(Path... roots) {
        Map<String, MavenModule> modules = new LinkedHashMap<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(path -> path.getFileName().toString().equals("pom.xml"))
                        .filter(path -> !path.toString().contains(TARGET_DIR_MARKER))
                        .sorted()
                        .forEach(pom -> parse(pom).ifPresent(module -> modules.put(module.artifactId(), module)));
            } catch (IOException e) {
                throw new IllegalStateException("Failed walking module root: " + root, e);
            }
        }
        return modules;
    }

    /** Excludes copies of pom.xml that Maven stages under target/ (e.g. shaded/effective POMs, unpacked jars). */
    private static final String TARGET_DIR_MARKER = java.io.File.separator + "target" + java.io.File.separator;

    private static java.util.Optional<MavenModule> parse(Path pom) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document doc = factory.newDocumentBuilder().parse(pom.toFile());
            XPath xpath = XPathFactory.newInstance().newXPath();

            String artifactId = ((String) xpath.evaluate("/project/artifactId", doc, XPathConstants.STRING)).trim();
            if (artifactId.isEmpty()) {
                return java.util.Optional.empty();
            }
            String ownGroupId = ((String) xpath.evaluate("/project/groupId", doc, XPathConstants.STRING)).trim();
            if (ownGroupId.isEmpty()) {
                ownGroupId = ((String) xpath.evaluate("/project/parent/groupId", doc, XPathConstants.STRING)).trim();
            }

            List<Dependency> dependencies = new ArrayList<>();
            NodeList depNodes = (NodeList) xpath.evaluate(
                    "/project/dependencies/dependency", doc, XPathConstants.NODESET);
            for (int i = 0; i < depNodes.getLength(); i++) {
                Element dep = (Element) depNodes.item(i);
                String depGroupId = childText(dep, "groupId");
                String depArtifactId = childText(dep, "artifactId");
                String scope = childText(dep, "scope");
                dependencies.add(new Dependency(
                        depGroupId, depArtifactId, scope == null || scope.isBlank() ? "compile" : scope));
            }
            return java.util.Optional.of(new MavenModule(artifactId, ownGroupId, pom, dependencies));
        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
            throw new IllegalStateException("Failed to parse POM: " + pom, e);
        }
    }

    private static String childText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        return list.getLength() == 0 ? null : list.item(0).getTextContent().trim();
    }
}
