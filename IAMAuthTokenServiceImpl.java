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
package com.ibm.openpages.ext.ui.service.impl;

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


import com.ibm.openpages.ext.iam.rest.util.ApiUtil5W;
import com.ibm.openpages.ext.tss.helpers.service.IHelperService;
import com.ibm.openpages.ext.tss.service.IApplicationUtil;
import com.ibm.openpages.ext.tss.service.IFieldUtil;
import com.ibm.openpages.ext.tss.service.IGRCObjectUtil;
import com.ibm.openpages.ext.tss.service.ILoggerUtil;
import com.ibm.openpages.ext.tss.service.impl.ApplicationUtil;
import com.ibm.openpages.ext.tss.service.impl.GRCObjectSearchUtil;
import com.ibm.openpages.ext.tss.service.proxy.IServiceFactoryProxy;
import com.ibm.openpages.ext.tss.service.util.CommonUtil;
import com.ibm.openpages.ext.ui.service.IAMAuthTokenService;
import com.ibm.openpages.ext.ui.util.DSMTLinkHelperUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("iAMAuthTokenServiceImpl")
public class IAMAuthTokenServiceImpl
  implements IAMAuthTokenService
{
//  private Log serviceLogger;
//  @Autowired
//  ILoggerUtil loggerUtil;
  @Autowired
  IHelperService helperService;
  @Autowired
  IHelperService commonHelperService;
  @Autowired
  IApplicationUtil applicationUtil;
  @Autowired
  IGRCObjectUtil grcObjectUtil;
  @Autowired
  GRCObjectSearchUtil grcObjectSearchUtil;
  @Autowired
  IFieldUtil fieldUtil;
  @Autowired
  IServiceFactoryProxy serviceFactoryProxy;
  
  @PostConstruct
  public void initServiceImpl()
  {
//    this.serviceLogger = this.loggerUtil.getExtLogger("Issue Similarity Batch Job Logs.log");
  }
//  
   
  public String getNLUResult(String input, String modelId, String apiKey, String version, String nluURL, String iamURL) throws Exception {
  	// TODO Auto-generated method stub
//  	logger.info("Inside SERVICE: getNLUResult() Implementation");
  	
  	String result = "";
//  	JSONObject jsonResponse = new JSONObject();
  	
  	ApiUtil5W authUtil = new ApiUtil5W(iamURL, nluURL, modelId, apiKey, version);
  	
  	
  	// If the length of control description is greater than 2000 characters then send multiple requests to the model (Max text size for model is 2000 chars)
  	
  		// If length of description is less than or equal to 2000 characters
	String response = authUtil.analyze(input);
//	jsonResponse = new JSONObject(response);
	
  	return response.toString();
  }  
}