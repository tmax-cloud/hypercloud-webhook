package k8s.example.client.handler;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.router.RouterNanoHTTPD.GeneralHandler;
import fi.iki.elonen.router.RouterNanoHTTPD.UriResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;

import k8s.example.client.Main;
import k8s.example.client.RequestOperation;
import k8s.example.client.util.WebhookUtil;

import k8s.example.client.Constants;

public class MutatingHandler extends GeneralHandler {

	private Logger logger = Main.logger;
	
    public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
    	
    	JsonObject requestBody = null;
    	JsonObject requestObject = null;
    	String uID = null;
    	String requestResource = null;
    	String operation = null;
    	String userName = null; 
    	String namespace = null;
		JsonObject responseBody = null;
		String resourceName = null;
		
    	logger.info("Start Mutating");
		Map<String, String> body = new HashMap<String, String>();
		try {
			session.parseBody( body );
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Parse input request body
		try {
			requestBody = JsonParser.parseString(body.get("postData")).getAsJsonObject();
			requestObject =  requestBody.get("request").getAsJsonObject();
			uID = requestObject.get("uid").getAsString();
			requestResource = requestObject.get("resource").getAsJsonObject().get("resource").getAsString();
			operation = requestObject.get("operation").getAsString();
			userName = requestObject.get("userInfo").getAsJsonObject().get("username").getAsString();
		} catch (Exception e) {
			logger.info("Fail to parse input json.");
			logger.info("Request body: \n" + requestBody);
			responseBody = WebhookUtil.buildAdmissionReview(uID, false, null, 403, "Fail to parse input json.");
			return WebhookUtil.setCors(NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", responseBody.toString()));
		}

		if (requestObject.has("namespace"))
			namespace = requestObject.get("namespace").getAsString();
		else
			namespace = "Cluster-scoped resource.";	
		
		if (requestObject.has("name"))
			resourceName = requestObject.get("name").getAsString();
		else
			resourceName = "";

		logger.info("Request namespace: " + namespace);
		logger.info("Request resource: " + requestResource);
		logger.info("Request resource name: " + resourceName);
		logger.info("Request operation: " + operation);
		logger.info("Request user: " + userName);		
    	
    	//Exception for all user action..
		if (denyRequest(userName, operation, requestObject)) {
			logger.info(Constants.ANNOTATION_UPDATE_EXCEPTION);
			responseBody = WebhookUtil.buildAdmissionReview(uID, false, null, 403, Constants.ANNOTATION_UPDATE_EXCEPTION);
			return WebhookUtil.setCors(NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", responseBody.toString()));
		}
    	
    	// Make JsonPatch
    	JsonPatchBuilder jsonPatchBuilder = Json.createPatchBuilder();
    	buildJsonPath(jsonPatchBuilder, userName, requestObject);
    	
    	// Encode JsonPatch
		JsonPatch jsonPatch = jsonPatchBuilder.build();
		String jsonPatchB64 = encodeJsonPatch(jsonPatch);				
		
		// Build AdmissionReview (responseBody)
		logger.info("Mutating is succeed.");
		responseBody = WebhookUtil.buildAdmissionReview(uID, true, jsonPatchB64, 200, "Mutating is succeed.");
    	return WebhookUtil.setCors(NanoHTTPD.newFixedLengthResponse(Status.OK, "application/json", responseBody.toString()));
    }
    
    private void buildJsonPath(JsonPatchBuilder jsonPatchBuilder, String userName, JsonObject requestObject) {
    	String currentTime = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(System.currentTimeMillis());
    	JsonObject metadataObejct = requestObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject();
		
		if (!metadataObejct.has("annotations")) {
			JsonBuilderFactory factory = Json.createBuilderFactory(null);
			javax.json.JsonObject annotationJson = factory.createObjectBuilder()
				.add("creator", userName).add("createdTime", currentTime)
				.add("updater", userName).add("updatedTime", currentTime)
				.build();			
			jsonPatchBuilder.add("/metadata/annotations", annotationJson);
		} else {
			if (!metadataObejct.get("annotations").getAsJsonObject().has("creator")) {
				jsonPatchBuilder.add(Constants.ANNOTATION_PATH_CREATOR, userName);
			}
			if (!metadataObejct.get("annotations").getAsJsonObject().has("createdTime")) {
				jsonPatchBuilder.add(Constants.ANNOTATION_PATH_CREATEDTIME, currentTime);
			} 
			jsonPatchBuilder.add(Constants.ANNOTATION_PATH_UPDATER, userName)
				.add(Constants.ANNOTATION_PATH_UPDATEDTIME, currentTime);
		}
    }    

    public boolean denyRequest (String userName, String operation, JsonObject requestObject) {        
        if(isSystem(userName))
        	return false;
		switch (RequestOperation.valueOf(operation)) {
		case CREATE:
			return isAnnotationsExisted(requestObject);
		case UPDATE:
			return isAnnotationsUpdated(requestObject);
		default:
			break;
		}
		return false;
    }

    public String encodeJsonPatch (JsonPatch jsonPatch) {
		Encoder encoder = Base64.getEncoder();
		String jsonPatchB64 = encoder.encodeToString(jsonPatch.toString().getBytes());
		return jsonPatchB64;
    }
 
    public boolean isSystem (String userName) {			
		return userName.split(":")[0].equalsIgnoreCase("system");   
	}
    
    public static boolean isAnnotationsExisted (JsonObject requestObject) {
        JsonObject metadataObejct = requestObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject();
    	if(!metadataObejct.has("annotations"))
    		return false;
        JsonObject annotationObject = metadataObejct.get("annotations").getAsJsonObject();
    	boolean hasCreator = annotationObject.has("creator");
    	boolean hasUpdater = annotationObject.has("updater");
    	boolean hasCreatedTime = annotationObject.has("createdTime");
    	boolean hasUpdatedTime = annotationObject.has("updatedTime");
    	
    	if (hasCreator || hasUpdater || hasCreatedTime || hasUpdatedTime) {
    		return true;
    	}
    return false;
    }
    
    public static boolean isAnnotationsUpdated (JsonObject requestObject) {        
        String [] requiredPath = {Constants.ANNOTATION_PATH_CREATOR, Constants.ANNOTATION_PATH_UPDATER, 
        		Constants.ANNOTATION_PATH_CREATEDTIME, Constants.ANNOTATION_PATH_UPDATEDTIME};
        ArrayList<String> requiredPathList = new ArrayList<>(Arrays.asList(requiredPath));
        
        JsonNode patch = patchDetails(requestObject);
		Iterator<JsonNode> iterator = patch.iterator();
		while (iterator.hasNext()) {
			JsonNode tmpNode = iterator.next();
    		String patchPath = tmpNode.get("path").asText();
      		if (requiredPathList.contains(patchPath))
      			return true;
		}
		return false;
    }
    
    public static JsonNode patchDetails (JsonObject requestObject) {
		
		ObjectMapper mapper = new ObjectMapper();
		JsonObject object = requestObject.get("object").getAsJsonObject();
		JsonObject oldObject = requestObject.get("oldObject").getAsJsonObject();
		JsonNode objectNode = null;
		JsonNode oldObjectNode = null;
	
		try {
	    	objectNode = mapper.readTree(object.toString());
	    	oldObjectNode = mapper.readTree(oldObject.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}		
		JsonNode jsonPatch = JsonDiff.asJson(oldObjectNode, objectNode);
			
		return jsonPatch;   
	}
}
