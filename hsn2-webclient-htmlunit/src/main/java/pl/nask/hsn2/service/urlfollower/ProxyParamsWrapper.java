package pl.nask.hsn2.service.urlfollower;

import org.apache.commons.httpclient.URIException;
import pl.nask.hsn2.normalizers.URLHostParseException;
import pl.nask.hsn2.normalizers.URLMalformedInputException;
import pl.nask.hsn2.normalizers.URLParseException;
import pl.nask.hsn2.normalizers.UrlNormalizer;


public class ProxyParamsWrapper {
//	private static Logger LOG = LoggerFactory.getLogger(ProxyParamsWrapper.class);
	private static final int HTTP_PROXY = 1;
	private static final int SOCKS_PROXY = -1;
	private int proxyType = 0;
	private String	hostname;
	private int	port = -1;
	private String	userCredentials;
	public ProxyParamsWrapper(String proxyToParse)  {
		if ( proxyToParse == null || proxyToParse.length() == 0)
			return;
		UrlNormalizer un = new UrlNormalizer(proxyToParse);
		try {
			un.normalize();
			if ( un.isURL()) {
				processHttp(un);
			} else {
				processSocks(un);
			}
		} catch (URIException | URLMalformedInputException | URLHostParseException | URLParseException e) {
//			LOG.warn("Cannot parse proxy: {}.parameter will be ignored.",proxyToParse);
			proxyType = 0;
		}
	}
	private void processSocks(UrlNormalizer un) {
		if ( !un.isNormalized() && !"socks".equalsIgnoreCase(un.getProtocol()) )
				return;
		String socks ="http"+ un.getNormalized().substring("socks".length());
		UrlNormalizer tmpNorm = new UrlNormalizer(socks);
		try {
			tmpNorm.normalize();
			if ( tmpNorm.getPort() < 1)
				return;	
			port = tmpNorm.getPort();
			hostname = tmpNorm.getHost();
			if(tmpNorm.getUserInfo().length() > 0)
				userCredentials = tmpNorm.getUserInfo();
			proxyType = SOCKS_PROXY ;
		} catch (URIException | URLMalformedInputException | URLHostParseException | URLParseException e) {
			proxyType = 0;
		}
		
		
	}
	private void processHttp(UrlNormalizer un) {
		this.hostname = un.getHost();
		this.port = un.getPort();
		if ( port < 1) {
			setUpDefaultPorts(un.getProtocol());
		}
		if ( un.getUserInfo().length() > 0)
			userCredentials = un.getUserInfo();
		proxyType = HTTP_PROXY;
		
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
		return proxyType != 0;
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
		if ( userCredentials != null)
			return userCredentials.substring(0,(userCredentials.indexOf(":") < 0) ? userCredentials.length() : userCredentials.indexOf(":") );
		return null;
	}
	public String getUserPswd() {
		if ( userCredentials == null)
			return "";
		
		int ps = userCredentials.indexOf(":");
		if (ps > 0  &&  ps < userCredentials.length()-1 )
			return userCredentials.substring(ps+1);
		
		return  "";
	}

}
