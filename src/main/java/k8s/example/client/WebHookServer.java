package k8s.example.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;

import org.slf4j.Logger;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.router.RouterNanoHTTPD;
import k8s.example.client.handler.AuditHandler;
import k8s.example.client.handler.MutatingHandler;
import k8s.example.client.handler.ValidatingHandler;

public class WebHookServer extends RouterNanoHTTPD {
    private Logger logger = Main.logger;
    
    public WebHookServer() throws IOException {
        super(8080);
		char[] password = "test".toCharArray();
		KeyStore ks = null;
		KeyManagerFactory kmf = null;
		try {
	        ks = KeyStore.getInstance(KeyStore.getDefaultType());
			FileInputStream fis = new FileInputStream("/run/secrets/tls/hypercloud4-webhook.jks");
			ks.load(fis, password);
			kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, password);
		} catch(Exception e) {
			e.printStackTrace();
		}
	    makeSecure(NanoHTTPD.makeSSLSocketFactory(ks, kmf), null);
        addMappings();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        logger.info("Nano HTTPD is running!!");
    }
  
    @Override
    public void addMappings() {
    	addRoute("/audit", AuditHandler.class);
    	addRoute("/mutate", MutatingHandler.class);
    	addRoute("/validate", ValidatingHandler.class);
    }
}
