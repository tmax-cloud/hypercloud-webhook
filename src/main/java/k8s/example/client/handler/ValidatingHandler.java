package k8s.example.client.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import k8s.example.client.util.WebhookUtil;
import k8s.example.client.Constants;
import k8s.example.client.Main;

public class ValidatingHandler extends GeneralHandler {
	
	private Logger logger = Main.logger;
	
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {

      	logger.info("Start Validating");
		Map<String, String> body = new HashMap<String, String>();
		try {
			session.parseBody( body );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Parse input request body
		JsonObject requestBody = JsonParser.parseString(body.get("postData")).getAsJsonObject();
		JsonObject requestObject =  requestBody.get("request").getAsJsonObject();
		String uID = requestObject.get("uid").getAsString();
		String requestResource = requestObject.get("resource").getAsJsonObject().get("resource").getAsString();
		String resourceName = requestObject.get("name").getAsString();
		String operation = requestObject.get("operation").getAsString();
		String userName = requestObject.get("userInfo").getAsJsonObject().get("username").getAsString();
		String namespace = null;
		
		if (!requestObject.has("namespace"))
			namespace = "Cluster-scoped resource.";
		else
			namespace = requestObject.get("namespace").getAsString();
		
		
		logger.info("Request namespace: " + namespace);
		logger.info("Request resource: " + requestResource);
		logger.info("Request resource name: " + resourceName);
		logger.info("Request operation: " + operation);
		logger.info("Request user: " + userName);
		logger.info("Request body: \n" + requestBody);
		
		JsonObject responseBody = null;
		
		if (!hasAllAnnotations(requestObject)) {
			logger.info(requestResource + Constants.ANNOTATION_NOT_EXISTED_EXCEPTION);
			responseBody = WebhookUtil.buildAdmissionReview(uID, false, null, 403, requestResource + " " +Constants.ANNOTATION_NOT_EXISTED_EXCEPTION);
			return WebhookUtil.setCors(NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", responseBody.toString()));
		}
		
		logger.info("Validating admission control is succeed.");
		responseBody = WebhookUtil.buildAdmissionReview(uID, true, null, 200, "Validating admission control is succeed.");
        return WebhookUtil.setCors(NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", responseBody.toString() ));
    }

	//�븯�굹�씪�룄 �뾾�쑝硫� �븞�뙋
    public static boolean hasAllAnnotations (JsonObject requestObject) {
//    	boolean existAll
        JsonObject metadataObejct = requestObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject();
    	if(!metadataObejct.has("annotations"))
    		return false;
        JsonObject annotationObject = metadataObejct.get("annotations").getAsJsonObject();
    	boolean hasCreator = annotationObject.has("creator");
    	boolean hasUpdater = annotationObject.has("updater");
    	boolean hasCreatedTime = annotationObject.has("createdTime");
    	boolean hasUpdatedTime = annotationObject.has("updatedTime");
    	
    	if (hasCreator && hasUpdater && hasCreatedTime && hasUpdatedTime) {
    		return true;
    	}
    	
    return false;
    }
    
}

