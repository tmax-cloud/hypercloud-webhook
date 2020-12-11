package k8s.example.client.audit;

import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;

import k8s.example.client.Util;
import k8s.example.client.audit.AuditDataObject.Event;
import k8s.example.client.audit.AuditDataObject.ResponseStatus;

public class AuditWorkerThread extends Thread {

	private Logger logger = AuditController.logger;
	
	private EventQueue queue = AuditController.getQueue();
		
	public AuditWorkerThread(int threadNum) throws Exception {
		this.setName("worker" + threadNum);
	}
	
	@Override
	public void run() {
		try {
			logger.info("Worker thread start.");
			
			while(true) {
				List<Event> eventList = queue.takeAll();
				for(Event event: eventList) {
					ResponseStatus responseStatus = event.getResponseStatus();
				 	if((responseStatus.getCode() / 100) == 2  && responseStatus.getStatus() == null) {
				 		responseStatus.setStatus("Success");
				 	}
				 	if((responseStatus.getCode() / 100) == 4  && responseStatus.getStatus() == null) {
				 		responseStatus.setStatus("Failure");
				 	}
				 	if((responseStatus.getCode() / 100) == 5  && responseStatus.getStatus() == null) {
				 		responseStatus.setStatus("Failure");
				 	}
				}
				AuditDataFactory.insert(eventList);
			}
			
		} catch(SQLException e) {
			logger.error("Sql exception, exception=\"" + e.getMessage() + "\"");
			logger.error(Util.printExceptionError(e));
		} catch(Exception e) {
			logger.error(Util.printExceptionError(e));
		}
		
	}
}
