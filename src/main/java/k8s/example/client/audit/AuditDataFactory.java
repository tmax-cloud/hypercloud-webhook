package k8s.example.client.audit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import k8s.example.client.Constants;
import k8s.example.client.Util;
import k8s.example.client.audit.AuditDataObject.Event;
import k8s.example.client.audit.AuditDataObject.ObjectReference;
import k8s.example.client.audit.AuditDataObject.ResponseStatus;
import k8s.example.client.audit.AuditDataObject.User;
import k8s.example.client.util.LogPreparedStatement;
import k8s.example.client.util.SimpleUtil;
import k8s.example.client.util.StringUtil;

public class AuditDataFactory {

	private static Logger logger = AuditController.logger;
	private static Connection conn = null;
	
	static {
		try {
			Class.forName(Constants.JDBC_DRIVER);
			conn = DriverManager.getConnection(Constants.DB_URL, Constants.USERNAME, System.getenv("DB_PASSWORD"));
			conn.setAutoCommit(false);

		} catch(Exception e) {
			logger.error("Failed to get connection, check stack trace: \n" + Util.printExceptionError(e));
		}
	}
	
	private static final String AUDIT_INSERT_QUERY = "insert into metering.audit values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private static final String AUDIT_SELECT_QUERY = "select SQL_CALC_FOUND_ROWS * from metering.audit where 1=1 ";
	private static final String AUDIT_SELECT_CNT_QUERY = "select FOUND_ROWS()";
	
	public static void insert(List<Event> eventList) throws Exception {
		int i = 1;
		try(LogPreparedStatement pstmt = new LogPreparedStatement(conn, AUDIT_INSERT_QUERY)){
			for(Event event: eventList) {
				i = 1;
				pstmt.setString(i++, event.getAuditID());
				pstmt.setString(i++, event.getUser().getUsername());
				pstmt.setString(i++, event.getUserAgent());
				ObjectReference objectRef = event.getObjectRef();
				pstmt.setString(i++, objectRef == null ? "" : objectRef.getNamespace());
				pstmt.setString(i++, objectRef == null ? "" : objectRef.getApiGroup());
				pstmt.setString(i++, objectRef == null ? "" : objectRef.getApiVersion());
				pstmt.setString(i++, objectRef == null ? "" : objectRef.getResource());
				pstmt.setString(i++, objectRef == null ? "" : objectRef.getName());
				pstmt.setString(i++, event.getStage());
				pstmt.setTimestamp(i++, new Timestamp(event.getStageTimestamp().getMillis()));
				pstmt.setString(i++, event.getVerb());
				ResponseStatus responseStatus = event.getResponseStatus();
				pstmt.setInt(i++, responseStatus == null ? 0 : responseStatus.getCode());
				pstmt.setString(i++, responseStatus == null ? "" : responseStatus.getStatus());
				pstmt.setString(i++, responseStatus == null ? "" : responseStatus.getReason());
				pstmt.setString(i++, responseStatus == null ? "" : responseStatus.getMessage());
										
				logger.debug("Query=\"" + pstmt.getQueryString() + "\"");
				pstmt.addBatch();
			}
			int[] result = pstmt.executeBatch();
			conn.commit();
		}
	}
	
