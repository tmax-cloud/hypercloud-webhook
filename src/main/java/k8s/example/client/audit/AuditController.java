package k8s.example.client.audit;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import k8s.example.client.Constants;
import k8s.example.client.Util;
import k8s.example.client.audit.AuditDataObject.Event;
import k8s.example.client.audit.AuditDataObject.ObjectReference;
import k8s.example.client.audit.AuditDataObject.ResponseStatus;
import k8s.example.client.audit.AuditDataObject.User;

public class AuditController {
	
	public static final String AUDIT = "Audit";
	public static Logger logger = LoggerFactory.getLogger(AUDIT);
	
	private static EventQueue queue = new EventQueue();

	public static void start() {
		logger.info("Audit controller start.");
		
		try {
			
			int workerThreadCnt = System.getenv("AUDIT_WORKER_THREAD_CNT") == null ? 3 : Integer.parseInt(System.getenv("AUDIT_WORKER_THREAD_CNT"));
			int batchInsertTime = System.getenv("AUDIT_BATCH_INSERT_TIME") == null ? 10 : Integer.parseInt(System.getenv("AUDIT_BATCH_INSERT_TIME"));
			logger.info("Set worker thread count form env. cnt=" + workerThreadCnt);
			logger.info("Set batch insert time interval from env. period=" + batchInsertTime);

			// TODO: Let's merge it later with webhook server 
//			AuditApiServer server = new AuditApiServer(28678);
//			server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
//			logger.info("Audit api server is now running.");
			
			// start worker thread
			for(int i = 0; i < workerThreadCnt; i++) {
				AuditWorkerThread worker = new AuditWorkerThread(i);
				worker.start();
			}
			
			// add timer
			Timer auditTimer = new Timer("audit-timer");
			auditTimer.schedule(new TimerTask() {
				public void run() {
//					logger.info("Timer wake up, " + batchInsertTime + " seconds have passed.");
					try {
						AuditController.getQueue().signalNotEmpty();
					} catch(Exception e) {
						logger.error("Failed to call signalNotEmpty method. \n" + Util.printExceptionError(e));
					}
				}
			}, new Date(), batchInsertTime * 1000);

			logger.info("Audit controller end.");
			
		} catch(Exception e) {
			logger.error("Failed to run audit controller. \n" + Util.printExceptionError(e));
		}
	}
	
	public static EventQueue getQueue() {
		return queue;
	}
	
	public static void auditLoginActivity(String username, String useragent, int code, String reason) {
		auditUserActivity("login", username, useragent, code, reason);
	}
	
	public static void auditLogoutActivity(String username, String useragent, int code, String reason) {
		auditUserActivity("logout", username, useragent, code, reason);
	}
	
	public static void auditUserActivity(String verb, String username, String useragent, int code, String reason) {
		try {
			Event event = new Event();
			event.setAuditID(UUID.randomUUID().toString());
			event.setUser(new User());
			event.getUser().setUsername(username);
			event.setUserAgent(useragent);
			event.setObjectRef(new ObjectReference());
			event.getObjectRef().setApiGroup(Constants.CUSTOM_OBJECT_GROUP);
			event.getObjectRef().setApiVersion(Constants.CUSTOM_OBJECT_VERSION);
			event.getObjectRef().setResource(Constants.CUSTOM_OBJECT_PLURAL_USER);
			event.getObjectRef().setName(username);
			event.setStage("ResponseComplete");
			event.setStageTimestamp(new DateTime());
			event.setVerb(verb);
			event.setResponseStatus(new ResponseStatus());
			event.getResponseStatus().setCode(code);
			event.getResponseStatus().setStatus(code/100 == 2 ? "Success" : "Failure");
			event.getResponseStatus().setReason(reason);
			event.getResponseStatus().setMessage(verb + " " + (code/100 == 2 ? "success" : "failed"));
			
			queue.put(event);
		} catch(Exception e) {
			logger.error("Failed to record user activity, verb=\"" + verb + ", username=\"" + username + "\"\n" + Util.printExceptionError(e));
		}
		
	}

}
