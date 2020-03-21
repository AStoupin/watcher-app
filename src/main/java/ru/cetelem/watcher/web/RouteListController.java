package ru.cetelem.watcher.web;



import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.cetelem.watcher.service.RouteDefinitionExplorer;
import ru.cetelem.watcher.service.RouteService;
import ru.cetelem.watcher.service.RouteStatService;



@Scope(value = "application")
@Component(value = "routeListController")
public class RouteListController {
	private static final Logger log = LoggerFactory.getLogger(RouteListController.class);
	
	@Autowired
	private CamelContext camelContext;

	@Autowired
	private RouteService routeLoaderService;
	
	@Autowired
	RouteDefinitionExplorer routeDefinitionExplorer;
	
	@Autowired
	private SelectedRoute selectedRoute;

	@Autowired
	RouteStatService routeStatService;

	String Version;
	
	public RouteListController(){
	}

	public void init(){

	}
    public String getVersion() {
		return Version;
	}

	public List<RouteDefinition> getRouteDefinitions(){
    	
    	
    	return camelContext.getRouteDefinitions();
    }

    public List<Route> getRoutes(){
    	return camelContext.getRoutes();
    }
    
    
    public ServiceStatus getRouteStatus(String id) {
    	
		return camelContext.getRouteStatus(id);
	}

    
    public String getFromInfo(String id) {
    	
		return routeDefinitionExplorer.getFromInfo(routeLoaderService.findById(id));
	}    

    
    public String getToInfo(String id) {
    	
		return routeDefinitionExplorer.getToInfo(routeLoaderService.findById(id));
	}

	public String getFailuresCount(String id) {

		return String.valueOf(routeStatService.getFailuresById(id));
	}

	public void setSelectedRouteDefinition(RouteDefinition selectedRouteDefinition) {
    	this.selectedRoute.setSelectedRouteDefinition(selectedRouteDefinition);
    }

    public RouteDefinition getSelectedRouteDefinition() {
    	return  this.selectedRoute.getSelectedRouteDefinition();
    }
    
    public void onSelect(RouteDefinition selectedRouteDefinition, String typeOfSelection, String indexes) throws InterruptedException {
    	selectedRoute.setSelectedRouteDefinition(selectedRouteDefinition);
      }
    
     

     
    public void start() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return;	
    	try {
	    	camelContext.startRoute(selectedRoute.getSelectedRouteDefinition().getId());
	    	routeLoaderService.save(selectedRoute.getSelectedRouteDefinition());
    	}
    	catch(Exception e) {
    		log.error(e.getMessage());
	        FacesContext context = FacesContext.getCurrentInstance();
	        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",  e.getMessage()) );
    	}
    	
    }
    
    public void stop() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return;	
    	try {
	    	camelContext.stopRoute(selectedRoute.getSelectedRouteDefinition().getId());
	    	routeLoaderService.save(selectedRoute.getSelectedRouteDefinition());
    	}
    	catch(Exception e) {
    		log.error(e.getMessage());
	        FacesContext context = FacesContext.getCurrentInstance();
	        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",  e.getMessage()) );
    	}
    	
    }

    public void startOrStop() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return;
    	
    	if (ServiceStatus.Started.equals(getRouteStatus(selectedRoute.getSelectedRouteDefinition().getId())) )
    		stop();
    	else
    		start();
    }
    
    public String create() throws Exception {
    	FacesContext.getCurrentInstance().getExternalContext().
			redirect("/route-add");     	
    	
    	//return "/route-edit.xhtml?faces-redirect=true";
		return "";
    }
    
    public String edit() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return "";	
    	FacesContext.getCurrentInstance().getExternalContext().
    		redirect(String.format("/route/%s", selectedRoute.getSelectedRouteDefinition().getId()));     	
    	//return "/route-edit.xhtml?faces-redirect=true&routeId=" + selectedRoute.getSelectedRouteDefinition().getId();
    	return "";
    }

    public void save() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return;	
    	routeLoaderService.save(selectedRoute.getSelectedRouteDefinition());
    	
    }
    
    public void delete() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return;	
    	routeLoaderService.delete(selectedRoute.getSelectedRouteDefinition(), true);
    	
    }
    



}