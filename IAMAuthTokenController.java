/*******************************************************************************
 * Licensed Materials - Property of IBM *
 * OpenPages GRC Platform (PID: 5725-D51)
 *
 * (c) Copyright IBM Corporation 2018 - 2020. All Rights Reserved.
 *  
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.openpages.ext.ui.controller;

import com.ibm.openpages.ext.iam.rest.bean.IAMAuthBean;
import com.ibm.openpages.ext.tss.service.IApplicationUtil;
import com.ibm.openpages.ext.tss.service.util.CommonUtil;
import com.ibm.openpages.ext.ui.bean.Employee;
//import com.ibm.openpages.ext.ui.dao.HelperAppBaseDAO;
import com.ibm.openpages.ext.ui.service.IAMAuthTokenService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.dbcp.BasicDataSource;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ibm.openpages.api.service.ServiceFactory;
import com.ibm.openpages.api.service.IServiceFactory;
import com.ibm.openpages.api.service.IQueryService;
import com.ibm.openpages.api.configuration.ICurrency;
import com.ibm.openpages.api.metadata.DataType;
import com.ibm.openpages.api.metadata.IEnumValue;
import com.ibm.openpages.api.metadata.Id;
import com.ibm.openpages.api.query.IQuery;
import com.ibm.openpages.api.query.IResultSetRow;
import com.ibm.openpages.api.query.ITabularResultSet;
import com.ibm.openpages.api.resource.IBooleanField;
import com.ibm.openpages.api.resource.ICurrencyField;
import com.ibm.openpages.api.resource.IDateField;
import com.ibm.openpages.api.resource.IEnumField;
import com.ibm.openpages.api.resource.IField;
import com.ibm.openpages.api.resource.IFloatField;
import com.ibm.openpages.api.resource.IIdField;
import com.ibm.openpages.api.resource.IIntegerField;
import com.ibm.openpages.api.resource.IMultiEnumField;
import com.ibm.openpages.api.resource.IReferenceField;
import com.ibm.openpages.api.resource.IStringField;


@Controller
@RequestMapping({"jspview/IAMAuthToken"})
public class IAMAuthTokenController
{
  @Autowired
  IAMAuthTokenService iamAuthTokenService;
  
//  @Autowired
//  HelperAppBaseDAO helperAppBaseDAO;
  
  @Autowired
  IApplicationUtil applicationUtil;
  
  @Autowired
  DataSource dataAccessConfig;
  
  @PostConstruct
  public void initController()
  {
//    this.logger = this.loggerUtil.getExtLogger("AIML-Custom-FiveWs-Issue-Similarity.log");
    this.dataAccessConfig = new BasicDataSource();
  }
  
  /*
   * 
   This end point can be used to check if the API is working
   * 
   */
  
  
  @RequestMapping(value={"/status"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public String getInitialPageForApp(Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response)
    throws Exception
  {
//    this.logger.info("AI/ML Custom FiveWs Issue Similarity JAR UP & RUNNING");
    

    String message = "Issue Similarity and FiveWs APIs Running";
    
//    this.logger.info(message);
    return message;
  }
  
/*
 * 
 GET request for test purposes
 * 
 */
  
  @RequestMapping(value={"/getClassifierResult"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
  @ResponseBody
  public String getIAMAuthToken(@RequestParam("inputData") String inputData, Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response)
		    throws Exception
  {

//    this.logger.info("getIAMAuthToken() Start");
//    this.logger.info("inputData: " + inputData);
    String result = null;
    
    // Fetching model id, api key, nlu url, iam url from common settings
//    String modelId = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/modelID");
//  	String apiKey = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/apiKey");
//  	String version = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/version");
//  	String nluURL = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/nluURL");
//  	String iamURL = this.applicationUtil.getRegistrySetting("/OpenPages/Common/Security/iam/Authentication URL");
    
    String modelId = "33b7443d-5a1f-4ad9-a0ce-4287c52eb405";
    String apiKey = "WOhH8EI5JBWJUxTUfEK56xDZRp2aeQVX05MEPvjDQd_n";
    String version = "2022-04-07";
    String nluURL = "https://api.us-south.natural-language-understanding.watson.cloud.ibm.com/instances/10a5a584-5015-4e7e-9f9b-34c2786fcea7";
    String iamURL = "";
    inputData = "BANK CARDS ONLY APPROVEDSegmentation_Key:No KeyCritical_Data_Element_Name:Primary AddressData_Source_Name:CITIKYC_CARDS_NA_GCB_DQP_ADDR_ALL_ FULL_.DATMeasurement_Point_Name: 164283|CitiKYCDQP_Rule_Version:5Rule_ID:AML15004_212Rule_Description:If not Null or Blank and RECORD_STATE = ACTIVE then produce error if any of the reference fields (Address_1, Address_2, Address_3 , Address_4), if populated, Â contains more than four consecutive repeatingÂ alpha or numeric Characters (e.g., XXXXX, TTTTT, aaaaaa,22222,999999, etc.), or contains all same consecutive characters, or less than 10 characters populated when the following conditions are met: 1. If ROLE_TYPE = CLIENT. 2. If CLIENT_TYPE = (SMCORP, NONBANK, NONPROFIT, GOVT, EMBASSY, MSB, WHV, BANK, FUND) and MEMBER_TYPE = INDIVIDUAL and ROLE_TYPE = BENEFICIAL_OWNER. 3. If CLIENT_TYPE = (CONSUMER) and SUPPLEMENTAL_CARDHOLDERS = Y and MEMBER_TYPE = INDIVIDUAL and ROLE_TYPE = BENEFICIAL_OWNER";
    
   	// If iam url not specified in common settings, default iam url will be used	
  	if (iamURL == null || iamURL.length() == 0) {
//  		logger.info("Using default IAM URL");
  		iamURL = "https://iam.cloud.ibm.com/identity/token";
  	}
    
	try {
//		 this.logger.info("getIAMAuthToken(): Calling service to get authToken");
//		 result = this.iamAuthTokenService.getNLUResult(inputData, this.logger);
		 result = this.iamAuthTokenService.getNLUResult(inputData, modelId, apiKey, version, nluURL, iamURL);
	} catch (Exception ex) {
		return "Controller Exception: "+ex.toString();
//		System.out.println("EXCEPTION!!!!!!!!!!!!!!! getIAMAuthToken()" + CommonUtil.getStackTrace(ex));
	}
	return result;
  }
//  
//  @RequestMapping(value={"/fetchFiveWNLUResult"}, method={org.springframework.web.bind.annotation.RequestMethod.POST})
//  @ResponseBody
//  public String postIAMAuthToken(@RequestBody IAMAuthBean iamAuthBean, Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response)
//		    throws Exception
//  {
//
//    this.logger.info("Fetch 5Ws API called");
//    String inputData = iamAuthBean.getInputData(); // Get the input control
//    this.logger.info("inputData: " + inputData);
//    String result = null;
//    
//    // Fetching model id, api key, nlu url, iam url from common settings
//    String modelId = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/modelID");
//  	String apiKey = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/apiKey");
//  	String version = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/version");
//  	String nluURL = this.applicationUtil.getRegistrySetting("/OpenPages/Applications/CognitiveControls/nluURL");
//  	String iamURL = this.applicationUtil.getRegistrySetting("/OpenPages/Common/Security/iam/Authentication URL");
//  	
//  	// If iam url not specified in common settings, default iam url will be used	
//  	if (iamURL == null || iamURL.length() == 0) {
//  		logger.info("Using default IAM URL");
//  		iamURL = "https://iam.cloud.ibm.com/identity/token";
//  	}
//    
//	try {	
//		this.logger.info("Calling service to get NLU result");
//		result = this.iamAuthTokenService.getNLUResult(inputData, modelId, apiKey, version, nluURL, iamURL, this.logger);
//		this.logger.info("Final Five Ws Result = "+result);
//		
//	} catch (Exception ex) {
//		
//		this.logger.error("EXCEPTION!!!!!!! Fetch 5Ws" + CommonUtil.getStackTrace(ex));
//		this.logger.error("Five Ws Result ERROR");
//		result = ex.toString();
//	}
//	
//	this.logger.info("Fetch FiveWs End");
//	return result;
//  }
  
//  private String printFieldValue(final IField field) {
//
//      System.out.print(field.getName() + ": ");
//      if (field.isNull()) {
//      	return "Null";
//      } else if (field instanceof IBooleanField) {
//          IBooleanField booleanField = (IBooleanField)field;
//          Boolean value = booleanField.getValue();
//          return value.toString();
//      } else if (field instanceof IIntegerField) {
//          IIntegerField integerField = (IIntegerField)field;
//          Integer value = integerField.getValue();
//          return value.toString();
//      } else if (field instanceof IDateField) {
//          IDateField dateField = (IDateField)field;
//          Date date = dateField.getValue();
//          return date.toString();
//      } else if (field instanceof IStringField ) {
//          IStringField stringField = (IStringField)field;
//          String value = stringField.getValue();
//          return value.toString();
//      } else if (field instanceof IEnumField) {
//          IEnumField eField = (IEnumField)field;
//          return eField.getEnumValue().getName().toString();
//      } else if (field instanceof IReferenceField) {
//          IReferenceField referenceField = (IReferenceField)field;
//          return referenceField.getValue().toString();
//      } else if (field instanceof IFloatField) {
//          IFloatField floatField = (IFloatField)field;
//          Double value = floatField.getValue();
//          return value.toString();
//      } else if (field instanceof IMultiEnumField) {
//          IMultiEnumField enumField = (IMultiEnumField) field;
//          for (IEnumValue enumValue : enumField.getEnumValues()) {
//          	return (enumValue.getName()+",").toString();
//          }
//      } else if (field instanceof ICurrencyField) {
//          ICurrencyField currencyField = (ICurrencyField)field;
//          Double localAmount = currencyField.getLocalAmount();
//          ICurrency localCurrency = currencyField.getLocalCurrency();
//          Double baseAmount = currencyField.getBaseAmount();
//          ICurrency baseCurrency = currencyField.getBaseCurrency();
//          String value = "";
//          if (localAmount != null) {
//              value = localCurrency.getCurrencyCode() + " " + localAmount;
//          }
//          if (baseAmount != null) {
//              value += " (" + baseCurrency.getCurrencyCode() + " " + baseAmount + ")";
//          }
//          return value.toString();
//      } else if (DataType.LARGE_STRING_TYPE.equals(field.getDataType())){
//      	 IStringField stringField = (IStringField)field;
//           String value = stringField.getValue();
//           return value.toString();
//      }else if (field instanceof IIdField){
//      	IIdField idField = (IIdField)field;
//          Id value = idField.getValue();
//          return value.toString();
//      }
//      else{
//          return "Unknown Field type: "+field.toString();
//      }
//      return "No field found";
//  }

//  private List<Employee> processEmployeeSearchResults(ResultSet queryResults) throws Exception {
//
//      logger.info("processEmployeeSearchResults() Start");
//
//      // Method Level Variables.
//      int count = 1;
////      int searchLimit;
//
//      Employee employee;
//      List<Employee> employeeList;
//
//      /* Initialize Variables */
//      employeeList = new ArrayList<Employee>();
////      searchLimit = getIntValue(applicationUtil.getRegistrySetting(MAX_EMPLOYEE_SEARCH_RESULT));
////      logger.info("Search Limit Val from reg: " + searchLimit);
////      searchLimit = searchLimit <= 0 ? 200 : searchLimit;
////      logger.info("Calculated Search Limit Val: " + searchLimit);
//
//      /* Make sure the query results are present */
//      if (queryResults != null) {
//
//          /* Iterate through each item in the query results. */
//          while (queryResults.next()) {
//
//              /* On max count reach quit */
//              if (count > 10) {
//                  break;
//              }
//              employee = new Employee();
//              employee.setSoeId(queryResults.getString(1));
//              employee.setFirstName(queryResults.getString(2));
//              employee.setLastName(queryResults.getString(3));
//              employee.setGocId(queryResults.getString(4));
//              employee.setEmailId(queryResults.getString(5));
//
//              employeeList.add(employee);
//              logger.info("employee : " + employee);
//              count++;
//          }
//      }
//
//      logger.info("processEmployeeSearchResults() End");
//      return employeeList;
//  }
  
  
//  @RequestMapping(value={"/testdbconnection"}, method={org.springframework.web.bind.annotation.RequestMethod.GET})
//  @ResponseBody
//  public String getdbresults(Model model, HttpSession session, HttpServletRequest request, HttpServletResponse response)
//    throws Exception
//  {
//	  
//	    this.logger.info("Test DB Connection Controller");
//	    
//	    PreparedStatement preparedStmt = null;
//	    ResultSet queryResults = null;
//	    List<String> preparedStmtValues = new ArrayList<String>();
//	    List<Employee> employeeList;
//	    int count = 1;
//	    String soeid = "cg43437";
//	    String firstName = "Colin";
//	    String lastName = "Grimes";
//	    String EMPLOYEE_SEARCH_QUERY = "SELECT CGH_SOE_ID, \n"
//	            + " FIRST_NAME,\n"
//	            + " LAST_NAME,\n"
//	            + " CGH_GOC,\n"
//	            + " EMAIL_ADDR\n"
//	            + " FROM TABLE(GDW_EMPLOYEE_PKG.GET_GDW_EMP_SEARCH_FUN(?,?,?))";
//	    
//	    Connection connection = this.helperAppBaseDAO.getConnection();
//	    
//	    this.logger.info("connection : " + connection);
//	    preparedStmt = connection.prepareStatement(EMPLOYEE_SEARCH_QUERY);
//	    preparedStmtValues.add(soeid);
//	    preparedStmtValues.add(firstName);
//	    preparedStmtValues.add(lastName);
//	    
//	    for (String preparedStmtValue : preparedStmtValues) {
//	
//	        preparedStmt.setString(count, preparedStmtValue);
//	        count++;
//	    }
//	
//	    this.logger.info("Prepared Statement Query: "+preparedStmt.toString());
//	    queryResults = preparedStmt.executeQuery();
//	    this.logger.info("queryResults: " + queryResults);
//	    this.logger.info("preparedStmt : " + preparedStmt);
//	    employeeList = processEmployeeSearchResults(queryResults);
//        logger.info("employeeList: " + employeeList);
//        
//	    return connection.toString() + "\n" + preparedStmt.toString() + "\n" + queryResults.toString() + "\n" + employeeList;
//	  
////    this.logger.info(message);
//    
//  }
  
}

