package k8s.example.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.iki.elonen.NanoHTTPD.Response;


public class Util {	
	public static Logger logger = Main.logger;

    public static Date getDateFromSecond(long seconds) {
		return Date.from(LocalDateTime.now().plusSeconds(seconds).atZone(ZoneId.systemDefault()).toInstant());
	}
    
    public static class Crypto {
	    public static String encryptSHA256(String input) throws Exception{
			String ret = "";
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] hash = digest.digest(input.getBytes("UTF-8"));
				StringBuffer hexString = new StringBuffer();
	
				for (int i = 0; i < hash.length; i++) {
					String hex = Integer.toHexString(0xff & hash[i]);
					if (hex.length() == 1) hexString.append('0');
					hexString.append(hex);
				}
				ret = hexString.toString();	
			} catch (Exception e) {
				throw e;
			}
			return ret;
		}
    }
    
    public static String getRamdomPassword(int len) { 
    	char[] charSet = new char[] { 
    			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
    			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' 
    			}; 
    	
    	int idx = 0; 
    	StringBuffer sb = new StringBuffer(); 
    	
    	for (int i = 0; i < len; i++) { 
    		idx = (int) (charSet.length * Math.random()); 
    		sb.append(charSet[idx]); 
    	}
    	return sb.toString();
	}
    
    public static String makeK8sFieldValue(String name) { 
    	return name.replaceAll("@", "-").replaceAll("_", "-");
	}
    
	 public static String numberGen(int len, int dupCd ) {
	        
	        Random rand = new Random();
	        String numStr = ""; 
	        
	        for(int i=0;i<len;i++) {
	        	String ran = null;
	        	if (i == 0) {
	        		ran = Integer.toString(rand.nextInt(9)+1);
	        	}else {
		            ran = Integer.toString(rand.nextInt(10));
	        	}    
	            if(dupCd==1) {
	                numStr += ran;
	            }else if(dupCd==2) {
	                if(!numStr.contains(ran)) {
	                    numStr += ran;
	                }else {
	                    i-=1;
	                }
	            }
	        }
        return numStr;
	}
	 

    
    public static Response setCors( Response resp ) {
		resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.addHeader("Access-Control-Max-Age", "3628800");
        resp.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        resp.addHeader("Access-Control-Allow-Headers", "Content-Type, X-Requested-With, Accept, Authorization, Referer, User-Agent" );
		return resp;
    }
    
    public static JsonNode jsonDiff(String beforeJson, String afterJson) throws Exception{
    	try {
    		ObjectMapper jackson = new ObjectMapper(); 
    		JsonNode beforeNode = jackson.readTree(beforeJson); 
    		JsonNode afterNode = jackson.readTree(afterJson); 
    		return JsonDiff.asJson(beforeNode, afterNode);
    	}catch(Exception e) {
    		
    		throw e;
    	}
    }
    
    public static JsonElement toJson(Object o) {
		JsonObject json = (JsonObject) new JsonParser().parse(new Gson().toJson(o));
		json.remove("status");
		JsonObject metadata = json.getAsJsonObject("metadata");
		if( metadata != null ) {
			metadata.remove("annotations");
			metadata.remove("creationTimestamp");
			metadata.remove("generation");
			metadata.remove("resourceVersion");
			metadata.remove("selfLink");
			metadata.remove("uid");
		}
		
		return json;
	}
    
    public static String parseImageName(String imageName) {
    	return imageName.replaceAll("[/]", "-s-").replaceAll("[_]", "-u-");
    }
    

	public static String printExceptionError(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
    
}
