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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ebay.sdk.call.AddToWatchListCall;
import com.ebay.sdk.call.GetMyeBayBuyingCall;
import com.ebay.sdk.ApiContext;
import com.ebay.sdk.ApiCredential;
import com.ebay.soap.eBLBaseComponents.DetailLevelCodeType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.PaginatedItemArrayType;

/**
 * Servlet implementation class WatchingController
 */
public class WatchingController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private AddToWatchListCall addToWatchListCall;
	private GetMyeBayBuyingCall getMyeBayBuyingCall;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public WatchingController() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void init() {
        // get application id and server address from web.xml
        ServletContext context = getServletContext();
        String eBayToken = context.getInitParameter("EBayToken");
        String tradingServerAddress = context.getInitParameter("TradingServerAddress");
    	
    	// initialize Trading context
    	ApiContext apiContext = new ApiContext();
    	ApiCredential apiCredential = new ApiCredential();
    	apiCredential.seteBayToken(eBayToken);
    	apiContext.setApiCredential(apiCredential);
    	apiContext.setApiServerUrl(tradingServerAddress);
    	
    	// initialize Trading call
    	addToWatchListCall = new AddToWatchListCall(apiContext);
    	getMyeBayBuyingCall = new GetMyeBayBuyingCall(apiContext);
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String itemId = (String)request.getParameter("itemId");
			
			//to be watched item id
			String[] itemIDs = {itemId};
			addToWatchListCall.setItemIDs(itemIDs);
			
			//add to watch
			addToWatchListCall.addToWatchList();
			
			DetailLevelCodeType[] detailLevels = new DetailLevelCodeType[] {
			          DetailLevelCodeType.RETURN_ALL
			};
			getMyeBayBuyingCall.setDetailLevel(detailLevels);
			
			//get my ebay buying
			getMyeBayBuyingCall.getMyeBayBuying();
			
			//handle returned watch list
			PaginatedItemArrayType watchList = getMyeBayBuyingCall.getReturnedWatchList();
			
			if (watchList != null && watchList.getItemArray() != null) {
		    	ItemType[] items = watchList.getItemArray().getItem();
		    	request.setAttribute(Constants.WATCH_LIST, items);
			}
			
		    request.getRequestDispatcher("/watching.jsp").forward(request, response);
			
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doGet(request, response);
	}

}
