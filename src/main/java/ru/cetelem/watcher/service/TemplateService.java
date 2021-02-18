package ru.cetelem.watcher.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

	private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
	
	public static final String EMPTY_TEMPLATE_KEY  = "Empty";
	public static final String WIZARD_TEMPLATE_KEY  = "Wizard...";

	
	@Value("${xmlTemplates.directory}")
	String xmlTemplatesDirectory;
	
	@Autowired
	ApplicationContext applicationContext;
	
	private HashMap<String, String> templates = new HashMap<String, String>();

	public HashMap<String, String>  getTemplates() {
		return templates;
	}



	@PostConstruct
	public void init() {
		loadTemplates(xmlTemplatesDirectory);

	}
	


	private void loadTemplates(String directory) {
		log.info("Loading templates XML routes from: {}", directory);

		try {
			templates.put(EMPTY_TEMPLATE_KEY, "");
			
			Resource[] xmlTemplates = applicationContext.getResources(directory);
			for (Resource xmlTemplate : xmlTemplates) {
				log.debug("Found XML route: {}", xmlTemplate);

				String template = new BufferedReader(new InputStreamReader(xmlTemplate.getInputStream()))
						  .lines().collect(Collectors.joining("\n"));
				
				templates.put(FilenameUtils.removeExtension(xmlTemplate.getFile().getName()), template);
			
				
			}

		} catch (Exception e) {
			log.error("No XML templates found in {}. Skipping XML templates detection.", directory);
		}

	}
}
