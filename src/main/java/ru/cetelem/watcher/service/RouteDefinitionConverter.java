package ru.cetelem.watcher.service;

import org.apache.camel.CamelContext;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Component
public class RouteDefinitionConverter {
    private static final Logger log = LoggerFactory.getLogger(RouteDefinitionConverter.class);

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private RouteDefinitionEnricher routeDefinitionEnricher;

    public RoutesDefinition getXmlAsRoutesDefinition(String xml) {
        RoutesDefinition xmlDefinition = null;
        try {
            xmlDefinition = camelContext.loadRoutesDefinition(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception e) {
            log.error("Error during getXmlAsRoutesDefinition ", e);
            throw new RuntimeException(e);
        }
        return xmlDefinition;
    }

    public String routeDefinitionToXml(RouteDefinition routeDefinition){
        String xml = "";
        try {

            routeDefinitionEnricher.cleanExtraRouteDefinition(routeDefinition);

            xml = ModelHelper.dumpModelAsXml(camelContext, routeDefinition);

            xml = prettyXml(xml);

        } catch (JAXBException | ParserConfigurationException | TransformerException | SAXException | IOException e) {
            log.error("Error during getRouteXml ", e);
        }
        return xml;
    }


    private String prettyXml(String xml) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Document document = builder.parse(stream);
        NodeList route = document.getElementsByTagName("route");
        fixElement(route);

        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(domSource, result);

        xml = writer.toString();
        return xml;
    }

    private void fixElement(NodeList route) {
        for (int i = 0; i < route.getLength(); i++) {
            Node routeItem = route.item(i);
            if(routeItem.hasChildNodes())
                fixElement(routeItem.getChildNodes());

            NamedNodeMap routeAttrs = routeItem.getAttributes();
            if(routeAttrs!=null) {
                if(routeAttrs.getNamedItem("customId")!=null)
                    routeAttrs.removeNamedItem("customId");
                if(routeAttrs.getNamedItem("id")!=null)
                    routeAttrs.removeNamedItem("id");

            }
        }
    }
}
