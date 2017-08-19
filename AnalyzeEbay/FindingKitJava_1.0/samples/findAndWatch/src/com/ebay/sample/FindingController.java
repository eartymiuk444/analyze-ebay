/*
Copyright (c) 2011 eBay, Inc.

This program is licensed under the terms of the eBay Common Development and 
Distribution License (CDDL) Version 1.0 (the "License") and any subsequent 
version thereof released by eBay.  The then-current version of the License 
can be found at https://www.codebase.ebay.com/Licenses.html and in the 
eBaySDKLicense file that is under the eBay SDK install directory.
*/
package com.ebay.sample;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ebay.services.client.FindingServiceClientFactory;
import com.ebay.services.client.ClientConfig;
import com.ebay.services.finding.*;

/**
 * Servlet implementation class FindingController
 */
public class FindingController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	//finding service client
    private FindingServicePortType serviceClient;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FindingController() {
        super();
    }

    public void init() {
        // get application id and server address from web.xml
        ServletContext context = getServletContext();
        String appId = context.getInitParameter("AppID");
        String findingServerAddress = context.getInitParameter("FindingServerAddress");
    	
        // initialize service end-point configuration
    	ClientConfig clientConfig = new ClientConfig();
    	clientConfig.setEndPointAddress(findingServerAddress);
    	clientConfig.setApplicationId(appId);
    	
    	// initialize finding service client
    	serviceClient = FindingServiceClientFactory.getServiceClient(clientConfig);    	
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String keyword = (String)request.getParameter("keyword");
		if (keyword == null) keyword = "ipad"; // default keyword
		
	    FindItemsAdvancedRequest fiRequest = new FindItemsAdvancedRequest();
	    //set request parameters
	    fiRequest.setKeywords(keyword);
	    PaginationInput pi = new PaginationInput();
	    pi.setEntriesPerPage(10);
	    fiRequest.setPaginationInput(pi);
	    
	    //call service
	    FindItemsAdvancedResponse fiResponse = serviceClient.findItemsAdvanced(fiRequest);
	    
	    //handle response
	    if (fiResponse != null && fiResponse.getSearchResult()!= null) {
	    	List<SearchItem> items = fiResponse.getSearchResult().getItem();
	    	request.setAttribute(Constants.SEARCH_RESULT, items);
	    }
	    request.setAttribute(Constants.QUERY_KEYWORD, keyword);
		
	    request.getRequestDispatcher("/finding.jsp").forward(request, response);
	}

}
