package ru.cetelem.watcher.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.cetelem.watcher.service.ErrorProcessor;

public class RouteStatItem {
	private static final Logger log = LoggerFactory.getLogger(RouteStatItem.class);
	
	private String routeId;
	private String fileName;
	private LocalDateTime eventDate;
	private boolean isSuccessed;
	private String errorMessage;
	private String errorDetails;

	public String getRouteId() {
		return routeId;
	}

	public String getFileName() {
		return fileName;
	}


	public LocalDateTime getEventDate() {
		return eventDate;
	}


	public boolean isSuccessed() {
		return isSuccessed;
	}


	public String getErrorMessage() {
		return errorMessage;
	}

	public String getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}
	
	
	public RouteStatItem(String routeId, String fileName, boolean isSuccessed, String errorMessage,
			String errorDetails) {
		super();
		this.routeId = routeId;
		this.fileName = fileName;
		this.eventDate = LocalDateTime.now();
		this.isSuccessed = isSuccessed;
		this.errorMessage = errorMessage;
		this.errorDetails = errorDetails;
		
		log.info("RouteStatItem event: " + this.toString());
	}



	@Override
	public String toString() {
		return "RouteStatItem [routeId=" + routeId + ", fileName=" + fileName + ", eventDate=" + eventDate
				+ ", isSuccessed=" + isSuccessed + ", errorMessage=" + errorMessage + "]";
	}




}
