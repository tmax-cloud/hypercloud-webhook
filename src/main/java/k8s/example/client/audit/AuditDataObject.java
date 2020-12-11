package k8s.example.client.audit;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.chrono.ISOChronology;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import k8s.example.client.util.StringUtil;

public class AuditDataObject {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class User {
		private String username;
		private String uid;
		private List<String> groups;
		
		public void setUsername(String username) {
			this.username = username;
		}
		public void setUid(String uid) {
			this.uid = uid;
		}
		public void setGroups(List<String> groups) {
			this.groups = groups;
		}
		public String getUsername() {
			return username;
		}
		public String getUid() {
			return uid;
		}
		public List<String> getGroups() {
			return groups;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ResponseStatus {
		private int code;
		private String status;
		private String reason;
		private String message;
		
		public void setCode(int code) {
			this.code = code;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public void setReason(String reason) {
			this.reason = reason;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public int getCode() {
			return code;
		}
		public String getStatus() {
			return status;
		}
		public String getReason() {
			return reason;
		}
		public String getMessage() {
			return message;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class EventList {
		private String kind;
		private String apiVersion;
		private List<Event> items;
		private long totalNum;
		
		public String getKind() {
			return kind;
		}
		public void setKind(String kind) {
			this.kind = kind;
		}
		public String getApiVersion() {
			return apiVersion;
		}
		public void setApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;
		}
		public long getTotalNum() {
			return totalNum;
		}
		public void setTotalNum(long totalNum) {
			this.totalNum = totalNum;
		}
		public List<Event> getItems() {
			return items;
		}
		public void setItems(List<Event> items) {
			this.items = items;
		}
		
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Event {
		private String level;
		private String auditID;
		private String stage;
		private String requestURI;
		private String verb;
		private User user;
		private List<String> sourceIPs;
		private String userAgent;
		private ObjectReference objectRef;
		private ResponseStatus responseStatus;
		private DateTime requestReceivedTimestamp;
		private DateTime stageTimestamp;
		private Map<String, String> annotations;

		public String getLevel() {
			return level;
		}
		public void setLevel(String level) {
			this.level = level;
		}
		public String getAuditID() {
			return auditID;
		}
		public void setAuditID(String auditID) {
			this.auditID = auditID;
		}
		public String getStage() {
			return stage;
		}
		public void setStage(String stage) {
			this.stage = stage;
		}
		public String getRequestURI() {
			return requestURI;
		}
		public void setRequestURI(String requestURI) {
			this.requestURI = requestURI;
		}
		public String getVerb() {
			return verb;
		}
		public void setVerb(String verb) {
			this.verb = verb;
		}
		public User getUser() {
			return user;
		}
		public void setUser(User user) {
			this.user = user;
		}
		public List<String> getSourceIPs() {
			return sourceIPs;
		}
		public void setSourceIPs(List<String> sourceIPs) {
			this.sourceIPs = sourceIPs;
		}
		public String getUserAgent() {
			return userAgent;
		}
		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}
		public ObjectReference getObjectRef() {
			return objectRef;
		}
		public void setObjectRef(ObjectReference objectRef) {
			this.objectRef = objectRef;
		}
		public ResponseStatus getResponseStatus() {
			return responseStatus;
		}
		public void setResponseStatus(ResponseStatus responseStatus) {
			this.responseStatus = responseStatus;
		}
		public DateTime getRequestReceivedTimestamp() {
			return requestReceivedTimestamp;
		}
		public void setRequestReceivedTimestamp(DateTime requestReceivedTimestamp) {
			this.requestReceivedTimestamp = requestReceivedTimestamp;
		}
		public DateTime getStageTimestamp() {
			return stageTimestamp;
		}
		public void setStageTimestamp(DateTime stageTimestamp) {
			this.stageTimestamp = stageTimestamp;
		}
		public Map<String, String> getAnnotations() {
			return annotations;
		}
		public void setAnnotations(Map<String, String> annotations) {
			this.annotations = annotations;
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ObjectReference {
		private String resource;
		private String namespace;
		private String name;
		private String uid;
		private String apiGroup;
		private String apiVersion;
		private String resourceVerion;
		private String subresource;
		
		public String getResource() {
			return resource;
		}
		public void setResource(String resource) {
			this.resource = resource;
		}
		public String getNamespace() {
			return namespace;
		}
		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getUid() {
			return uid;
		}
		public void setUid(String uid) {
			this.uid = uid;
		}
		public String getApiGroup() {
			return apiGroup;
		}
		public void setApiGroup(String apiGroup) {
			this.apiGroup = apiGroup;
		}
		public String getApiVersion() {
			return apiVersion;
		}
		public void setApiVersion(String apiVersion) {
			this.apiVersion = apiVersion;
		}
		public String getResourceVerion() {
			return resourceVerion;
		}
		public void setResourceVerion(String resourceVerion) {
			this.resourceVerion = resourceVerion;
		}
		public String getSubresource() {
			return subresource;
		}
		public void setSubresource(String subresource) {
			this.subresource = subresource;
		}
		
	}
	
	public static class DateTimeSerializer implements JsonSerializer<DateTime> {

		@Override
		public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {	
			return new JsonPrimitive(src.toString("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"));
		}

	}
	
	public static class DateTimeFormatModule extends SimpleModule {
		public DateTimeFormatModule() {
			super();
			addSerializer(DateTime.class, new com.fasterxml.jackson.databind.JsonSerializer<DateTime>() {
				public final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
						.withLocale(Locale.ROOT)
						.withChronology(ISOChronology.getInstanceUTC());

				@Override
				public void serialize(DateTime value, JsonGenerator gen, SerializerProvider serializers)
						throws IOException, JsonProcessingException {
					if (value == null) {
						gen.writeNull();
					} else {
						gen.writeString(FORMATTER.print(value));
					}
				}
			});

			addDeserializer(DateTime.class, new JsonDeserializer<DateTime>() {
				public final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
						.withLocale(Locale.ROOT)
						.withChronology(ISOChronology.getInstanceUTC());

				@Override
				public DateTime deserialize(JsonParser p, DeserializationContext ctxt)
						throws IOException, JsonProcessingException {
					JsonToken t = p.getCurrentToken();

					if (t == JsonToken.VALUE_NUMBER_INT) {
						return new DateTime(p.getLongValue());
					}

					if (t == JsonToken.VALUE_STRING) {
						String str = p.getText();
						if (StringUtil.isEmpty(str)) {
							return null;
						}
						return FORMATTER.parseDateTime(str);
					}
					
					throw ctxt.mappingException(DateTime.class);
				}
			});
		}
	}
	
	

}