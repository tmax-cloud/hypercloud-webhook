package k8s.example.client.util;

import com.google.gson.JsonObject;

import fi.iki.elonen.NanoHTTPD.Response;

public class WebhookUtil {
	public static final String ADMISSION_REVIEW_VERSION = "admission.k8s.io/v1beta1";
	
    public static JsonObject buildAdmissionReview (String uID, boolean allowd, String jsonPatchB64, int code, String message) {
		// Build AdmissionReview 
    	JsonObject admissionReview = new JsonObject();
    	JsonObject responseObject = new JsonObject();
    	JsonObject statusObject = new JsonObject();
    	
    	admissionReview.addProperty("apiVersion", ADMISSION_REVIEW_VERSION);
    	admissionReview.addProperty("kind", "AdmissionReview");
    	
    	if (jsonPatchB64 != null) {
	    	responseObject.addProperty("patch", jsonPatchB64);
	    	responseObject.addProperty("patchType", "JSONPatch");
    	}
    	responseObject.addProperty("allowed", allowd);
    	responseObject.addProperty("uid", uID); 
    	
    	statusObject.addProperty("code", code);
    	statusObject.addProperty("message", message);
    	responseObject.add("status", statusObject);

    	admissionReview.add("response", responseObject);
    	System.out.println("AdmissionReview = " + admissionReview);
    	return admissionReview;
    }
    
    public static Response setCors( Response resp ) {
		resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Max-Age", "3628800");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, Accept, Authorization, Referer, User-Agent" );
		return resp;
    }
    
}
