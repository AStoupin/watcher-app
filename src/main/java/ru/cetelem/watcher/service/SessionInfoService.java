package ru.cetelem.watcher.service;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.stereotype.Component;

@Component(value = "sessionInfoService")
public class SessionInfoService {

	@Setter
	@Getter
	public boolean ldapEnabled;

	public String getFullName() {
		if(SecurityContextHolder.getContext()==null || 
				!(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof InetOrgPerson))
			return "";		
			
		InetOrgPerson ip = (InetOrgPerson) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		 

		return Optional.ofNullable(ip.getDisplayName()).orElseGet(()->ip.getSn());
	}

	public boolean isAuthenticated(){
		return !"".equals(getFullName());
	}
}
