package ru.cetelem.watcher.web;



import java.util.List;
import java.util.Optional;

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

import ru.cetelem.watcher.service.ApplicationSettingsService;
import ru.cetelem.watcher.service.RouteDefinitionExplorer;
import ru.cetelem.watcher.service.RouteService;
import ru.cetelem.watcher.service.RouteStatService;
import ru.cetelem.watcher.service.TemplateService;

import org.primefaces.event.SelectEvent;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSeparator;
import org.primefaces.model.menu.MenuModel;


@Scope(value = "application")
@Component(value = "routeListController")
public class RouteListController {
	private static final Logger log = LoggerFactory.getLogger(RouteListController.class);
	
	@Autowired
	private CamelContext camelContext;

	@Autowired
	private RouteService routeService;
	
	@Autowired
	RouteDefinitionExplorer routeDefinitionExplorer;
	
	@Autowired
	private SelectedRoute selectedRoute;

	@Autowired
	private RouteStatService routeStatService;

	@Autowired
	private TemplateService templateService;
	
	@Autowired
	ApplicationSettingsService applicationSettingsService;
	
	
	private MenuModel templates;
		

	
	
	public RouteListController(){
	
	}


	public void init(){
		FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("pageIndex", "0");
		log.info("routeListController init");
	}
	


    public MenuModel getTemplatesMenu() {
    	if(templates!=null)
        	return templates;    		

 
		templates  = new DefaultMenuModel();
		 
		DefaultMenuItem item = DefaultMenuItem.builder()
	                .value(TemplateService.EMPTY_TEMPLATE_KEY)
	                .url("/route-add/"+TemplateService.EMPTY_TEMPLATE_KEY)
	                .build();
		 templates.getElements().add(item);
		 templates.getElements().add(new DefaultSeparator());
		 
		 
		for (String key: templateService.getTemplates().keySet()) {
			 if(TemplateService.EMPTY_TEMPLATE_KEY.equals(key))
				 continue;
			 
			  item = DefaultMenuItem.builder()
		                .value(key)
		                .url("/route-add/"+key)
		                .build();
			  templates.getElements().add(item);
		}

		return templates;
    }    
	public List<RouteDefinition> getRouteDefinitions(){
    	
    	
    	return routeService.getRoutes();
    }



	List<Route> filteredRoutes;

	public List<Route> getFilteredRoutes(){
		return filteredRoutes;
	}

	public void setFilteredRoutes(List<Route> filteredRoutes){
		this.filteredRoutes = 	filteredRoutes;
	}

	


    public String getRouteStatus(String id) {
		String status = Optional.ofNullable(camelContext.getRouteStatus(id))
				.map(s->s.name())
				.orElse("Loading");
    	log.info("status for {} is {}", id, status);
		return status;
	}

    
    public String getFromInfo(String id) {
    	
		return routeDefinitionExplorer.getFromInfo(routeService.findById(id));
	}    

    
    public String getToInfo(String id) {
    	
		return routeDefinitionExplorer.getToInfo(routeService.findById(id));
	}

	public String getFailuresCount(String id) {

		return String.valueOf(routeStatService.getFailuresById(id));
	}

	public void onRowSelect(SelectEvent event) {
		setSelectedRouteDefinition((RouteDefinition)event.getObject());
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
	    	routeService.save(selectedRoute.getSelectedRouteDefinition());
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
	    	routeService.save(selectedRoute.getSelectedRouteDefinition());
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
    	
    	if (ServiceStatus.Started.name().equals(getRouteStatus(selectedRoute.getSelectedRouteDefinition().getId())) )
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

    
    public void delete() throws Exception {
    	if(selectedRoute.getSelectedRouteDefinition()==null)
    		return;	
    	routeService.delete(selectedRoute.getSelectedRouteDefinition());
    	
    }
    



}