package ru.cetelem.watcher.web;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.time.DateUtils;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.cetelem.watcher.model.RouteStatItem;
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
	RouteDefinitionExplorer routeDefinitionInformer;
	
	@Autowired
	private SelectedRoute selectedRoute;
	@Autowired
	RouteStatService routeStatService;

	String Version;
	
	public RouteListController(){
	}
	
    public String getVersion() {
		return Version;
	}

	public List<RouteDefinition> getRouteDefinitions(){
    	
    	
    	return camelContext.getRouteDefinitions();
    }

    public List<Route> getRoutes(){
    	//getRoutes().get(0).getEndpoint().toString()
    	//getRoutes().get(0).getConsumer().toString()
    	//getRoutes().get(0).getEndpoint().
    	return camelContext.getRoutes();
    }
    
    
    public ServiceStatus getRouteStatus(String id) {
    	
		return camelContext.getRouteStatus(id);
	}

    
    public String getFromInfo(String id) {
    	
		return routeDefinitionInformer.getFromInfo(routeLoaderService.findById(id));
	}    

    
    public String getToInfo(String id) {
    	
		return routeDefinitionInformer.getToInfo(routeLoaderService.findById(id));
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
    

    private TimelineModel model;  
    private LocalDateTime start;
    private LocalDateTime end;
    
    public TimelineModel getModel() {  
        return model;  
    }  
   
    public Date getStart() {
        return asDate(start);  
    }  
   
    public Date getEnd() {
        return asDate(end);  
    }
    
    public void init() {  
        // set initial start / end dates for the axis of the timeline  
        start = LocalDateTime.now().minusHours(2);
        end = LocalDateTime.now().plusHours(2);
 

   
        // create timeline model  
        model = new TimelineModel();
   
        for (RouteDefinition routeDefinition : camelContext.getRouteDefinitions()) {
            LocalDateTime end = LocalDateTime.now().minusHours(12).withMinute(0).withSecond(0).withNano(0);
            List<RouteStatItem> routeStatItems = routeStatService.getStatListById(routeDefinition.getId());
            for (RouteStatItem routeStatItem : routeStatItems) {
                LocalDateTime start = routeStatItem.getEventDate();
                end = start.plusMinutes(1);
 
                  
                String availability = routeStatItem.isSuccessed() ? "ok" : "ko";  
                String successedClass = routeStatItem.isSuccessed() ? "ok-class" : "ko-class";
                
                // create an event with content, start / end dates, editable flag, group name and custom style class  
                TimelineEvent event = new TimelineEvent(routeStatItem.getFileName(), asDate(start), asDate(end), true, routeDefinition.getId(), successedClass.toLowerCase());  
                model.add(event);  
            }  
        }  
    }    
    
    public static Date asDate(LocalDateTime localDateTime) {
    	ZonedDateTime zdt = localDateTime.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }
}