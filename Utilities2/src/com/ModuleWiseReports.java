package com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRXhtmlExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

class ModuleWiseReports {
	private Connection conn;
	private String testExecutionOn;
	
	public static void main(String[] args) throws Exception
	{
		ModuleWiseReports  mwr = new ModuleWiseReports();
		mwr.generateReports();
	}
	
	public void generateReports() throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
	{
		testExecutionOn = getSettingsFromConfig("TestExecutionOn");
		String execidStr = getSettingsFromConfig("ExecIdStr");
		String folderStr = getSettingsFromConfig("FolderStr");
		String[] execidStrAry = execidStr.split(",");
		String[] folderStrAry = folderStr.split(",");
		for(int i = 0; i < execidStrAry.length; i++)
		{
			int exec_id = Integer.parseInt(execidStrAry[i].trim());
			File directory = new File (".");
			String directoryPath = directory.getCanonicalPath();
			String strLocExec_Modules = folderStrAry[i].trim();
			String reportFolderPath = directoryPath + "\\reports\\" + strLocExec_Modules;
			directory = new File(reportFolderPath);
			if(!directory.exists()) 
				directory.mkdirs();	
			String strDetailReportFileName, strSummaryReportFileName, strDetailImportsReportFileName, strSummaryImportsReportFileName;
			if (testExecutionOn.toUpperCase().startsWith("MARS7I"))
			{
				strDetailReportFileName = getSettingsFromConfig("DetailReportFileName1");
				strSummaryReportFileName = getSettingsFromConfig("SummaryReportFileName1");
				strDetailImportsReportFileName=getSettingsFromConfig("DetailImportsReportFileName1");
				strSummaryImportsReportFileName=getSettingsFromConfig("SummaryImportsReportFileName1");
			}
			else
			{
				strDetailReportFileName = getSettingsFromConfig("DetailReportFileName2");
				strSummaryReportFileName = getSettingsFromConfig("SummaryReportFileName2");
				strDetailImportsReportFileName=getSettingsFromConfig("DetailImportsReportFileName2");
				strSummaryImportsReportFileName=getSettingsFromConfig("SummaryImportsReportFileName2");
			}
			
			if(strLocExec_Modules.toUpperCase().startsWith("Imports".toUpperCase()))
			{
				strDetailReportFileName = strDetailImportsReportFileName;
				strSummaryReportFileName = strSummaryImportsReportFileName;
			}
			jasperReportsGeneration(strDetailReportFileName, exec_id, reportFolderPath);
			jasperReportsGeneration(strSummaryReportFileName, exec_id, reportFolderPath);
			try
			{
				String imagesPath = getSettingsFromConfig("ImagesFolder") + "\\images_" + exec_id;
				File srcDir = new File(imagesPath);
				File destDir = new File (reportFolderPath);
				FileUtils.copyDirectoryToDirectory(srcDir, destDir);
			}
			catch (Exception e) 
			{
				System.out.println("Error in copying files...");
			}
		}
	}

	private void jasperReportsGeneration(String reportFile, int Exec_Id, String reportFolderPath) 
	{
		try
		{
			JasperPrint jasperPrint;
			JasperDesign jasperDesign = JRXmlLoader.load(reportFile);
			JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
//			if (IsExcelConnection)
//				jasperPrint = JasperFillManager.fillReport(jasperReport, null, conn);
//			else
			{
				Connection connection = getDataSourceOracle();	
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("EXECUTION_ID", new Integer(Exec_Id));
				
			   	//jasperPrint = JasperFillManager.fillReport(jasperReport, params, conn);	
				jasperPrint = JasperFillManager.fillReport(jasperReport, params, connection); 
			}			
			reportFile = reportFile.split("/")[2];
			reportFile = reportFile.replace(".jrxml", "");
			exportReportToXHtmlFile(jasperPrint, reportFolderPath + "\\" + reportFile + "_" + Exec_Id + ".html");
			
			//JasperExportManager.exportReportToHtmlFile(jasperPrint, reportFileTmp[0] + "_" + Exec_Id + ".html");
		}
		catch(Exception e) 
		{
			String connectMsg = "Could not create the report " + e.getMessage() + "" + e.getLocalizedMessage();
			System.out.println(connectMsg);
		}
	}
	
	private void exportReportToXHtmlFile(JasperPrint jasperPrint, String outputFile)
	{
		JRXhtmlExporter exporter = new JRXhtmlExporter();
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, outputFile);
		exporter.setParameter(JRExporterParameter.CHARACTER_ENCODING, "UTF-8");
		try
		{
			exporter.exportReport();
		}
		catch (JRException ex)
		{
			System.out.println("JR Exception :" + ex.getMessage());	
		}
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
	
	private String getSettingsFromConfig(String strKey) throws FileNotFoundException, IOException 
	{
		Properties prop = new Properties();
		prop.load(new FileInputStream("Utilities.properties"));
		String strData = prop.getProperty(strKey);
		strData = strData.trim();
		return strData;
	}
}
