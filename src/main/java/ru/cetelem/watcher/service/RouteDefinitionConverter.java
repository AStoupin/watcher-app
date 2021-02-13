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
import ru.cetelem.watcher.model.UriData;

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

    private String routeTemplate = "<route xmlns=\"http://camel.apache.org/schema/spring\" autoStartup=\"false\" description=\"\" >\n" +
            "    <from uri=\"%s\"/>\n" +
            "    <to uri=\"%s\"/>\n" +
            "</route>";

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private RouteDefinitionEnricher routeDefinitionEnricher;

    public RouteDefinition getXmlAsRouteDefinition(String routeId, String xml) {
        RouteDefinition routeDefinition;
        try {
            String xmlPretty = prettyXml(xml);

            RoutesDefinition xmlDefinition = camelContext.loadRoutesDefinition(new ByteArrayInputStream(xmlPretty.getBytes()));

            if(xmlDefinition.getRoutes().size()==0)
                return null;

            if(xmlDefinition.getRoutes().size()>1){
                log.warn("too mach routes in xml for routeId={}. There are {} routes",
                        routeId,  xmlDefinition.getRoutes().size());
            }


            routeDefinition = xmlDefinition.getRoutes().get(0);

            routeDefinition.setId(routeId);
            routeDefinitionEnricher.cleanExtraRouteDefinition(routeDefinition);
            routeDefinitionEnricher.enrichRouteDefinition(routeId, routeDefinition);

        } catch (Exception e) {
            log.error("Error during getXmlAsRoutesDefinition ", e);
            throw new RuntimeException(e);
        }
        return routeDefinition;
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

    private void fixElement(NodeList routeItems) {
        for (int i = 0; i < routeItems.getLength(); i++) {
            Node routeItem = routeItems.item(i);
            if(routeItem.hasChildNodes())
                fixElement(routeItem.getChildNodes());

            NamedNodeMap routeAttrs = routeItem.getAttributes();
            if(routeAttrs!=null) {
                if(routeAttrs.getNamedItem("customId")!=null)
                    routeAttrs.removeNamedItem("customId");
                if(routeAttrs.getNamedItem("id")!=null && !"route".equals(routeItem.getNodeName()))
                    routeAttrs.removeNamedItem("id");

            }
        }
    }




    public static String getUriBase(UriData uriData){
        String result = "";
        if("local".equals(uriData.getUriType())) {
            result = String.format("file://%s", uriData.getFolder());
            return result;
        }
        if("smb".equals(uriData.getUriType())) {
            result = String.format("%s://%s:%s@%s%s", uriData.getUriType(),
                    uriData.getAccountName(), uriData.getAccountPassword(),
                    uriData.getServerName(), uriData.getFolder());
            return result;
        }

        result  =  String.format("%s://%s@%s%s?password=%s", uriData.getUriType(),
                uriData.getAccountName(), uriData.getServerName(),
                uriData.getFolder(), uriData.getAccountPassword());

        return result;
    }


    public String getRouteXml(UriData uriFrom, UriData uriTo){
        String from = new UriJoiner(getUriBase(uriFrom))
                .add(getUriFlagInfo(uriFrom))
                .add(getUriDelay(uriFrom))
                .add(getUriMask(uriFrom))
                .add(getUriTrasferType(uriFrom))
                .toString();
        String to = new UriJoiner(getUriBase(uriTo))
                .add(getUriFlagInfo(uriTo))
                .add(getUriDelay(uriTo))
                .add(getUriMask(uriTo))
                .add(getUriTrasferType(uriTo))
                .toString();


        return  String.format(routeTemplate, from, to);
    }


    private String getUriFlagInfo(UriData uri) {
        if ("fileAndFlag".equals(uri.getFlagProcess())){
            return String.format("doneFileName=${file:name.noext}%s",uri.getDoneFileName());
        }
        return "";
    }
    private String getUriTrasferType(UriData uri) {
        if ("from".equals(uri.getUriDirection())
                && "moveAndStore".equals(uri.getTransferType())){
            return String.format("move=%s&amp;moveFailed=%s",uri.getMove(), uri.getMoveFailed());
        }
        if ("from".equals(uri.getUriDirection())
                && "moveAndDelete".equals(uri.getTransferType())){
            return String.format("delete=true");
        }
        return "";
    }
    private String getUriDelay(UriData uri) {
        if ("from".equals(uri.getUriDirection())){
            return String.format("delay=%d",Integer.parseInt(uri.getDelay()) * 1000);
        }
        return "";
    }
    private String getUriMask(UriData uri) {
        if ("from".equals(uri.getUriDirection())){
            return String.format("antInclude=%s",uri.getFileMask());
        }
        return "";
    }





    public static class UriJoiner {
        private String uri;

        public UriJoiner(String uri){
            this.uri = uri;
        }

        private UriJoiner add(String str) {
            if ("".equals(str) || str == null) {
                return new UriJoiner(uri);
            }

            if (uri.contains("?"))
                return new UriJoiner(uri + "&amp;" + str);
            else
                return new UriJoiner(uri + "?" + str);
        }

        @Override
        public String toString() {
            return uri;
        }
    }

}
