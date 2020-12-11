package k8s.example.client;

public class Constants {
	
	// MutatingHandler,ValidatingHandler
	public static final String ADMISSION_REVIEW_VERSION = "admission.k8s.io/v1beta1";
	public static final String ANNOTATION_PATH_CREATOR = "/metadata/annotations/creator";
	public static final String ANNOTATION_PATH_UPDATER = "/metadata/annotations/updater";
	public static final String ANNOTATION_PATH_CREATEDTIME = "/metadata/annotations/createdTime";
	public static final String ANNOTATION_PATH_UPDATEDTIME = "/metadata/annotations/updatedTime";
	public static final String ANNOTATION_UPDATE_EXCEPTION = "Users cannot create or patch creator/updater/createdTime/updatedTime.";
	public static final String ANNOTATION_NOT_EXISTED_EXCEPTION = "do not have user information in annotations (Some of creator/updater/createdTime/updatedTime are missing).";
	
	public static final String TEMPLATE_NAMESPACE = "hypercloud4-system";
	public static final String DEFAULT_NAMESPACE = "default";
	public static final String SYSTEM_ENV_CATALOG_NAMESPACE = "CATALOG_NAMESPACE";
	public static final String REGISTRY_NAMESPACE = "hypercloud4-system";

	public static final String CUSTOM_OBJECT_GROUP = "tmax.io";
	public static final String CUSTOM_OBJECT_VERSION = "v1";
	public static final String CUSTOM_OBJECT_PLURAL_USER = "users";
	public static final String CUSTOM_OBJECT_PLURAL_USER_SECURITY_POLICY = "usersecuritypolicies";
	public static final String CUSTOM_OBJECT_PLURAL_TOKEN = "tokens";
	public static final String CUSTOM_OBJECT_PLURAL_TEMPLATE = "templates";
	public static final String CUSTOM_OBJECT_PLURAL_TEMPLATE_INSTANCE = "templateinstances";
	public static final String CUSTOM_OBJECT_PLURAL_CLIENT = "clients";
	public static final String CUSTOM_OBJECT_KIND_TEMPLATE_INSTANCE = "TemplateInstance";
	public static final String CUSTOM_OBJECT_PLURAL_REGISTRY = "registries";
	public static final String CUSTOM_OBJECT_PLURAL_IMAGE = "images";
	public static final String CUSTOM_OBJECT_PLURAL_NAMESPACECLAIM = "namespaceclaims";
	public static final String CUSTOM_OBJECT_PLURAL_RESOURCEQUOTACLAIM = "resourcequotaclaims";
	public static final String CUSTOM_OBJECT_PLURAL_ROLEBINDINGCLAIM = "rolebindingclaims";
	
	// Audit Get Query Parameters
	public static final String QUERY_PARAMETER_OFFSET = "offset";
	public static final String QUERY_PARAMETER_LIMIT = "limit";
	public static final String QUERY_PARAMETER_NAMESPACE = "namespace";
	public static final String QUERY_PARAMETER_TIMEUNIT = "timeUnit";
	public static final String QUERY_PARAMETER_STARTTIME = "startTime";
	public static final String QUERY_PARAMETER_ENDTIME = "endTime";
	public static final String QUERY_PARAMETER_SORT = "sort";
	public static final String QUERY_PARAMETER_RESOURCE = "resource";
	public static final String QUERY_PARAMETER_CODE = "code";
	public static final String QUERY_PARAMETER_ACTION = "verb";
	
	// Mysql DB Connection
	public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DB_URL = "jdbc:mysql://mysql-service.hypercloud4-system:3306/metering?useSSL=false";
	public static final String USERNAME = "root";
	
	
}
