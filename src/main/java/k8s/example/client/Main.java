
package k8s.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	public static Logger logger = LoggerFactory.getLogger("Webhook");
	public static void main(String[] args) {
		try {
			// Start webhook server
			logger.info("[Main] Start webhook server");
			new WebHookServer();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}