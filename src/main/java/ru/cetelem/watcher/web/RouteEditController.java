package ru.cetelem.watcher.web;

import java.io.IOException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.cetelem.watcher.service.RouteService;
import ru.cetelem.watcher.service.TemplateService;

@Component(value = "routeEditController")
public class RouteEditController {
	private static final Logger log = LoggerFactory.getLogger(RouteEditController.class);
	
	@Autowired
	private RouteService routeLoaderService;
	
	@Autowired
	private SelectedRoute selectedRoute;
	
	@Autowired
	TemplateService templateService;
	

	private String xml;
	private String routeId;

	private boolean isNew = true;

	private String refer = "/";
	
	public String getReferUrl() {
		return refer; 
	}
	
	public boolean getIsNew() {
		return isNew;
	}

	public void init() {
		log.info("init started");
		if (FacesContext.getCurrentInstance().isPostback()){
			return;
		}

		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("pageIndex", "-1");

		refer = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("referer");
		refer = refer==null?"/":refer;
				 
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, String> paramMap = context.getExternalContext().getRequestParameterMap();
		routeId = paramMap.get("routeId");		
		isNew = true;
		
		
		if (routeId!=null) {
			selectedRoute.setSelectedRouteDefinition(routeLoaderService.findById(routeId));
			
			xml = routeLoaderService.getRouteDefinitionAsXml(selectedRoute.getSelectedRouteDefinition());
			routeId = selectedRoute.getSelectedRouteDefinition().getId();

			isNew = false;
		}
		else {
			routeId="route-name";
			xml=templateService.getTemplates().get(paramMap.get("templateId"));
		}
		
		log.info("init started {}", routeId);
	}
	
	
	
	public SelectedRoute getSelectedRoute() {
		return selectedRoute;
	}


	public void setSelectedRoute(SelectedRoute selectedRoute) {
		this.selectedRoute = selectedRoute;
	}
	
	
	public String getXml()  {
		return xml;
	}
	
	public void setXml(String xml) {
		this.xml = xml;
	}	
	
	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}
	
	public String save() throws IOException {
		log.info("save started {}", routeId);

		
		if(isNew && routeLoaderService.findById(routeId)!=null){
			String errorMessage = String.format("Already exists %s", routeId);
			log.error(errorMessage);
	        FacesContext context = FacesContext.getCurrentInstance();
	         
	        context.addMessage("route-id", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",  errorMessage) );
	        return "";
		}
		
		
		
		try {
			routeLoaderService.save(routeId, xml);
		}
		catch(Exception e) {
	        FacesContext context = FacesContext.getCurrentInstance();
	         
	        context.addMessage("route-xml", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",  e.getMessage()) );
	        return "";
		}

    	FacesContext.getCurrentInstance().getExternalContext().
			redirect(getReferUrl());     		
		
		//return "/route-list.xhtml?faces-redirect=true";
    	return "";
	}

	public String cancel() throws IOException {
		log.info("save cancel {}", routeId);
    	FacesContext.getCurrentInstance().getExternalContext().
			redirect(getReferUrl());        		
		//return "/route-list.xhtml?faces-redirect=true";
    	return "";
	}	
}
