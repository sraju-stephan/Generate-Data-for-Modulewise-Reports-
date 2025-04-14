package com;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.sql.rowset.CachedRowSet;

import com.sun.rowset.CachedRowSetImpl;

public class ScheduleReports {
	private Connection conn;	
	
	public static void main(String[] args) throws Exception
	{
		ScheduleReports schedReports = new ScheduleReports();
		schedReports.scheduleReports();
	}
	
	private int[] generateIds() throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException
	{
		String reportIdQuery = "SELECT MS_BATCH_ACTIVITY_LOG_SEQ.NEXTVAL FROM DUAL";
		CachedRowSet reportIdRst = dataBaseOracleConnectionSelect(reportIdQuery);
		reportIdRst.first();
		int reportId = reportIdRst.getInt(1);
		String criteriaIdQuery = "SELECT MS_CRITERIA_HDR_SEQ.NEXTVAL FROM DUAL";
		CachedRowSet criteriaIdRst = dataBaseOracleConnectionSelect(criteriaIdQuery);
		criteriaIdRst.first();
		int criteriaId = criteriaIdRst.getInt(1);
		int[] Ids = {reportId, criteriaId};
		System.out.println("Report Id : " + Ids[0] + ", Criteria Id: " + Ids[1]);
		return Ids;	
	}
	
	private void scheduleReports() throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException, InterruptedException
	{
		int[] Ids; int reportId, criteriaId, rId;
		try{
			String reportsIdsQuery = "";
			
			String reportFolders = getSettingsFromConfig("ReportFolder");
			String[] reportIdsAry = reportFolders.split(",");
			for (int i = 0; i < reportIdsAry.length; i++)
			{
			String reportFolder=reportIdsAry[i];
			
			if (reportFolder == null || reportFolder.equals(""))
				reportsIdsQuery = "SELECT RID FROM TMP_SCHED_REPORTS WHERE RSTATUS = 'Y'";
			else
				reportsIdsQuery = "SELECT RID FROM TMP_SCHED_REPORTS WHERE RFOLDER = '" + reportFolder + "'";
			CachedRowSet reportIdsRst = dataBaseOracleConnectionSelect(reportsIdsQuery);
			reportIdsRst.beforeFirst();
			while(reportIdsRst.next())
			{
				Ids = generateIds();
				reportId = Ids[0];
				criteriaId = Ids[1];
				rId = reportIdsRst.getInt(1);
				updateIdsInSchedReports(rId, reportId, criteriaId);
				updateIdsInCriteriaDtl(rId, criteriaId);
				updateIdsInCriteriaDisplay(rId, criteriaId);
				updateIdsInQueryDisplay(rId, criteriaId);
				updateIdsInQueryCriteria(rId, criteriaId);
				updateIdsInCriteriaHdr(rId, criteriaId);
				updateIdsInDistList(rId, reportId);
				insertDataIntoMarsTables(rId);
			}
			}
			System.out.println("Reports Scheduled Successfully");
		}catch (Exception e) {
			System.out.println("Error in updating temp tables!");
		}
		conn.close();
		conn = null;
	}
	
	private void updateIdsInSchedReports(int rId, int reportId, int criteriaId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String strQuery = "";  
		strQuery = "UPDATE TMP_SCHED_REPORTS SET REPORT_ID = " + reportId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(strQuery);
		strQuery = "UPDATE TMP_SCHED_REPORTS SET CRITERIA_ID = " + criteriaId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(strQuery);
		String reportTitle = "REPORT " + getTimeStamp();
		Thread.sleep(2000);
		strQuery = "UPDATE TMP_SCHED_REPORTS SET REPORT_NAME = '" + reportTitle + "' WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(strQuery);
		strQuery = "SELECT RNAME FROM TMP_SCHED_REPORTS" + " WHERE RID = " + rId;
		CachedRowSet rset = dataBaseOracleConnectionSelect(strQuery);
		rset.first();
		String reportName = rset.getString(1);
		System.out.println("Report Name : " + reportName + ", Report Title : " + reportTitle);
		strQuery = "UPDATE TMP_SCHED_REPORTS SET DATE_SCHED = SYSDATE WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(strQuery);
		strQuery = "UPDATE TMP_SCHED_REPORTS SET HOLD_UNTIL_TIME = SYSDATE WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(strQuery);
	}
	
	private void updateIdsInCriteriaDisplay(int rId, int criteriaId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = "UPDATE TMP_CRITERIA_DISPLAY SET CRITERIA_ID = " + criteriaId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(updateQuery);
	}
	
	private void updateIdsInCriteriaHdr(int rId, int criteriaId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = "UPDATE TMP_CRITERIA_HDR SET CRITERIA_ID = " + criteriaId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(updateQuery);		
	}
	
	private void updateIdsInDistList(int rId, int reportId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = "UPDATE TMP_RUN_REPORTS_DIST_LIST SET REPORT_ID = " + reportId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(updateQuery);	
	}
	
	private void updateIdsInCriteriaDtl(int rId, int criteriaId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = "UPDATE TMP_CRITERIA_DTL SET CRITERIA_ID = " + criteriaId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(updateQuery);	
	}
	
	private void updateIdsInQueryDisplay(int rId, int criteriaId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = "UPDATE TMP_QUERY_DISPLAY SET CRITERIA_ID = " + criteriaId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(updateQuery);
	}
	
	private void updateIdsInQueryCriteria(int rId, int criteriaId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = "UPDATE TMP_QUERY_CRITERIA SET CRITERIA_ID = " + criteriaId + " WHERE RID = " + rId;
		dataBaseOracleConnectionUpdate(updateQuery);	
	}
	
