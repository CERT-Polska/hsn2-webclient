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
	private int	port;
	private String	userCredentials;
	public ProxyParamsWrapper(String proxyToParse)  {
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
		//TODO
		proxyType = 0;
	}
	private void processHttp(UrlNormalizer un) {
		this.hostname = un.getHost();
		this.port = un.getPort();
		if ( port < 1) {
			setUpDefaultPorts(un.getProtocol());
		}
		if ( un.getUserInfo().length() > 0)
			userCredentials = un.getUserInfo();
		proxyType = 1;
		
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
	public boolean isCorrectProxy() {
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
		return userCredentials.substring(userCredentials.indexOf(":"));
	}
	public String getUserPswd() {
		int ps = userCredentials.indexOf(":");
		if (ps > 0)
			return userCredentials.substring(ps);
		return ""; //TODO check return null is accepted.
	}

}
