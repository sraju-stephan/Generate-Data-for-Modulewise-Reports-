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

import javax.sql.rowset.CachedRowSet;

import com.sun.rowset.CachedRowSetImpl;

public class GenericTestDataUpdation {
	private Connection conn;
	private String testExecutionOn;
	
	public static void main(String[] args) throws Exception
	{
		GenericTestDataUpdation  gt = new GenericTestDataUpdation();
		gt.updateTestData();
	}
	
	public void updateTestData() throws Exception
	{
		String strQuery = null;
		String expectedTestdatafor="SFDC";
		testExecutionOn = getSettingsFromConfig("TestExecutionOn");
		String testDataTable = getSettingsFromConfig("TestDataTable");
		strQuery = "SELECT DATA_VALUE FROM " + testDataTable + " WHERE DATA_KEY = 'SuffixDateGeneration'";
		String suffixDateGeneration = selectResultFromDataBase(strQuery);
		strQuery = "SELECT DATA_VALUE FROM " + testDataTable + " WHERE DATA_KEY = 'SuffixDateModification'";
		String suffixDateModification = selectResultFromDataBase(strQuery);
		strQuery = "SELECT DATA_KEY,DATA_VALUE FROM " + testDataTable + " WHERE SECTION = 'Generic' AND ISVARIED = 'G'";
		updateDataValues(strQuery, suffixDateGeneration, testDataTable);
		strQuery = "SELECT DATA_KEY,DATA_VALUE FROM " + testDataTable + " WHERE SECTION = 'Generic' AND ISVARIED = 'M'";
		updateDataValues(strQuery, suffixDateModification, testDataTable);
		
		String testdatafor=getSettingsFromConfig("TestExecutionfor");
		if (testdatafor.equalsIgnoreCase(expectedTestdatafor))
		{
			
			strQuery = "SELECT DATA_VALUE FROM " + testDataTable + " WHERE DATA_KEY = 'SuffixSfdcDateGeneration'";
			String SuffixSfdcDateGeneration = selectResultFromDataBase(strQuery);
			strQuery = "SELECT DATA_KEY,DATA_VALUE FROM " + testDataTable + " WHERE SECTION = 'Generic' AND ISVARIED = 'S'";
			updateDataValues(strQuery, SuffixSfdcDateGeneration, testDataTable);
		}
		
		
	}
	
	private void updateDataValues(String strQuery, String suffixDate, String testDataTable) throws Exception
	{
		CachedRowSet dataSets = dataBaseOracleConnectionSelect(strQuery);
		int noOfDataSets = getRecordCount(dataSets);
		System.out.println("Total data keys : " + noOfDataSets);
		while (dataSets.next())
		{
			String dataKey = dataSets.getString(1);
		    String dataValue = dataSets.getString(2);
			String oldSuffixDate  = dataValue.replaceAll("[a-zA-Z[^0-9]]", "");
			if (!oldSuffixDate.equals(""))
			{
				String newDataValue = dataValue.replace(oldSuffixDate, suffixDate);
				strQuery = "UPDATE " + testDataTable + " SET DATA_VALUE = '" + newDataValue + "' WHERE DATA_KEY = '" + dataKey + "' AND SECTION = 'Generic'";
	        	updateResultIntoDataBase(strQuery);
	        	System.out.println("Updated data value - " + dataKey + ":" + newDataValue);
			}
			else
				System.out.println(dataKey + " - data key value does not have date suffix!");
        }
	}
	
	private int getRecordCount(CachedRowSet rset) throws Exception 
	{
		int nRowCount = 0;
		rset.last();
		nRowCount = rset.getRow();
		rset.beforeFirst();
		return nRowCount;
	}

	private Connection getDataSourceOracle() throws SQLException	//Added Throws SQLException
	, InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, IOException
		{		
			try
			{
				if (conn != null && !conn.isClosed())
					return conn;
			}
			catch(Exception ex)
			{
				System.out.println(ex.getLocalizedMessage());
			}
			
			//Connection jdbcConnection = null;	
			try
			{
				String strDbUrl, strDbUser, strDbPassword; 
				String[] UrlAry = testExecutionOn.split("-");
				try {
					strDbUrl = getSettingsFromConfig("DbUrl" + "-" + UrlAry[1]);
				}
				catch (Exception e) {
					System.out.println("Database-Warning: Error in getting AMARS TNS!");
					strDbUrl = getSettingsFromConfig("DbUrl");
				}
				strDbUser = getSettingsFromConfig("DbUser");
				strDbPassword = getSettingsFromConfig("DbPassword");
				
				Class.forName("oracle.jdbc.driver.OracleDriver"); 
				conn=DriverManager.getConnection(strDbUrl, strDbUser, strDbPassword);	
				conn.setAutoCommit(true);	
				//jdbcConnection = DriverManager.getConnection(strDbUrl, strDbUser, strDbPassword);	
				//jdbcConnection.setAutoCommit(true);	
			
			}
			catch(SQLException e) 
			{
				String connectMsg = "Could not connect to the database: " + e.getMessage() + " "  + e.getLocalizedMessage();
				System.out.println(connectMsg);
				throw (e);
			}
			//return jdbcConnection;	
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

	private String getSettingsFromConfig(String strKey) throws FileNotFoundException, IOException 
	{
		Properties prop = new Properties();
		prop.load(new FileInputStream("Utilities.properties"));
		String strData = prop.getProperty(strKey);
		strData = strData.trim();
		return strData;
	}
	
	private boolean updateResultIntoDataBase(String strQuery) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException
	{
		boolean isSuccessful = true;
		try
		{
			Connection connection = getDataSourceOracle();	
			Statement execStmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);	
			execStmt.executeUpdate(strQuery);
			connection.setAutoCommit(true);
			execStmt.close();
			connection.close();
		}
		catch (Exception e) 
		{
			isSuccessful = false;
			System.out.println(e.getLocalizedMessage());
		}
		return isSuccessful;
	}
	
	private String selectResultFromDataBase(String strQuery) throws InstantiationException, IllegalAccessException, ClassNotFoundException, FileNotFoundException, SQLException, IOException
	{
		String queryResult = "";
		try
		{
			Connection connection = getDataSourceOracle();	
			ResultSet execRset = null;
			Statement execStmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);	
			execRset = execStmt.executeQuery(strQuery);
			execRset.first();
			queryResult = execRset.getString(1);
			execStmt.close();
			execRset.close();
			connection.close();
		}
		catch (Exception e) 
		{
			System.out.println(e.getLocalizedMessage());
		}
		return queryResult;
	}
	
}
