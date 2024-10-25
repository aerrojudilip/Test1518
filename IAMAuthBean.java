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
package com.ibm.openpages.ext.iam.rest.bean;

/**
 * Basic POJO Bean that can be serialized to JSON easily in a desired format.
 * 
 * In a real-world case you would probably make something more complex to
 * represent the full GRC Object including associations, custom fields. This
 * approach gives the developer full control over format and representation of
 * the data they want their API to use.
 * 
 */
public class IAMAuthBean {

	
	private String inputData;
	
	public IAMAuthBean() {
	
		inputData = null;
		
	}
	
	public IAMAuthBean(String inputData) {
		super();
		this.inputData = inputData;
	}

	public String getInputData() {
		return inputData;
	}

	public void setInputData(String inputData) {
		this.inputData = inputData;
	}

	
}