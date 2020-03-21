package ru.cetelem.watcher.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ru.cetelem.watcher.model.RouteStatItem;

@Service
public class RouteStatService {
	private static final Logger log = LoggerFactory.getLogger(RouteStatService.class);
	private static final int MAX_SIZE = 5;
	
	private HashMap<String, List<RouteStatItem>> routeStat = new HashMap<String, List<RouteStatItem>>();
	
	public RouteStatService(){
		log.info("RouteStatService inited");
	}
	
	public void add(Exchange exchange) {
		String completeMessage = Optional.ofNullable(exchange.getProperty(Exchange.EXCEPTION_CAUGHT)).orElse("ok").toString();
		GenericFile<Object> fileName = (GenericFile<Object>)Optional.ofNullable(exchange.getProperty("CamelFileExchangeFile")).
																		orElse(getNoFile());
		 
		RouteStatItem routeStatItem = new RouteStatItem(exchange.getFromRouteId(), fileName.getFileName(), 
													"ok".equals(completeMessage), completeMessage);
		addStatItem(exchange.getFromRouteId(), routeStatItem);
		
	}
	
	public List<RouteStatItem> getStatListById(String routeID) {
		List<RouteStatItem> result = routeStat.get(routeID);
		
		if(result==null) {
			result = new ArrayList<RouteStatItem>();
			routeStat.put(routeID, result);
		}
		return result;
	}

	public long getFailuresById(String routeID) {
		return Optional.ofNullable(getStatListById(routeID)).orElse(new ArrayList<RouteStatItem>())
				.stream().filter(ri->!ri.isSuccessed()).count();
	}

	private void addStatItem(String routeId, RouteStatItem routeStatItem) {
		List<RouteStatItem> lst = getStatListById(routeId);
		lst.add(routeStatItem);
		if (lst.size() > MAX_SIZE) lst.remove(0);
	}
	
	private GenericFile<Object> getNoFile(){
		GenericFile<Object>  file = new GenericFile<Object> (); 
		file.setFileName("No-File"); 
		return file;		
	}
	
	public List<RouteStatItem> getStatList(){
		List<RouteStatItem> lst;
		lst = routeStat.entrySet().stream().flatMap(k->k.getValue().stream())
					.sorted((si1, si2)->si2.getEventDate().compareTo(si1.getEventDate())).collect(Collectors.toList());
		return lst;
	}

}
