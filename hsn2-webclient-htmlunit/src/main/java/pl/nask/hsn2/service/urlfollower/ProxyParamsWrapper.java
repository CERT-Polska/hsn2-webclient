package pl.nask.hsn2.service.urlfollower;

import org.apache.commons.httpclient.URIException;
import pl.nask.hsn2.normalizers.URLHostParseException;
import pl.nask.hsn2.normalizers.URLMalformedInputException;
import pl.nask.hsn2.normalizers.URLParseException;
import pl.nask.hsn2.normalizers.UrlNormalizer;


public class ProxyParamsWrapper {
	private static final int HTTP_PROXY = 1;
	private static final int SOCKS_PROXY = -1;
	private static final int INCORRECT_PROXY = 0;
	private int proxyType = INCORRECT_PROXY;
	private String	hostname;
	private int	port = -1;
	private String	userCredentials;
	public ProxyParamsWrapper(String proxyToParse)  {
		if ( proxyToParse == null || proxyToParse.length() == 0) {
			return;
		}
		UrlNormalizer un = new UrlNormalizer(proxyToParse);
		try {
			un.normalize();
		} catch (URIException | URLMalformedInputException | URLHostParseException | URLParseException e) {
			proxyType = INCORRECT_PROXY;
			return ;
		}
		if ( un.isURL()) {
			processHttp(un);
		} else {
			processSocks(un);
		}

	}
	private void processSocks(UrlNormalizer un) {
		if ( !un.isNormalized() && !"socks".equalsIgnoreCase(un.getProtocol()) ) {
				return;
		}
		String socks ="http"+ un.getNormalized().substring("socks".length());
		UrlNormalizer tmpNorm = new UrlNormalizer(socks);
		try {
			tmpNorm.normalize();
			if ( tmpNorm.getPort() < 1) {
				return;	
			}
			extractRequiredFields(tmpNorm);
			proxyType = SOCKS_PROXY ;
		} catch (URIException | URLMalformedInputException | URLHostParseException | URLParseException e) {
			proxyType = INCORRECT_PROXY;
		}
	}
	
	private void processHttp(UrlNormalizer normalized) {
		extractRequiredFields(normalized);
		if ( this.port < 1) {
			setUpDefaultPorts(normalized.getProtocol());
		}
		proxyType = HTTP_PROXY;
		
	}
	
	private void extractRequiredFields(UrlNormalizer tmpNorm) {
		this.port = tmpNorm.getPort();
		this.hostname = tmpNorm.getHost();
		if(!tmpNorm.getUserInfo().isEmpty()) {
			this.userCredentials = tmpNorm.getUserInfo();
		}
	}
	
	private void setUpDefaultPorts(String protocol) {
		if ( "https".equalsIgnoreCase(protocol)) {
			this.port = 443;
		} else {
			this.port = 80;
		} 
		
	}
	public String getHost() {
		return hostname;
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean isProxy() {
		return proxyType != INCORRECT_PROXY;
	}
	
	public boolean isHttpProxy() {
		return HTTP_PROXY == proxyType;
	}
	
	public boolean isSocksProxy() {
		return SOCKS_PROXY == proxyType;
	}
	
	public boolean hasUserCredentials() {
		return userCredentials != null  && userCredentials.indexOf(":") != 0;
	}
	
	public String getUserName() {
		if ( userCredentials != null) {
			int userNameTerm = (userCredentials.indexOf(":") < 0) ? userCredentials.length() : userCredentials.indexOf(":");
			return userCredentials.substring(0, userNameTerm);
		}
		return null;
	}
	public String getUserPswd() {
		if ( userCredentials == null)
			return "";
		
		int ps = userCredentials.indexOf(":");
		if (ps > 0  &&  ps < userCredentials.length()-1 )
			return userCredentials.substring(ps+1);
		
		return  "";//it might look inconsistent with getUserName(), but better supplying empty password if called.
	}
	@Override
	public String toString() {
		if (!isProxy()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		if ( isHttpProxy()) {
			sb.append("http://");
		}
		else {
			sb.append("socks://");
		}
		if(hasUserCredentials()) {
			sb.append("xxxxxx@");
		}
		sb.append(hostname);
		sb.append(":").append(port);	
		return sb.toString();
	}

}