	public static List<Event> select(Map<String, List<String>> query) throws Exception {
		String offset = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_OFFSET);
		String limit = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_LIMIT);
		String startTime = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_STARTTIME);
		String endTime = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_ENDTIME);
		String namespace = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_NAMESPACE);
		List<String> sort = SimpleUtil.getQueryParameterArray(query, Constants.QUERY_PARAMETER_SORT);
		String resource = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_RESOURCE);
		String code = SimpleUtil.getQueryParameter(query, Constants.QUERY_PARAMETER_CODE);
		query.remove(Constants.QUERY_PARAMETER_OFFSET);
		query.remove(Constants.QUERY_PARAMETER_LIMIT);
		query.remove(Constants.QUERY_PARAMETER_STARTTIME);
		query.remove(Constants.QUERY_PARAMETER_ENDTIME);
		query.remove(Constants.QUERY_PARAMETER_NAMESPACE);
		query.remove(Constants.QUERY_PARAMETER_SORT);
		query.remove(Constants.QUERY_PARAMETER_RESOURCE);
		query.remove(Constants.QUERY_PARAMETER_CODE);

		
		StringBuilder sb = new StringBuilder(AUDIT_SELECT_QUERY);
		query.forEach((key, value) -> sb.append("and ").append(key).append(" like '%").append(value.get(0)).append("%' "));
		
		if(StringUtil.isNotEmpty(startTime) && StringUtil.isNotEmpty(endTime)) {
			sb.append("and stagetimestamp between '").append(new Timestamp(Long.parseLong(startTime))).append("' and '").append(new Timestamp(Long.parseLong(endTime))).append("' ");
		}
		
		if(StringUtil.isNotEmpty(namespace)) {
			sb.append("and namespace = '").append(namespace).append("' ");
		}
		
		if(StringUtil.isNotEmpty(resource)) {
			sb.append("and resource = '").append(resource).append("' ");
		}
		
		if(StringUtil.isNotEmpty(code)) {
			 int codeInt = Integer.parseInt(code);
			 int lowerBound = (codeInt / 100) * 100;
			 int upperBound = lowerBound + 99;
			 sb.append("and code between '").append(lowerBound).append("' and '").append(upperBound).append("' ");
		}
		
		if(sort != null && sort.size() > 0){
			sb.append("order by ");
			for (String s : sort) {
				String order =" asc, ";
				if (s.charAt(0) == '-') {
					order = " desc, ";
					s = s.substring(1, s.length());
				}
				sb.append(s);
				sb.append(order);
			}
			sb.append("stagetimestamp desc ");
		} else {
			sb.append("order by stagetimestamp desc ");
		}
		
		if(StringUtil.isNotEmpty(limit)) {
			sb.append("limit ").append(limit).append(" ");
		} else {
			sb.append("limit 100 ");
		}
		
		if(StringUtil.isNotEmpty(offset)) {
			sb.append("offset ").append(offset);
		} else {
			sb.append("offset 0");
		}
		
		List<Event> result = new ArrayList<>();
		try(LogPreparedStatement pstmt = new LogPreparedStatement(conn, sb.toString())){
			logger.debug("Query=\"" + pstmt.getQueryString() + "\"");
			try(ResultSet rs = pstmt.executeQuery()) {
				while(rs.next()) {
					Event event = new Event();
					event.setAuditID(rs.getString("id"));
					event.setUser(new User());
					event.getUser().setUsername(rs.getString("username"));
					event.setUserAgent(rs.getString("useragent"));
					event.setObjectRef(new ObjectReference());
					event.getObjectRef().setNamespace(rs.getString("namespace"));
					event.getObjectRef().setApiGroup(rs.getString("apigroup"));
					event.getObjectRef().setApiVersion(rs.getString("apiversion"));
					event.getObjectRef().setResource(rs.getString("resource"));
					event.getObjectRef().setName(rs.getString("name"));
					event.setStage(rs.getString("stage"));
					event.setStageTimestamp(new DateTime(rs.getTimestamp("stagetimestamp")));
					event.setVerb(rs.getString("verb"));
					event.setResponseStatus(new ResponseStatus());
					event.getResponseStatus().setCode(rs.getInt("code"));
					event.getResponseStatus().setStatus(rs.getString("status"));
					event.getResponseStatus().setReason(rs.getString("reason"));
					event.getResponseStatus().setMessage(rs.getString("message"));
					result.add(event);
				}
			}
		}
		return result;
	}
	
	public static long selectCnt() throws Exception {
		StringBuilder sb = new StringBuilder(AUDIT_SELECT_CNT_QUERY);
		long result;
		
		try(LogPreparedStatement pstmt = new LogPreparedStatement(conn, sb.toString())){
			logger.debug("Query=\"" + pstmt.getQueryString() + "\"");
			try(ResultSet rs = pstmt.executeQuery()) {
				rs.next();
				result = rs.getLong("FOUND_ROWS()");
			}
		}
		return result;
	}
}
