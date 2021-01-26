package ru.cetelem.watcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component(value = "applicationSettingsService")
public class ApplicationSettingsService {

	@Value("${build.version:0.0.X}")
	private String buildVersion;
	
	
	public String getBuildVersion() {
		return buildVersion;
	}
	
}
