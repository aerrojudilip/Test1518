/*******************************************************************************
 * Licensed Materials - Property of IBM
 *
 *
 * OpenPages GRC Platform (PID: 5725-D51)
 *
 * � Copyright IBM Corporation 2019 - CURRENT_YEAR. All Rights Reserved.
 *
 * US Government Users Restricted Rights- Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *
 *******************************************************************************/
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
package com.ibm.openpages.ext.iam.rest.util;

import com.ibm.openpages.api.OpenPagesException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.apache.log4j.Logger;


public class APIUtil {
	private static Logger logger = Logger.getLogger("IAMAuthApiUtil.log");

    private int maxRetry = 5;
    private long retryInterval = 60 * 1000;

    private static RestTemplate restTemplate = new RestTemplate();

    /*
    static {
        restTemplate.setInterceptors(Arrays.asList(new PlusEncoderInterceptor())); // thread-safe
    }
*/
    public APIUtil() {
    }

    public APIUtil(int maxRetry, int retryInterval) {
        this.maxRetry = maxRetry;
        this.retryInterval = retryInterval;
    }

    public <T> ResponseEntity<T> post(String url, HttpEntity<?> request, Class<T> responseType, String errMsg) {
        return exchange(url, HttpMethod.POST, request, responseType, errMsg, 0);
    }

    public <T> ResponseEntity<T> get(String url, HttpEntity<?> request, Class<T> responseType, String errMsg) {
        return exchange(url, HttpMethod.GET, request, responseType, errMsg, 0);
    }

    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, HttpEntity<?> request, Class<T> responseType, String errMsg, int retry) {
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, method, request, responseType);
            checkHttpOk(response, errMsg);
            return response;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && retry < maxRetry) {
                logger.error("Too many requests, sleeping 60s and trying %d more time(s)");
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e1) {
                    logger.error("Interrupted while waiting to resend HTTP request, continuing.", e);
                }
                return exchange(url, method, request, responseType, errMsg, retry + 1);
            }
            throw e;
        }
    }

    private void checkHttpOk(ResponseEntity<?> response, String message) throws OpenPagesException {
        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.ACCEPTED)
            return;

        // log error and throw if not ok or accepted
        logger.error(message);
        logger.error("Status code: " + response.getStatusCodeValue());
        logger.error("Body: ");
        logger.error(response.getBody() == null ? "null" : response.getBody().toString());

        throw new OpenPagesException(message);
    }
}
