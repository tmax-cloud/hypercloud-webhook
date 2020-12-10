package k8s.example.client.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;
import k8s.example.client.Util;
import k8s.example.client.audit.AuditController;
import k8s.example.client.audit.AuditDataFactory;
import k8s.example.client.audit.AuditDataObject.DateTimeFormatModule;
import k8s.example.client.audit.AuditDataObject.DateTimeSerializer;
import k8s.example.client.audit.AuditDataObject.Event;
import k8s.example.client.audit.AuditDataObject.EventList;
import k8s.example.client.audit.AuditDataObject.ResponseStatus;
import k8s.example.client.util.StringUtil;
import k8s.example.client.audit.EventQueue;

public class AuditHandler extends GeneralHandler{
	private Logger logger = AuditController.logger;
	
	private EventQueue queue = AuditController.getQueue();	
	private Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new DateTimeSerializer()).setPrettyPrinting().create();
	
	@Override
	public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		logger.info("Http request, method=\"POST\", uri=\"" + session.getUri() + (StringUtil.isEmpty(session.getQueryParameterString()) ? "" : "?" + session.getQueryParameterString()) + "\"");
		
		String postData = null;
		try {
			Map<String, String> body = new HashMap<>();
			session.parseBody(body);
			postData = body.get("postData");
			
			EventList eventList = new ObjectMapper().registerModule(new DateTimeFormatModule()).readValue(postData, EventList.class);
			
			List<Event> listevent = eventList.getItems();
			logger.info("Check event list size, size=" + listevent.size());
			for(Event event : listevent) {
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
				queue.put(event);
			}
			
		} catch(Exception e) {
			logger.error("Post data: \n" + postData);
			logger.error("Check stack trace: \n" + Util.printExceptionError(e));
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, getMimeType(), "{\"result\":\"Failed to put data on the queue.\"}");
		}
		return NanoHTTPD.newFixedLengthResponse(Status.CREATED, getMimeType(), "{\"result\":\"Success\"}");
	}
	
	@Override
	public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
		logger.info("Http request, method=\"GET\", uri=\"" + session.getUri() + (StringUtil.isEmpty(session.getQueryParameterString()) ? "" : "?" + session.getQueryParameterString()) + "\"");
		
		EventList outdto = new EventList();
		try {
			outdto.setKind("EventList");
			outdto.setApiVersion("audit.k8s.io/v1");
//			resultAuditDataFactory.select(session.getParameters())
			EventList tmp = new EventList();
			tmp = AuditDataFactory.select(session.getParameters());
			outdto.setItems(tmp.getItems());
			outdto.setTotalNum(tmp.getTotalNum());
		} catch (Exception e) {
			logger.error("Failed to get event data. \n" + Util.printExceptionError(e));
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, getMimeType(), "Failed to get data.");
		}
		return NanoHTTPD.newFixedLengthResponse(Status.CREATED, getMimeType(), gson.toJson(outdto));
	}
	
    @Override
    public String getMimeType() {
        return "application/json";
    }
}
