package ru.cetelem.watcher.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.ldap.userdetails.InetOrgPersonContextMapper;
import ru.cetelem.watcher.service.SessionInfoService;


@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	static final Logger log = LogManager.getLogger(SecurityConfig.class); 

	@Value("${ldap.urls}")
	private String ldapUrls;

	@Value("${ldap.base.dn}")
	private String ldapBaseDn;

	@Value("${ldap.username}")
	private String ldapSecurityPrincipal;

	@Value("${ldap.password}")
	private String ldapPrincipalPassword;

	@Value("${ldap.user.dn.pattern}")
	private String ldapUserDnPattern;

	@Value("${ldap.enabled}")
	private String ldapEnabled;
	

	@Value("${ldap.group.search.base}")				
	private String ldapGroupSearchBase;
	@Value("${ldap.group.search.filter}")				
	private String ldapGroupSearchFilter;
	@Value("${ldap.group.role.attribute}")				
	private String ldapGroupRoleAttribute;

	@Autowired
	private SessionInfoService sessionInfoService;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		sessionInfoService.setLdapEnabled(Boolean.parseBoolean(ldapEnabled));

		if (!Boolean.parseBoolean(ldapEnabled)) {
			http.authorizeRequests()
					.antMatchers("/**").permitAll();
			http.csrf().disable();

			return;
		}

		http.authorizeRequests()
			.antMatchers("/javax.faces.resource/**").permitAll()
			.antMatchers("/j_spring_security_check").permitAll()
			.antMatchers("/login**").permitAll()
			.antMatchers("/").permitAll()
			.antMatchers("/route/*").fullyAuthenticated()
			.antMatchers("/route-add/*").fullyAuthenticated()
			.antMatchers("/master").fullyAuthenticated()
		  ;
	  
		http.formLogin().loginPage("/login").permitAll()
			.failureUrl("/login?error=true")
        	.successForwardUrl("/")
			;
	  
		http.logout().invalidateHttpSession(true)
	  		.logoutSuccessUrl("/login").deleteCookies("JSESSIONID").permitAll();
	  
		http.csrf().disable();

	}


   
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		
		if (!Boolean.parseBoolean(ldapEnabled)) 
			log.info("LDAP security is disabled");
		

		auth.ldapAuthentication()
				.contextSource()
					.url(ldapUrls + ldapBaseDn)
					.managerDn(ldapSecurityPrincipal)
					.managerPassword(ldapPrincipalPassword)
					
				.and()
					.userSearchFilter(ldapUserDnPattern)
					.groupSearchBase(ldapGroupSearchBase)
					.groupRoleAttribute(ldapGroupRoleAttribute)
					.groupSearchFilter (ldapGroupSearchFilter)
					.userDetailsContextMapper(new InetOrgPersonContextMapper());

		log.info("LDAP security:  " + ldapUrls + ldapBaseDn);

	}


}