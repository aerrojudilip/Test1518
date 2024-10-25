/*******************************************************************************
 * Licensed Materials - Property of IBM
 *
 *
 * OpenPages GRC Platform (PID: 5725-D51)
 *
 * © Copyright IBM Corporation 2022 - CURRENT_YEAR. All Rights Reserved.
 *
 * US Government Users Restricted Rights- Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *
 *******************************************************************************/
package com.ibm.openpages.ext.iam.rest.util;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.logging.Log;
//import org.apache.log4j.Logger;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ibm.cloud.sdk.core.security.IamToken;
import com.ibm.watson.common.SdkCommon;
import com.ibm.watson.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.natural_language_understanding.v1.model.ClassificationsOptions;
import com.ibm.watson.natural_language_understanding.v1.model.Features;


public class ApiUtil5W {

    private static final HttpHeaders HEADERS_IAM_AUTH = new HttpHeaders();
    private static final HttpHeaders HEADERS_ANALYZE_QUERY_APPLICATION_JSON = new HttpHeaders();

    static {
        HEADERS_IAM_AUTH.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HEADERS_IAM_AUTH.setCacheControl(CacheControl.noCache());

    }

    static {
        HEADERS_IAM_AUTH.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HEADERS_IAM_AUTH.setCacheControl(CacheControl.noCache());
        HEADERS_ANALYZE_QUERY_APPLICATION_JSON.setContentType(MediaType.APPLICATION_JSON);
        HEADERS_ANALYZE_QUERY_APPLICATION_JSON.setCacheControl(CacheControl.noCache());
        HEADERS_ANALYZE_QUERY_APPLICATION_JSON.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        Map<String, String> sdkHeaders = SdkCommon.getSdkHeaders("natural-language-understanding", "v1", "analyze");
        for (Map.Entry<String, String> entry : sdkHeaders.entrySet()) {
            HEADERS_ANALYZE_QUERY_APPLICATION_JSON.add(entry.getKey(), entry.getValue());
        }
    }

    private static final String GRANT_TYPE = "grant_type";
    private static final String REQUEST_GRANT_TYPE = "urn:ibm:params:oauth:grant-type:apikey";
    private static final String API_KEY = "apikey";
    private static final String RESPONSE_TYPE = "response_type";
    private static final String CLOUD_IAM = "cloud_iam";

    private String authUrl= "";
    private String apiKey = "";
    private APIUtil apiUtil;
    private String version="";
    private String analyzeUrl="";
    private String nluUrl = "";
    private String modelID = "";
    private Gson gson = new Gson();
    private Log logger;

