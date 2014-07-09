package org.gluu.oxtrust.action;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.gluu.oxtrust.ldap.service.RegistrationsExpirationService;
import org.gluu.oxtrust.model.GluuOrganization;
import org.gluu.oxtrust.model.RegistrationConfiguration;
import org.gluu.oxtrust.model.RegistrationInterceptorScript;
import org.gluu.oxtrust.model.SimpleCustomPropertiesListModel;
import org.gluu.oxtrust.model.Tuple;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.log.Log;
import org.xdi.model.SimpleCustomProperty;

/**
 * Action class for displaying attributes
 * 
 * @author Yuriy Movchan Date: 10.17.2010
 */
@Scope(CONVERSATION)
@Name("registrationManagementAction")
@Restrict("#{identity.loggedIn}")
public @Data class RegistrationManagementAction implements SimpleCustomPropertiesListModel, Serializable {

	private static final long serialVersionUID = -3832167044333943686L;

	private boolean enableInvitationCodes;
	
	private boolean configureInterceptors;
	
	private boolean enableRegistrationWithoutInvitation;
	
	private boolean enableInboundSAMLRegistration;
	
	private List<RegistrationInterceptorScript> registrationInterceptors;
	
	private Tuple<String, String> linksExpirationServicePeriod;
	
	private Tuple<String, String> accountsExpirationServicePeriod;
	
	private Tuple<String, String> accountsExpirationPeriod;
	
	private boolean accountsTimeLimited;
	
	@In 
	private OrganizationService organizationService;
	
	@Logger
	private Log log;

	private List<String> customScriptTypes;
	
	@In 
	private RegistrationsExpirationService registrationsExpirationService;
	
	public String init(){
		customScriptTypes = new ArrayList<String>();
		customScriptTypes.add(OxTrustConstants.PRE_REGISTRATION_SCRIPT);
		customScriptTypes.add(OxTrustConstants.POST_REGISTRATION_SCRIPT);
		GluuOrganization org = organizationService.getOrganization();
		RegistrationConfiguration config = org.getOxRegistrationConfiguration();
		List<RegistrationInterceptorScript> newScripts = new ArrayList<RegistrationInterceptorScript>();
		String configLinksExpirationFrequency = null;
		String configAccountsExpirationServiceFrequency = null;
		String configAccountsExpirationPeriod = null;
		if(config != null){
			List<RegistrationInterceptorScript> scripts = config.getRegistrationInterceptorScripts();
			if(scripts != null && !scripts.isEmpty()){
				newScripts.addAll(scripts);
			}
			enableInvitationCodes = config.isInvitationCodesManagementEnabled();
			configureInterceptors = config.isRegistrationInterceptorsConfigured();
			enableRegistrationWithoutInvitation = config.isUninvitedRegistrationAllowed();
			enableInboundSAMLRegistration = config.isInboundSAMLRegistrationAllowed();
			accountsTimeLimited = config.isAccountsTimeLimited();
			configLinksExpirationFrequency = config.getLinksExpirationFrequency();
			configAccountsExpirationServiceFrequency = config.getAccountsExpirationServiceFrequency();
			configAccountsExpirationPeriod = config.getAccountsExpirationPeriod();

		}
		
		if(configLinksExpirationFrequency == null || configLinksExpirationFrequency.isEmpty()){
			configLinksExpirationFrequency = Integer.toString(registrationsExpirationService.getDefaultLinksExpirationFrequency());
		}
		linksExpirationServicePeriod = getPeriod(new BigInteger(configLinksExpirationFrequency));
		
		if(configAccountsExpirationServiceFrequency == null || configAccountsExpirationServiceFrequency.isEmpty() ){
			configAccountsExpirationServiceFrequency = Integer.toString(registrationsExpirationService.getDefaultAccountsExpirationServiceFrequency());
		}
		accountsExpirationServicePeriod = getPeriod(new BigInteger(configAccountsExpirationServiceFrequency));
		
		if(configAccountsExpirationPeriod == null || configAccountsExpirationServiceFrequency.isEmpty()){
			configAccountsExpirationPeriod = Integer.toString(registrationsExpirationService.getDefaultAccountsExpirationPeriod());
		}
		accountsExpirationPeriod = getPeriod(new BigInteger(configAccountsExpirationPeriod));
		
		
		
		registrationInterceptors = newScripts;
		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	/**
	 * @param linksExpirationFrequency
	 * @return
	 */
	private Tuple<String, String> getPeriod(BigInteger linksExpirationFrequency) {
		Tuple<String, String> result = new Tuple<String, String>();
		BigInteger[] divideAndRemainder = linksExpirationFrequency.divideAndRemainder(BigInteger.valueOf(7*24*60));
		BigInteger weeks = divideAndRemainder[0];
		BigInteger reminder = divideAndRemainder[1];
		if( ! weeks.equals(BigInteger.valueOf(0)) && reminder.equals(BigInteger.valueOf(0))){
			result.setValue0(weeks.toString());
			result.setValue1("3");
			return result;
		}
		
		divideAndRemainder = linksExpirationFrequency.divideAndRemainder(BigInteger.valueOf(24*60));
		BigInteger days = divideAndRemainder[0];
		reminder = divideAndRemainder[1];
		if( ! days.equals(BigInteger.valueOf(0)) && reminder.equals(BigInteger.valueOf(0))){
			result.setValue0(days.toString());
			result.setValue1("2");
			return result;
		}
		
		divideAndRemainder = linksExpirationFrequency.divideAndRemainder(BigInteger.valueOf(60));
		BigInteger hours = divideAndRemainder[0];
		reminder = divideAndRemainder[1];
		if( ! hours.equals(BigInteger.valueOf(0)) && reminder.equals(BigInteger.valueOf(0))){
			result.setValue0(hours.toString());
			result.setValue1("1");
			return result;
		}

		BigInteger minutes = linksExpirationFrequency;
		result.setValue0(minutes.toString());
		result.setValue1("0");
		return result;
	}

	public String save(){
		GluuOrganization org = organizationService.getOrganization();
		RegistrationConfiguration config = org.getOxRegistrationConfiguration();
		if(config == null){
			config = new RegistrationConfiguration();
		}
		config.setRegistrationInterceptorScripts(registrationInterceptors);
		config.setInboundSAMLRegistrationAllowed(enableInboundSAMLRegistration);
		config.setInvitationCodesManagementEnabled(enableInvitationCodes);
		config.setRegistrationInterceptorsConfigured(configureInterceptors);
		config.setUninvitedRegistrationAllowed(enableRegistrationWithoutInvitation);
		config.setAccountsTimeLimited(accountsTimeLimited);
		
		config.setLinksExpirationFrequency(getPeriod(linksExpirationServicePeriod));
		config.setAccountsExpirationServiceFrequency(getPeriod(accountsExpirationServicePeriod));
		config.setAccountsExpirationPeriod(getPeriod(accountsExpirationPeriod));
		
		org.setOxRegistrationConfiguration(config);
		organizationService.updateOrganization(org);
		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	/**
	 * @param linksExpirationServicePeriod2
	 * @return
	 */
	private String getPeriod(Tuple<String, String> period) {
		Integer result = Integer.parseInt(period.getValue0());
		switch(Integer.parseInt(period.getValue1())){
			case 0: 
					break;
			case 1: result = result* 60 ;
					break;
			case 2: result = result* 24*60;
					break;
			case 3: result = result* 7*24*60;
					break;
			default:break;
		}
			
		return result.toString();
	}

	@Override
	public void removeItemFromSimpleCustomProperties(
			List<SimpleCustomProperty> simpleCustomProperties,
			SimpleCustomProperty simpleCustomProperty) {
		if (simpleCustomProperties != null) {
			simpleCustomProperties.remove(simpleCustomProperty);
		}
		
	}


	@Override
	public void addItemToSimpleCustomProperties(
			List<SimpleCustomProperty> simpleCustomProperties) {
		if (simpleCustomProperties != null) {
			simpleCustomProperties.add(new SimpleCustomProperty("", ""));
		}
	}


	public String addRegistrationInterceptor(){
		RegistrationInterceptorScript registrationScript = new RegistrationInterceptorScript();
		
		registrationInterceptors.add(registrationScript);
		
		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	public String removeCustomAuthenticationConfiguration(RegistrationInterceptorScript script){
		if(registrationInterceptors.contains(script)){
			registrationInterceptors.remove(script);
		}
		return OxTrustConstants.RESULT_SUCCESS;
	}
	
	public String cancel(){
		return OxTrustConstants.RESULT_SUCCESS;
	}
}