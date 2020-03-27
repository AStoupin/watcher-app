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
@Component(value = "routeMonitorController")
public class RouteMonitorController {
	private static final Logger log = LoggerFactory.getLogger(RouteMonitorController.class);
	
	@Autowired
	private CamelContext camelContext;

	@Autowired
	RouteDefinitionExplorer routeDefinitionInformer;

	@Autowired
	RouteStatService routeStatService;

	
	public RouteMonitorController(){
	}
	private List<RouteStatItem>  routeStatFilteredItems;
	public List<RouteStatItem> getRouteStatItems(){
    	return routeStatService.getStatList();
    }
	public List<RouteStatItem> getRouteStatFilteredItems(){
		return routeStatFilteredItems;
	}
	public void setRouteStatFilteredItems(List<RouteStatItem>  routeStatFilteredItems ){
		this.routeStatFilteredItems = routeStatFilteredItems;

	}



}