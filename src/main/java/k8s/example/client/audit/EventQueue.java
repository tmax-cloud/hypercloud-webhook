package k8s.example.client.audit;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import k8s.example.client.Util;
import k8s.example.client.audit.AuditDataObject.Event;

public class EventQueue {
	
	final Lock lock = new ReentrantLock();
	final Condition notEmpty = lock.newCondition();
	
	private LinkedList<Event> eventList = new LinkedList<>();
	private Logger logger = AuditController.logger;
	
	private int maxQueueSize;

	public EventQueue() {
		maxQueueSize = System.getenv("AUDIT_MAX_QUEUE_SIZE") == null ? 500 : Integer.parseInt(System.getenv("AUDIT_MAX_QUEUE_SIZE"));
		logger.info("Set max queue size from env. size=" + maxQueueSize);
	}

	public void put(Event event) {
		lock.lock();
		try {
			if(eventList.size() >= maxQueueSize) {
				throw new IllegalStateException();
			}
			
			eventList.add(event);
			
			if(eventList.size() == maxQueueSize) {
				logger.info("The queue is full, forcing data to be processed.");
				notEmpty.signal();
			}

		} catch(IllegalStateException e) {
			logger.error("Element cannot be added at this time due to capacity restrictions.");
			throw e;
		} catch(Exception e) {
			logger.error(Util.printExceptionError(e));
		} finally {
			lock.unlock();
		}
	}
	
	public List<Event> takeAll() {
		lock.lock();
		
		List<Event> result = null;
		try {
			if(eventList.size() < maxQueueSize) {
				notEmpty.await();
			}
			
			result = eventList;
			eventList = new LinkedList<>();
			
		} catch(Exception e) {
			logger.error(Util.printExceptionError(e));
		} finally {
			lock.unlock();
		}
		return result;
	}
	
	public void signalNotEmpty() {
		lock.lock();
		try {
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
	}
	
	

}
