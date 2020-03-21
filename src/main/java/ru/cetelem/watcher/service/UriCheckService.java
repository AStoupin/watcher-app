package ru.cetelem.watcher.service;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class UriCheckService {
	private static final Logger log = LoggerFactory.getLogger(UriCheckService.class);
	
	@Autowired
	CamelContext camelContext;

	public static class TestRouteBuilder extends RouteBuilder {
		private String uri;
		private ErrorProcessor errorProcessor;
		public TestRouteBuilder(String uri) {
			this.uri = uri; 
			errorProcessor = new ErrorProcessor();
		}

		@Override
		public void configure() throws Exception {
		   onException(Exception.class).process(errorProcessor);
		   
		   RouteDefinition rb = from("direct:start").to(uri);
		   rb.setId("testRoute");
		   rb.setAutoStartup("true");
		   
		}
	}

	public static class ErrorProcessor implements Processor{
		 private String errorText;
		 public void process(Exchange exchange) throws Exception {
			 errorText = exchange.getProperty(Exchange.EXCEPTION_CAUGHT).toString();
		       
		  }
	}	
	
	public String check(String uri) {
		log.info("Check start for uri = {}", uri );
		String errorMessage;
		TestRouteBuilder testRouteBuilder = null;

		try {
			testRouteBuilder = new TestRouteBuilder(uri); 
			camelContext.addRoutes(testRouteBuilder);
			
			ProducerTemplate template = camelContext.createProducerTemplate();
			template.sendBody("direct:start", "Hello, world!");

			
		} catch (Exception e) {
			log.error("check error {}", uri);
			errorMessage = e.getMessage();
			if(testRouteBuilder!= null)
				errorMessage = String.format("%s \n\r %s", errorMessage, testRouteBuilder.errorProcessor.errorText);  
			
			log.info("Check finish fails for uri = {} with error ",uri, errorMessage );
	        
	        return errorMessage;
		}
		finally {
			try {
				camelContext.removeRoute("testRoute");
				camelContext.removeRouteDefinition(camelContext.getRouteDefinition("testRoute"));						
			} catch (Exception e) {
			}
		}

		log.info("Check finish ok for uri = {} ", uri);		
		errorMessage = "ok";
		return errorMessage;

	}
}
