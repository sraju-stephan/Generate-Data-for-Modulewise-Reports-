package com;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class InsertDataIntoTempTables {
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException {
		
		InsertDataIntoTempTables insertIntoTables = new InsertDataIntoTempTables();
		String reportIds = insertIntoTables.getSettingsFromConfig("ReportIds");
		String[] reportIdsAry = reportIds.split(",");
		for (int i = 0; i < reportIdsAry.length; i++)
			insertIntoTables.replaceVariables(reportIdsAry[i].trim());
	}
	
	public void replaceVariables(String reportId) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException {
				
		String str = "INSERT INTO TMP_SCHED_REPORTS (REPORT_ID, REPORT_NAME, COMMENTS, USERID, DATE_SCHED, PURGE_DAYS, HOLD_UNTIL_TIME, SCREEN_ID, STAT_CD, DIST_LIST_ID, DIST_USRID, FIRM_ID, OFF_ID, REP_ID, CUST_ID, ACCOUNT_ID, BATCH_TYPE, CRITERIA_ID, IMPORT_STRING, PRIORITY, RECURRING_RPT_ID, DIST_TYPE, DIST_ROLE_CD, SCENARIO_STRING, SCENARIO_ID) SELECT * FROM MS_SCHED_REPORTS WHERE REPORT_ID = 'CRPTID'; INSERT INTO TMP_RUN_REPORTS_DIST_LIST (REPORT_ID, USR_ID, USER_NOTIFIED) SELECT * FROM MS_RUN_REPORTS_DIST_LIST WHERE REPORT_ID = 'CRPTID'; INSERT INTO TMP_CRITERIA_HDR (CRITERIA_ID, SCREEN_ID, CRITERIA_CAT, CRITERIA_NAME, CRITERIA_COMMENT, OWNER, PUBLIC_FLG, CRITERIA_TYPE_CD, BATCH_FLG, DIST_LIST_ID, DIST_USRID, PURGE_DAYS, UPD_DT, EMAIL_IMPORT_STRING) SELECT * FROM MS_CRITERIA_HDR WHERE CRITERIA_ID = 'CCRAID'; INSERT INTO TMP_CRITERIA_DISPLAY (CRITERIA_ID, DISPLAY_FIELD_ID, DISPLAY_SEQ, REQUIRED_FLG) SELECT * FROM MS_CRITERIA_DISPLAY WHERE CRITERIA_ID = 'CCRAID'; INSERT INTO TMP_CRITERIA_DTL (CRITERIA_ID, FIELD_NM, FIELD_VALUE_TEXT, FIELD_VALUE_DATE, FIELD_VALUE_NUMR, FIELD_VALUE_LIST_ID, CRITERIA_HANDLING_CD, FIELD_VALUE_CLOB, CRITERIA_FIELD_ID, CRITERIA_DESC) SELECT * FROM MS_CRITERIA_DTL WHERE CRITERIA_ID = 'CCRAID'; INSERT INTO TMP_QUERY_DISPLAY (CRITERIA_ID, DISPLAY_FIELD_ID, DISPLAY_SEQ, SORT_ID, SORT_CD) SELECT * FROM MS_QUERY_DISPLAY WHERE CRITERIA_ID = 'CCRAID'; INSERT INTO TMP_QUERY_CRITERIA (CRITERIA_ID, CRITERIA_ROW, CRITERIA_SEQ, FIELD_NM, FIELD_VALUE_TEXT, FIELD_VALUE_DATE, FIELD_VALUE_NUMR, FIELD_VALUE_LIST_ID, CRITERIA_HANDLING_CD, CRITERIA_FIELD_ID, CRITERIA_DESC, FIELD_VALUE_DATE_UTC) SELECT * FROM MS_QUERY_CRITERIA WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_SCHED_REPORTS SET RID = CRID WHERE REPORT_ID = 'CRPTID'; UPDATE TMP_RUN_REPORTS_DIST_LIST SET RID = CRID WHERE REPORT_ID = 'CRPTID'; UPDATE TMP_CRITERIA_HDR SET RID = CRID WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_CRITERIA_DISPLAY SET RID = CRID WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_CRITERIA_DTL SET RID = CRID WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_QUERY_DISPLAY SET RID = CRID WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_QUERY_CRITERIA SET RID = CRID WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_SCHED_REPORTS SET RNAME = (SELECT REPORT_NAME FROM TMP_SCHED_REPORTS WHERE CRITERIA_ID = 'CCRAID') WHERE CRITERIA_ID = 'CCRAID'; UPDATE TMP_SCHED_REPORTS SET RSTATUS = 'N' WHERE RID = CRID;";
		String cntr = selectResultFromDataBase("SELECT MAX(RID)+1 FROM TMP_SCHED_REPORTS");
		str = str.replaceAll("CRID", cntr);
		str = str.replaceAll("CRPTID", reportId);
		String criteriaId = selectResultFromDataBase("SELECT CRITERIA_ID FROM MS_SCHED_REPORTS WHERE REPORT_ID = '"+ reportId + "'");
		str = str.replaceAll("CCRAID", criteriaId);
		String[] strAry = str.split(";");
		for(int i = 0; i < strAry.length; i++)
		{
			updateResultIntoDataBase(strAry[i]);
			System.out.println(strAry[i]);
		}
	}
	
	private String selectResultFromDataBase(String strQuery) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException
	{
		String queryResult = "";
		try
		{
			Connection conn = getDataSourceOracle();
			ResultSet execRset = null;
			Statement execStmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);	
			execRset = execStmt.executeQuery(strQuery);
			execRset.first();
			queryResult = execRset.getString(1);
			execStmt.close();
			execRset.close();
			conn.close();
		}
		catch (Exception e) 
		{
			System.out.println(e.getLocalizedMessage());
		}
		return queryResult;
	}
	
	private boolean updateResultIntoDataBase(String strQuery) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException
	{
		boolean isSuccessful = true;
		try
		{
			Connection conn = getDataSourceOracle();	
			Statement execStmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);	
			execStmt.executeUpdate(strQuery);
			conn.setAutoCommit(true);
			execStmt.close();
			conn.close();
		}
		catch (Exception e) 
		{
			isSuccessful = false;
		}
		return isSuccessful;
	}

	private Connection getDataSourceOracle() throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver"); 
		Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@192.168.169.101:1741:IND104", "mars", "mars");	
		conn.setAutoCommit(true);
		return conn;
	}
	
	private String getSettingsFromConfig(String strKey) throws FileNotFoundException, IOException 
	{
		Properties prop = new Properties();
		prop.load(new FileInputStream("Utilities.properties"));
		String strData = prop.getProperty(strKey);
		strData = strData.trim();
		return strData;
	}
}
