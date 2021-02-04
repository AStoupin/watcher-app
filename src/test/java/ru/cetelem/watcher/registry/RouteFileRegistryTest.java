package ru.cetelem.watcher.registry;


import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringRunner;

import org.xml.sax.SAXException;
import ru.cetelem.watcher.WatcherApp;
import ru.cetelem.watcher.WatcherAppTestConfig;
import ru.cetelem.watcher.service.RouteDefinitionConverter;
import ru.cetelem.watcher.service.RouteService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {WatcherApp.class})
@SpringBootTest
public class RouteFileRegistryTest {
    private static final Logger log = LoggerFactory.getLogger(WatcherAppTestConfig.class);

	@Autowired
    RouteFileRegistry routeFileRegistry;
    @Autowired
    RouteService routeService;

    @Autowired
    RouteDefinitionConverter routeDefinitionConverter;

	@Autowired
	CamelContext camelContext;

	@Test
	@DisplayName("fix id check")
	public void testFixId() {

		assertTrue(routeService.findById("test-route1").getStatus(camelContext).isStarted());
		assertTrue("Error during deduplication routes info",
                routeService.findById("test-route2").getStatus(camelContext).isStarted());

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

        RouteDefinition rd = routeDefinitionConverter.getXmlAsRouteDefinition("route2", route2);
        assertTrue("Id incorrect", "My-File-Transfer12".equals(rd.getId()));

        String fixedXml =  routeDefinitionConverter.routeDefinitionToXml(rd);

        assertTrue("route must has id",
                fixedXml.indexOf("id=")>0);

        assertTrue("no extra id should be in xml",
                fixedXml.indexOf("id=", 160)==-1);


    }


}
