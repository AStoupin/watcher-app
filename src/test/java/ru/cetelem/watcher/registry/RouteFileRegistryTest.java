package ru.cetelem.watcher.registry;


import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringRunner;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import ru.cetelem.watcher.WatcherApp;
import ru.cetelem.watcher.WatcherAppTestConfig;
import ru.cetelem.watcher.registry.RouteFileRegistry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {WatcherApp.class})
@SpringBootTest
public class RouteFileRegistryTest {
    private static final Logger log = LoggerFactory.getLogger(WatcherAppTestConfig.class);

	@Autowired
    RouteFileRegistry routeFileRegistry;
	@Autowired
	CamelContext camelContext;

	@Test
	@DisplayName("fix id check")
	public void testFixId() {
		String route1 ="<route xmlns=\"http://camel.apache.org/schema/spring\" autoStartup=\"false\" customId=\"true\" description=\" asashdljh asljfh lasdj1111111 \" id=\"My-File-Transfer12\">\r\n"
				+ "    <from customId=\"true\" uri=\"file://Temp/2?move=.done&amp;moveFailed=.error&amp;delay=44000&amp;include=.*\\.txt&amp;bridgeErrorHandler=true\"/>\r\n"
				+ "    <choice customId=\"false\" id=\"choice1\">\r\n"
				+ "        <when customId=\"true\" id=\"when1\">\r\n"
				+ "            <simple>${header.CamelFileAbsolutePath} regex '.*[AUD].*\\.*'</simple>\r\n"
				+ "        </when>\r\n"
				+ "        <otherwise customId=\"true\" id=\"otherwise1\">\r\n"
				+ "            <to customId=\"true\" id=\"to3\" uri=\"file://Temp/3\"/>\r\n"
				+ "        </otherwise>\r\n"
				+ "    </choice>\r\n"
				+ "</route>\r\n"
				+ "";
		InputStream stream1 = new ByteArrayInputStream(route1.getBytes(StandardCharsets.UTF_8));

		String route2 ="<route xmlns=\"http://camel.apache.org/schema/spring\" autoStartup=\"false\" customId=\"true\" description=\" asashdljh asljfh lasdj1111111 \" id=\"My-File-Transfer12\">\r\n"
				+ "    <from customId=\"true\" uri=\"file://Temp/2?move=.done&amp;moveFailed=.error&amp;delay=44000&amp;include=.*\\.txt1&amp;bridgeErrorHandler=true\"/>\r\n"
				+ "    <choice customId=\"false\" id=\"choice1\">\r\n"
				+ "        <when customId=\"true\" id=\"when1\">\r\n"
				+ "            <simple>${header.CamelFileAbsolutePath} regex '.*[AUD].*\\.*'</simple>\r\n"
				+ "        </when>\r\n"
				+ "        <otherwise customId=\"true\" id=\"otherwise1\">\r\n"
				+ "            <to customId=\"true\" id=\"to3\" uri=\"file://Temp/4\"/>\r\n"
				+ "        </otherwise>\r\n"
				+ "    </choice>\r\n"
				+ "</route>\r\n"
				+ "";
		InputStream stream2 = new ByteArrayInputStream(route2.getBytes(StandardCharsets.UTF_8));

        routeFileRegistry.loadRouteDefinitionFromStream("routeTest1", true, stream1);
        routeFileRegistry.loadRouteDefinitionFromStream("routeTest2", true, stream2);
		assertTrue(routeFileRegistry.findById("routeTest1").getStatus(camelContext).isStarted());
		assertTrue("Error during deduplication routes info",
                routeFileRegistry.findById("routeTest2").getStatus(camelContext).isStarted());

	}




    @Test
    public void testXmlDom() throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    DocumentBuilder builder = factory.newDocumentBuilder();


        String route2 ="<route xmlns=\"http://camel.apache.org/schema/spring\" autoStartup=\"false\" customId=\"true\" description=\" asashdljh asljfh lasdj1111111 \" id=\"My-File-Transfer12\">\r\n"
                + "    <from customId=\"true\" uri=\"file://Temp/2?move=.done&amp;moveFailed=.error&amp;delay=44000&amp;include=.*\\.txt1&amp;bridgeErrorHandler=true\"/>\r\n"
                + "    <choice customId=\"false\" id=\"choice1\">\r\n"
                + "        <when customId=\"true\" id=\"when1\">\r\n"
                + "            <simple>${header.CamelFileAbsolutePath} regex '.*[AUD].*\\.*'</simple>\r\n"
                + "        </when>\r\n"
                + "        <otherwise customId=\"true\" id=\"otherwise1\">\r\n"
                + "            <to customId=\"true\" id=\"to3\" uri=\"file://Temp/4\"/>\r\n"
                + "        </otherwise>\r\n"
                + "    </choice>\r\n"
                + "</route>\r\n"
                + "";

        InputStream stream2 = new ByteArrayInputStream(route2.getBytes(StandardCharsets.UTF_8));
        Document document = builder.parse(stream2);
        NodeList route = document.getElementsByTagName("route");
        fixElement(route);
        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        log.info(writer.toString());

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
/*            for(int j=0; j < routeItem.getAttributes().getLength(); j++){
                    if("customId".equals(routeItem.getAttributes().item(j).getNodeName())){
                        routeItem.getAttributes().removeNamedItem("customId");
                    }

            }*/

        }
    }

}
