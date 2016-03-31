package org.maruku.example.scannerplugin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by maruku on 31/03/16.
 */
@Mojo(name = "scanner", requiresDependencyResolution = ResolutionScope.TEST)
public class ScannerPlugin extends AbstractMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running SCANNER PLUGIN");
        MavenProject project = (MavenProject)getPluginContext().get("project");
        List runtimeElements;
        try {
            runtimeElements = project.getRuntimeClasspathElements();

            for (Object element : runtimeElements) {
                scanFiles(element);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanFiles(Object element) throws IOException, SAXException, ParserConfigurationException {
        Collection<File> files = FileUtils.listFiles(
                new File(element.toString()),
                new RegexFileFilter("^(.*?\\.xml)"),
                DirectoryFileFilter.DIRECTORY
        );
        for (File f : files) {
            getLog().info(f.getName());
            final Map<String, String> beanMap = extractBeansWithSufix(f, "Service");
            for (String s : beanMap.keySet()) {
                getLog().info("KEY = " + s);
            }
        }
    }

    private Map<String, String> extractBeansWithSufix(File file, String suffix) throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> beanMap = new HashMap<String, String>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);

        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName("bean");
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            NamedNodeMap attributes = node.getAttributes();
            Node classAttr = attributes.getNamedItem("class");
            Node abstractAttr = attributes.getNamedItem("abstract");
            Node idAttr = attributes.getNamedItem("id");
            String classValue = classAttr != null ? classAttr.getNodeValue() : "";
            String abstractValue = abstractAttr != null ? abstractAttr.getNodeValue() : "";
            String idValue = idAttr != null ? idAttr.getNodeValue() : "";

            boolean validBean = abstractValue.equals("") || abstractValue.equals("false");
            validBean &= classValue.endsWith(suffix);
            if (validBean) {
                beanMap.put(idValue, classValue);
            }
        }
        return beanMap;
    }

}
