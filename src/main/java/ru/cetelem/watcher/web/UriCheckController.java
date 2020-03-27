package ru.cetelem.watcher.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import ru.cetelem.watcher.service.UriCheckService;

@Component(value = "uriCheckController")
public class UriCheckController {
	private static final Logger log = LoggerFactory.getLogger(UriCheckController.class);

	private String uri;

	@Autowired
	UriCheckService uriCheckService;
	
	public void init() {
		uri = "Input URI here";
	}



	public void check() {
		String checkResult = uriCheckService.check(getUri());
		
        FacesContext context = FacesContext.getCurrentInstance();
        
		if ("ok".equals(checkResult)) {
			context.addMessage("uri-error-message", new FacesMessage(FacesMessage.SEVERITY_INFO, "Info",  checkResult) );
		}
		else {
			context.addMessage("uri-error-message", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",  checkResult) );
		}
	}
	
	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
}
