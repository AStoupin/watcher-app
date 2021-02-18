package ru.cetelem.watcher.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.cetelem.watcher.service.SessionInfoService;

import javax.faces.context.FacesContext;
import java.io.IOException;

@Scope(value = "session")
@Component(value = "loginController")
public class LoginController {
	private static final Logger log = LoggerFactory.getLogger(LoginController.class);

	@Autowired
	SessionInfoService sessionInfoService;

	LoginController(){
		log.info("login");
	}
	public String getFullName() {
		return sessionInfoService.getFullName();
	}


	public void init() throws IOException {
		if (FacesContext.getCurrentInstance().isPostback() && sessionInfoService.getFullName() != null){
			FacesContext.getCurrentInstance().getExternalContext().
					redirect(".");
			return;
		}
	}


	public String logout() throws IOException {
		FacesContext.getCurrentInstance().getExternalContext().
				redirect("logout");
		return "logout";
	}
}