	private void insertDataIntoMarsTables(int rId) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, InterruptedException, SQLException
	{
		String updateQuery = ""; 
		updateQuery = "INSERT INTO MS_CRITERIA_HDR SELECT CRITERIA_ID, SCREEN_ID, CRITERIA_CAT, CRITERIA_NAME, CRITERIA_COMMENT, OWNER, PUBLIC_FLG, CRITERIA_TYPE_CD, BATCH_FLG, DIST_LIST_ID, DIST_USRID, PURGE_DAYS, UPD_DT, EMAIL_IMPORT_STRING FROM TMP_CRITERIA_HDR WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery);  
		updateQuery = "INSERT INTO MS_CRITERIA_DTL SELECT CRITERIA_ID, FIELD_NM, FIELD_VALUE_TEXT, FIELD_VALUE_DATE, FIELD_VALUE_NUMR, FIELD_VALUE_LIST_ID, CRITERIA_HANDLING_CD, FIELD_VALUE_CLOB, CRITERIA_FIELD_ID, CRITERIA_DESC FROM TMP_CRITERIA_DTL WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery); 
		updateQuery = "INSERT INTO MS_CRITERIA_DISPLAY SELECT CRITERIA_ID, DISPLAY_FIELD_ID, DISPLAY_SEQ, REQUIRED_FLG FROM TMP_CRITERIA_DISPLAY WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery);
		updateQuery = "INSERT INTO MS_QUERY_CRITERIA SELECT CRITERIA_ID, CRITERIA_ROW, CRITERIA_SEQ, FIELD_NM, FIELD_VALUE_TEXT, FIELD_VALUE_DATE, FIELD_VALUE_NUMR, FIELD_VALUE_LIST_ID, CRITERIA_HANDLING_CD, CRITERIA_FIELD_ID, CRITERIA_DESC, FIELD_VALUE_DATE_UTC FROM TMP_QUERY_CRITERIA WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery); 
		updateQuery = "INSERT INTO MS_QUERY_DISPLAY SELECT CRITERIA_ID, DISPLAY_FIELD_ID, DISPLAY_SEQ, SORT_ID, SORT_CD FROM TMP_QUERY_DISPLAY WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery);
		updateQuery = "INSERT INTO MS_RUN_REPORTS_DIST_LIST SELECT REPORT_ID, USR_ID, USER_NOTIFIED FROM TMP_RUN_REPORTS_DIST_LIST WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery);
		updateQuery = "INSERT INTO MS_SCHED_REPORTS SELECT REPORT_ID, REPORT_NAME, COMMENTS, USERID, DATE_SCHED, PURGE_DAYS, HOLD_UNTIL_TIME, SCREEN_ID, STAT_CD, DIST_LIST_ID, DIST_USRID, FIRM_ID, OFF_ID, REP_ID, CUST_ID, ACCOUNT_ID, BATCH_TYPE, CRITERIA_ID, IMPORT_STRING, PRIORITY, RECURRING_RPT_ID, DIST_TYPE, DIST_ROLE_CD, SCENARIO_STRING, SCENARIO_ID FROM TMP_SCHED_REPORTS WHERE RID = '" + rId + "'";
		dataBaseOracleConnectionUpdate(updateQuery);
	}
	
	
	private String getTimeStamp() { 
		String today; 
		DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss"); 
		Calendar calendar = Calendar.getInstance(); 
		today = dateFormat.format(calendar.getTime()); 
		return today; 
	}

	private Connection getDataSourceOracle() throws SQLException	//Added Throws SQLException
	, InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, IOException
		{		
			String testExecutionOn = getSettingsFromConfig("TestExecutionOn");
			try
			{
				if (conn != null && !conn.isClosed())
					return conn;
			}
			catch(Exception ex)
			{
				System.out.println(ex.getLocalizedMessage());
			}
			
			try
			{
				String strDbUrl;
				String[] UrlAry = testExecutionOn.split("-");
				try {
					strDbUrl = getSettingsFromConfig("DbUrl" + "-" + UrlAry[1]);
				}
				catch (Exception e) {
					System.out.println("Database-Warning: Default value of DbUrl is used!");
					strDbUrl = getSettingsFromConfig("DbUrl");
				}
				Class.forName("oracle.jdbc.driver.OracleDriver"); 
				conn=DriverManager.getConnection(strDbUrl, "mars", "mars");	
				conn.setAutoCommit(true);	
			}
			catch(SQLException e) 
			{
				String connectMsg = "Could not connect to the database: " + e.getMessage() + " "  + e.getLocalizedMessage();
				System.out.println(connectMsg);
				throw (e);
			}
			return conn;		
		}

	private CachedRowSet dataBaseOracleConnectionSelect(String StrQuery) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, IOException	
	{
		Connection connection = getDataSourceOracle();
		CachedRowSet crset =  new CachedRowSetImpl();	
		crset.setCommand(StrQuery);	
		crset.execute(connection);
		return crset;		
	}
	
	private void dataBaseOracleConnectionUpdate(String StrQuery) throws ClassNotFoundException, IOException, InterruptedException, SQLException, InstantiationException, IllegalAccessException
	{
		try{
			//Use the single connection 
			Connection connection = getDataSourceOracle();	
			//Statement stmtUpdate = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);	
			Statement stmtUpdate = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			stmtUpdate.executeUpdate(StrQuery);
			//conn.setAutoCommit(true);
			stmtUpdate.close();
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
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
