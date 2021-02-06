package ru.cetelem.watcher.service;

import java.util.HashMap;

import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.springframework.stereotype.Component;

@Component
public class RouteDefinitionExplorer {
	protected static final String LS = System.lineSeparator();
    private static final String SEPARATOR = "&";
    
	public String getFromInfo(RouteDefinition routeDefinition) {
		if(routeDefinition.getInputs().size()==0){
			return "";
		}
		return  getUriInfo(routeDefinition.getInputs().get(0).getEndpointUri());
	}
	
	
	public String getToInfo(RouteDefinition routeDefinition) {
		StringBuilder result = new StringBuilder("");
		routeDefinition.getOutputs().forEach(
				pd->result.append(getProcessorDefinition(pd)));

		return result.toString();
			
	}
	
	private String getProcessorDefinition(ProcessorDefinition<?> processorDefinition) {
		StringBuilder result = new StringBuilder("");

		if (processorDefinition instanceof  org.apache.camel.model.ToDefinition)
			result.append(getUriInfo(((org.apache.camel.model.ToDefinition)processorDefinition).getEndpointUri()) + "; ");
		else
			processorDefinition.getOutputs().forEach(
					pd->{
						result.append(getProcessorDefinition((ProcessorDefinition<?>)pd));
					}
				);

		return result.toString();
			
	}
	
	private String getUriInfo(String uri) {
		String[] serverAndParams = uri.split("\\?"); 
		String serverName = serverAndParams[0].replaceAll("(:[^\\/].*)@",":***@") /*replace password info*/;
		
		String paramInfo ="";

		//Collect route extra params
		if (serverAndParams.length>1) {
			String paramString = serverAndParams[1];
			HashMap<String, String> params = new HashMap<String, String>();
			
			//split root params key/value 
	        for (String s : paramString.split(SEPARATOR)) {
	            if (s != null) {
	            	params.put(s.split("=")[0], s.split("=")[1]);
	            }
	        }
	        
	        // include some params to Info result
	        for ( String key : params.keySet()) {
	        	if("include".equals(key))
	        		paramInfo = paramInfo + " " + params.get("include");
	        	if("antInclude".equals(key))
	        		paramInfo = paramInfo + " " +  params.get("antInclude");
	        }
	        
	        paramInfo =  "".equals(paramInfo) ? "" : " ("+ paramInfo+")";
		}
		
		return serverName + paramInfo;
	}
	
}