    private String mapKey = authUrl + "_" + apiKey;
    private static Map<String, Map.Entry<String, Long>> tokenMap = new ConcurrentHashMap<>();

    
    public ApiUtil5W(String authUrl, String nluURL, String modelID, String apiKey, String version) {
    	
        apiUtil = new APIUtil();
        
        this.authUrl = authUrl;
        this.nluUrl = nluURL;
        this.modelID = modelID;
        this.analyzeUrl = nluUrl + "/v1/analyze";
        this.apiKey = apiKey;
        this.version = version;
//        this.logger = logger;
        
        
        this.gson = new Gson();
        this.mapKey = this.authUrl + "_" + apiKey;   //In case the user changes the auth URL
//        this.logger.info("5Ws API Util Initiated");
        
    }

   
    public String connect() throws Exception {

        // Figure out whether to get a new token by using the current time plus 5 minutes to
        // make sure there is enough time to complete the operation; otherwise, create a new token
    	
//    	this.logger.info("5Ws API Util IAM Authentication Called");
    	String token = "";
//        this.logger.info("@Starting Map Operations");
        
        synchronized (tokenMap) {
        	
        	// Preparing request to fetch the bearer token
            tokenMap.remove(mapKey);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
            body.add(GRANT_TYPE, REQUEST_GRANT_TYPE);
            body.add(API_KEY, apiKey);
            body.add(RESPONSE_TYPE, CLOUD_IAM);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, HEADERS_IAM_AUTH);
            try {
            	
                ResponseEntity<String> response = apiUtil.post(authUrl, request, String.class, "Could not authenticate with IBM IAM on Cloud.");
                String iamTokenResponse = response.getBody();
                
                IamToken iam = gson.fromJson(iamTokenResponse, IamToken.class);
//                this.logger.info("IAM Auth Token Response received");
                Map.Entry<String, Long> newTokenInfo = new ImmutablePair<>(iam.getAccessToken(), iam.getExpiration());
                
                tokenMap.put(mapKey, newTokenInfo);
                
            } catch (Exception e) {
//                this.logger.error("Could not get IAM token.", e);
                throw e;
            }
            token = tokenMap.get(mapKey).getKey();
//            this.logger.info("IAM Token received");
            
            return token;  //The key is the token
        }
    }

    public String analyze(String inputVal) throws Exception {
        
//    	this.logger.info("5Ws Analyze method called");
    	
    	// Creating entityOptions, Features and AnalyzeOPtions for NLU analyze call. Refer NLU documentation

    	ClassificationsOptions categories= new ClassificationsOptions.Builder().model(modelID).build();
        Features features = new Features.Builder().classifications(categories).build();
        AnalyzeOptions analyzeOptions = new AnalyzeOptions.Builder().text(inputVal).features(features).build();
        
        HEADERS_ANALYZE_QUERY_APPLICATION_JSON.setBearerAuth(connect()); // Fetching the bearer token and setting it in the headers
        
        UriComponentsBuilder fullUrl = UriComponentsBuilder.fromHttpUrl(analyzeUrl); // Building the URL
        fullUrl.queryParam("version", this.version); // Adding the NLU Version
        
        // Setting the analyze options as features
        final JsonObject contentJson = new JsonObject();
        contentJson.add("features", com.ibm.cloud.sdk.core.util.GsonSingleton.getGson().toJsonTree(analyzeOptions.features()));
        
        if (analyzeOptions.text() != null) {
            contentJson.addProperty("text", analyzeOptions.text());
        }
        if (analyzeOptions.html() != null) {
            contentJson.addProperty("html", analyzeOptions.html());
        }
        if (analyzeOptions.url() != null) {
            contentJson.addProperty("url", analyzeOptions.url());
        }
        if (analyzeOptions.clean() != null) {
            contentJson.addProperty("clean", analyzeOptions.clean());
        }
        if (analyzeOptions.xpath() != null) {
            contentJson.addProperty("xpath", analyzeOptions.xpath());
        }
        if (analyzeOptions.fallbackToRaw() != null) {
            contentJson.addProperty("fallback_to_raw", analyzeOptions.fallbackToRaw());
        }
        if (analyzeOptions.returnAnalyzedText() != null) {
            contentJson.addProperty("return_analyzed_text", analyzeOptions.returnAnalyzedText());
        }
        if (analyzeOptions.language() != null) {
            contentJson.addProperty("language", analyzeOptions.language());
        }
        if (analyzeOptions.limitTextCharacters() != null) {
            contentJson.addProperty("limit_text_characters", analyzeOptions.limitTextCharacters());
        }
//        this.logger.info("Preparing and sending 5Ws Analyze request");
        HttpEntity<String> request = new HttpEntity<>(contentJson.toString(), HEADERS_ANALYZE_QUERY_APPLICATION_JSON);
        try {
            ResponseEntity<String> response = apiUtil.post(fullUrl.toUriString(), request, String.class, "Could not analyze request with NLU.");
            String analysisJson = response.getBody();
            AnalysisResults analysisResults = gson.fromJson(analysisJson, AnalysisResults.class);
//            this.logger.info("Model Results= "+analysisResults);
         
            return analysisResults.toString();
        } catch (Exception e) {
//            this.logger.error("Failed Request: " + contentJson, e);
        	return e.toString();
//            throw e;
        }

    }
    
}
