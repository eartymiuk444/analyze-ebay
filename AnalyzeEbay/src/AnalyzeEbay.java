import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.ebay.services.client.ClientConfig;
import com.ebay.services.client.FindingServiceClientFactory;
import com.ebay.services.finding.FindCompletedItemsRequest;
import com.ebay.services.finding.FindCompletedItemsResponse;
import com.ebay.services.finding.FindingServicePortType;
import com.ebay.services.finding.ItemFilter;
import com.ebay.services.finding.ItemFilterType;
import com.ebay.services.finding.PaginationInput;
import com.ebay.services.finding.SearchItem;

/**
 * AnalyzeEbay command line program main class.
 * 
 * Program to be run as jar as: 
 * "analyzeEbay keyword1 keyword2 keyword3 ... --min(optional) min --max(optional) max"
 * 
 * Finds used items that sold on ebay between the min and max price specified. 
 * 
 * Stores results in a file unique to the search query. First searches for existence of this file and
 * will read from and add to it. (keyword1 keyword2 ...--min(min)--max(max))
 * 
 * Constructs report of summary stats of the items that were found in the search in a text file of the 
 * same name. (keyword1 keyword2 ...--min(min)--max(max).txt)
 */
public class AnalyzeEbay 
{
	private static final String APPLICATION_ID = "ErikArty-AnalyzeE-PRD-78e31710d-e9534a1c";
	
	private static final String NO_KEYWORDS_ERROR = "ERROR: NO KEYWORDS";
	private static final String MAX_BEFORE_MIN_ERROR = "ERROR: MAX FOUND BEFORE MIN SPECIFIED";
	private static final String MIN_VALUE_INVALID = "ERROR: INVALID VALUE SPECIFIED FOR MIN";
	private static final String MIN_VALUE_NOT_SPECIFIED = "ERROR: MIN VALUE NOT SPECIFIED";
	private static final String MAX_VALUE_INVALID = "ERROR: INVALID VALUE SPECIFIED FOR MAX";
	private static final String MAX_VALUE_NOT_SPECIFIED = "ERROR: MAX VALUE NOT SPECIFIED";
	private static final String ONLY_MAX_AFTER_MIN = "ERROR: CAN ONLY HAVE THE MAX SPECIFIED AFTER MIN";
	
	private static final String MIN_COMMAND_OPTION = "--min";
	private static final String MAX_COMMAND_OPTION = "--max";
	
	private static final String USED_OPTION = "--u";
	private static final String NEW_OPTION = "--n";
	private static final String BROKEN_OPTION = "--b";
	
	private static final String TXT_EXT = ".txt";
	
	private static final String ITEMS_ALREADY_IN_FILE = "Items already in file: ";
	private static final String REACHED_EOF = "Reached end of file";
	private static final String EXISTING_FILE_NOT_FOUND = "Existing file not found";
	
	private static final String USED_CONDITION = "Used";	//includes used, refurbished, or for parts. Excludes items with new or unspecified conditions
	private static final String NEW_CONDITION = "New";		//excludes used, refurbished, for parts, or unspecified conditions
	private static final String BROKEN_CONDITION = "7000";
	
	/**
		1000
		    New 
		1500
		    New other (see details) 
		1750
		    New with defects 
		2000
		    Manufacturer refurbished 
		2500
		    Seller refurbished 
		3000
		    Used 
		4000
		    Very Good 
		5000
		    Good 
		6000
		    Acceptable 
		7000
		    For parts or not working 
	 */
	
	private static final String PAGE = "Page";
	
	private static final String NUM_DUP = "Number of Duplicates: ";
	private static final String NUM_NEW = "Number of New Items: ";
	
	private static final String AUCTION = "Auction";
	private static final String FIXED = "FixedPrice";
	private static final String STORE = "StoreInventory";
	
	private static final String MISC = "Misc/Unknown Listing Types";
	
	private static final String EARLY = "Early";
	private static final String MORNING = "Morning";
	private static final String AFTERNOON = "Afternoon";
	private static final String EVENING = "Evening";
	
	private static final String SUNDAY = "Sunday";
	private static final String MONDAY = "Monday";
	private static final String TUESDAY = "Tuesday";
	private static final String WEDNESDAY = "Wednesday";
	private static final String THURSDAY = "Thursday";
	private static final String FRIDAY = "Friday";
	private static final String SATURDAY = "Saturday";
	
	private static final String ALL_STATS = "All Stats";
	
	private static final String WEEKLY_AVERAGE = "Weekly Average Over Time";
	
	/**
	 * Entry point for the analyze ebay command line program
	 * @param args - keywords and min and max should be specified on the command line as keyword1 keyword2 ... --ubn --min min --max max
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        
    	/** Process command line arguments **/
        Object[] commandLineResult = processCommandLineArgs(args);
        
        if (commandLineResult[0] != null)
        {
        	System.err.println((String)commandLineResult[0]);
        	System.exit(1);
        }
        
        String keywords = (String)commandLineResult[1];
        boolean minSet = false;
        boolean maxSet = false;
        double min = -1;
        double max = -1;
        String condition = USED_CONDITION;
        
        if (commandLineResult[2] != null)
        {
        	minSet = true;
        	min = (Double)commandLineResult[2];
        }
        if (commandLineResult[3] != null)
        {
        	maxSet = true;
            max = (Double)commandLineResult[3];
        }
        
        int conditionInt = (Integer)commandLineResult[4];
        if (conditionInt == 0)
        {
        	condition = USED_CONDITION;
        }
        else if (conditionInt == 1)
        {
        	condition = NEW_CONDITION;
        }
        else if (conditionInt == 2)
        {
        	condition = BROKEN_CONDITION;
        }
        System.out.println(condition);
        
        /** End process command line arguments **/
        
        /** Read in any existing items **/
        HashMap<String, SearchItem> allItemsMap = new HashMap<String, SearchItem>();

        String fileName = keywords + " " + condition + " " + MIN_COMMAND_OPTION + Double.toString(min) + " " + MAX_COMMAND_OPTION + Double.toString(max);
        fileName = fileName.replace("/", "");
        
        File file = new File(fileName);
        PrintWriter output = new PrintWriter(fileName + TXT_EXT);
        PrintWriter itemOutput = new PrintWriter(fileName + "_items" + TXT_EXT);
        
    	//Read in all existing items from the file (if any)
        if (file.exists())
        {
        	FileInputStream fileInputStream = new FileInputStream(file);
        	ObjectInputStream objInputStream = new ObjectInputStream(fileInputStream);
        	
        	try
        	{
        		while(true)
            	{
    	        	SearchItem item = (SearchItem) objInputStream.readObject();
    	        	allItemsMap.put(item.getItemId(), item);
            	}
        	}
        	catch (EOFException e)
        	{
        		System.out.println(ITEMS_ALREADY_IN_FILE + allItemsMap.size());
        		output.println(ITEMS_ALREADY_IN_FILE + allItemsMap.size());
        		output.println(REACHED_EOF);
        		fileInputStream.close();
        		objInputStream.close();
        		
        	}
        }
        else
        {
        	System.out.println(EXISTING_FILE_NOT_FOUND);
        	output.println(EXISTING_FILE_NOT_FOUND);
        }
        /** End read in any existing items **/
        
        /** Make an API call **/
        try {
            // initialize service end-point configuration
            ClientConfig config = new ClientConfig(); config.setApplicationId(APPLICATION_ID);
            //create a service client
            FindingServicePortType serviceClient = FindingServiceClientFactory.getServiceClient(config);
            //create request object
            FindCompletedItemsRequest request = new FindCompletedItemsRequest();
            
            //set request parameters
            System.out.println(keywords);
            request.setKeywords(keywords);

            ItemFilter soldItemFilter = new ItemFilter();
            soldItemFilter.setName(ItemFilterType.SOLD_ITEMS_ONLY);
            soldItemFilter.getValue().add(Boolean.toString(true));
            request.getItemFilter().add(soldItemFilter);
            
            ItemFilter conditionItemFilter = new ItemFilter();
            conditionItemFilter.setName(ItemFilterType.CONDITION);
            conditionItemFilter.getValue().add(condition);
            request.getItemFilter().add(conditionItemFilter);
                        
            if (minSet)
            {
            	ItemFilter minItemFilter = new ItemFilter();
                minItemFilter.setName(ItemFilterType.MIN_PRICE);
                minItemFilter.getValue().add(Double.toString(min));
                request.getItemFilter().add(minItemFilter);
            }
            
            if (maxSet)
            {
            	 ItemFilter maxItemFilter = new ItemFilter();
                 maxItemFilter.setName(ItemFilterType.MAX_PRICE);
                 maxItemFilter.getValue().add(Double.toString(max));
                 request.getItemFilter().add(maxItemFilter);
            }
    
            //call service 
            int currPageNum = 1;
            int itemsPerPage = 100;
            
            int numDup = 0;
            int numNewItems = 0;
            
            PaginationInput pi = new PaginationInput();
            pi.setPageNumber(currPageNum);
            pi.setEntriesPerPage(itemsPerPage);
            request.setPaginationInput(pi);
            
            FindCompletedItemsResponse result = serviceClient.findCompletedItems(request);
            
            while (result.getSearchResult() != null && result.getSearchResult().getItem() != null && !result.getSearchResult().getItem().isEmpty()
            		&& currPageNum <= result.getPaginationOutput().getTotalPages())
            {
            	System.out.println(PAGE + currPageNum);
            	List<SearchItem> items = result.getSearchResult().getItem();
            	
	        	for (SearchItem item : items)
	        	{
	        		if (allItemsMap.containsKey(item.getItemId()))
	        		{
	        			numDup++;
	        		}
	        		else
	        		{
	        			numNewItems++;
	        			allItemsMap.put(item.getItemId(), item);	        			
	        		}
	        	}
	        	
	        	currPageNum++;
	        	pi = new PaginationInput();
	            pi.setPageNumber(currPageNum);
	            pi.setEntriesPerPage(itemsPerPage);
	            request.setPaginationInput(pi);
	            
	        	result = serviceClient.findCompletedItems(request);
            }
            output.println(NUM_DUP + numDup);
            output.println(NUM_NEW + numNewItems);
            System.out.println(NUM_DUP + numDup);
            System.out.println(NUM_NEW + numNewItems);
        } 
        catch (Exception ex) {
            // handle exception if any 
            ex.printStackTrace();
        }
        /** End make an API call **/
        
        /** Print reports **/
        printFullReport(allItemsMap, output);
        
        /** Save back to our file **/
        FileOutputStream fileOutputStream = new FileOutputStream(file);
    	ObjectOutputStream objOutputStream = new ObjectOutputStream(fileOutputStream);
        
        for (SearchItem item : allItemsMap.values())
        {        	
			printItemDetails(itemOutput, item);
        	objOutputStream.writeObject(item);
        }
        fileOutputStream.close();
        objOutputStream.close();
        itemOutput.close();
        output.close();
    }
    
    /**
     * Private helper method that prints out details of a SearchItem to the passed print stream.
     * @param out - the print writer to print to
     * @param item - the item to print the details of
     */
    private static void printItemDetails(PrintWriter out, SearchItem item)
    {
    	out.println(item.getTitle());
        out.println(item.getSellingStatus().getConvertedCurrentPrice().getValue());
        out.println(item.getListingInfo().getEndTime().getTime());
    	out.println(item.getListingInfo().getListingType());
        out.println(item.getSellingStatus().getSellingState());
        out.println(item.getCondition().getConditionDisplayName());
        out.println();
    }
    
    /**
     * Private helper that prints a report (summary stats) of all the SearchItems in the passed allItemMap to the
     * given PrintWriter.
     * @param allItemsMap - a mapping of itemId to SearchItem to print the some details on
     * @param output - the printwriter to print the details of the SearchItems to
     * @throws FileNotFoundException
     */
    private static void printFullReport(HashMap<String, SearchItem> allItemsMap, PrintWriter output) throws FileNotFoundException
    {
    	/** Put together summary stats **/
        DescriptiveStatistics stat = new DescriptiveStatistics();
        
        DescriptiveStatistics sundayStats = new DescriptiveStatistics();
        DescriptiveStatistics mondayStats = new DescriptiveStatistics();
        DescriptiveStatistics tuesdayStats = new DescriptiveStatistics();
        DescriptiveStatistics wednesdayStats = new DescriptiveStatistics();
        DescriptiveStatistics thursdayStats = new DescriptiveStatistics();
        DescriptiveStatistics fridayStats = new DescriptiveStatistics();
        DescriptiveStatistics saturdayStats = new DescriptiveStatistics();
        
        DescriptiveStatistics earlyStats = new DescriptiveStatistics();
        DescriptiveStatistics morningStats = new DescriptiveStatistics();
        DescriptiveStatistics afternoonStats = new DescriptiveStatistics();
        DescriptiveStatistics eveningStats = new DescriptiveStatistics();
        
        DescriptiveStatistics auctionStats = new DescriptiveStatistics();
        DescriptiveStatistics fixedPriceStats = new DescriptiveStatistics();
        DescriptiveStatistics storeStats = new DescriptiveStatistics();
        DescriptiveStatistics miscListingStats = new DescriptiveStatistics();
        
        HashMap<Date, DescriptiveStatistics> weeklyStats = new HashMap<Date, DescriptiveStatistics>();
        Calendar weekCal = Calendar.getInstance();
		weekCal.add(Calendar.WEEK_OF_YEAR, -1);
		weeklyStats.put(weekCal.getTime(), new DescriptiveStatistics());
		
		
		List<SearchItem> allItemsList = new ArrayList<SearchItem>();
		allItemsList.addAll(allItemsMap.values());
		allItemsList.sort(new Comparator<SearchItem>() {
			@Override
			public int compare(SearchItem o1, SearchItem o2) 
			{
				return o2.getListingInfo().getEndTime().getTime().compareTo(o1.getListingInfo().getEndTime().getTime());
			}
		});

        for (SearchItem item : allItemsList)
        {        	
        	
        	double sellValue = item.getSellingStatus().getConvertedCurrentPrice().getValue();
    		stat.addValue(sellValue);
    		
    		Calendar endCal = item.getListingInfo().getEndTime();
    		
    		if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
    		{
    			sundayStats.addValue(sellValue);
    		}
    		else if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
    		{
    			mondayStats.addValue(sellValue);
    		}
    		else if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY)
    		{
    			tuesdayStats.addValue(sellValue);
    		}
    		else if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
    		{
    			wednesdayStats.addValue(sellValue);
    		}
    		else if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY)
    		{
    			thursdayStats.addValue(sellValue);
    		}
    		else if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
    		{
    			fridayStats.addValue(sellValue);
    		}
    		else if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
    		{
    			saturdayStats.addValue(sellValue);
    		}
    		
    		int hourOfDay = endCal.get(Calendar.HOUR_OF_DAY);
    		if (hourOfDay >= 0 && hourOfDay < 6)
    		{
    			earlyStats.addValue(sellValue);
    		}
    		if (hourOfDay >= 6 && hourOfDay < 12)
    		{
    			morningStats.addValue(sellValue);
    		}
    		if (hourOfDay >= 12 && hourOfDay < 18)
    		{
    			afternoonStats.addValue(sellValue);
    		}
    		if (hourOfDay >= 18 && hourOfDay < 24)
    		{
    			eveningStats.addValue(sellValue);
    		}
    		
    		String listingType = item.getListingInfo().getListingType();
    		if (listingType.equals(AUCTION))
    		{
    			auctionStats.addValue(sellValue);
    		}
    		else if (listingType.equals(FIXED))
    		{
    			fixedPriceStats.addValue(sellValue);
    		}
    		else if (listingType.equals(STORE))
    		{
    			storeStats.addValue(sellValue);
    		}
    		else
    		{
    			miscListingStats.addValue(sellValue);
    		}
    		
    		while (endCal.before(weekCal))
    		{
    			weekCal.add(Calendar.WEEK_OF_YEAR, -1);
    			weeklyStats.put(weekCal.getTime(), new DescriptiveStatistics());
    		}
    		
    		weeklyStats.get(weekCal.getTime()).addValue(sellValue);
        }
        /** End put together summary stats **/
        
        /** Print summary stats **/
        output.println();
        output.println();
        output.println("-------------------------------------");
        
        printStats(ALL_STATS, stat, output);
        
        printStats(AUCTION, auctionStats, output);
        printStats(FIXED, fixedPriceStats, output);
        printStats(STORE, storeStats, output);
        
        printStats(SUNDAY, sundayStats, output);
        printStats(MONDAY, mondayStats, output);
        printStats(TUESDAY, tuesdayStats, output);
        printStats(WEDNESDAY, wednesdayStats, output);
        printStats(THURSDAY, thursdayStats, output);
        printStats(FRIDAY, fridayStats, output);
        printStats(SATURDAY, saturdayStats, output);
        
        printStats(EARLY, earlyStats, output);
        printStats(MORNING, morningStats, output);
        printStats(AFTERNOON, afternoonStats, output);
        printStats(EVENING, eveningStats, output);
        
        printStats(MISC, miscListingStats, output);
        
        Set<Date> dates = weeklyStats.keySet();
        ArrayList<Date> dateList = new ArrayList<Date>(dates);
        Collections.sort(dateList);
        
        for (Date dateKey : dateList)
        {
        	printStats(dateKey.toString(), weeklyStats.get(dateKey), output);
        }
        
        output.println(WEEKLY_AVERAGE);
        for (int i = 0; i < dateList.size(); i++)
        {
        	Date dateKey = dateList.get(i);
        	if (i == dateList.size() - 1)
        	{
            	output.print(weeklyStats.get(dateKey).getMean());
        	}
        	else
        	{
            	output.print(weeklyStats.get(dateKey).getMean());
            	output.print("->");
        	}
        }
        
        output.println();
        /** End print summary stats **/
    }
    
    /**
     * Private helper method that prints out the descriptive stats for a collection of values
     * identified by identifier.
     * @param identifier - an identifier for the collection of values in the descriptive stats objects
     * @param stats - the descriptive stats values to print as a string
     */
    private static void printStats(String identifier, DescriptiveStatistics stats, PrintWriter output)
    {
    	double q1 = stats.getPercentile(25);
        double q2 = stats.getPercentile(75);
        
        output.println(identifier);
        output.println("MEAN: " + stats.getMean());
        output.println("Q1: " + q1);
        output.println("Q2: " + q2);
        output.println("MIN: " + stats.getMin());
        output.println("MAX: " + stats.getMax());
        output.println("REASONABLE PROFIT MARGIN (EXCLUDES SHIPPING & HANDLING COSTS): " + (0.87*q2-q1));
        output.println("NUM ITEMS: " + stats.getN());
        output.println("------------------------------");
    }
    
    /**
     * Private helper method to process the command line arguments of the analyzeEbay program
     * @param args - command line arguments passed to the program
     * @return an object array with the results of processing the command line arguments
     * 			arr[0]: String - error message if one exists; null otherwise
     * 			arr[1]: String - concatenated keywords
     * 			arr[2]: Double- min value if set null otherwise
     * 			arr[3]: Double - max value if set null otherwise
     * 			arr[4]: Integer - 0 if used condition (default), 1 if new condition, 2 if broken
     */
    private static Object[] processCommandLineArgs(String[] args)
    {
    	Object[] objArr = new Object[5];
    	objArr[0] = null;
    	objArr[1] = null;
    	objArr[2] = null;
    	objArr[3] = null;
    	objArr[4] = 0;
    	
    	if (args.length < 1 || args[0].equals(MIN_COMMAND_OPTION) || args[0].equals(MAX_COMMAND_OPTION)
    			|| args[0].equals(USED_OPTION) || args[0].equals(NEW_OPTION) || args[0].equals(BROKEN_OPTION))
    	{
    		objArr[0] = NO_KEYWORDS_ERROR;
    		return objArr;
    	}
    	
    	String keywords = args[0];

    	//Concatenate the keywords and check for condition option and the --min option
        for (int i = 1; i < args.length; i++)
        {
        	objArr[1] = keywords;
        	
        	//Check for the condition option (the last one specified is the one we use)
        	if (args[i].equals(USED_OPTION) || 
        			args[i].equals(NEW_OPTION) || 
        			args[i].equals(BROKEN_OPTION))
        	{
        		if (args[i].equals(USED_OPTION))
        		{
        			objArr[4] = 0;
        		}
        		else if (args[i].equals(NEW_OPTION))
        		{
        			objArr[4] = 1;
        		}
        		else if (args[i].equals(BROKEN_OPTION))
        		{
        			objArr[4] = 2;
        		}
        		
        		break;
        	}
        	
        	
        	//Check for the --min option
        	
        	// If --min specified at i than
        	// should have:
        	// i (i+1 length): --min
        	// i+1 (i+2 length): <min_value>
        	// i+2 (i+3 length): --max (optional)
        	// i+3 (i+4 length): <max_value>
        	if (args[i].equals(MIN_COMMAND_OPTION))
        	{
        		int minOptionIndex = i;
        		int minValueIndex = i+1;
        		int maxOptionIndex = i+2;
        		int maxValueIndex = i+3;
        		
        		int lastValidIndex = args.length-1;
        		
        		if (lastValidIndex < minValueIndex)
        		{
        			objArr[0] = MIN_VALUE_NOT_SPECIFIED;
            		return objArr;
        		}
        		
        		try
    			{
    				objArr[2] = Double.parseDouble(args[minValueIndex]);
    			}
    			catch (NumberFormatException e)
    			{
    				objArr[0] = MIN_VALUE_INVALID;
    	    		return objArr;
    			}
        		
        		//Check for the --max option
        		if (lastValidIndex >= maxOptionIndex)
        		{
        			if (!args[maxOptionIndex].equals(MAX_COMMAND_OPTION) || lastValidIndex > maxValueIndex)
        			{
        				objArr[0] = ONLY_MAX_AFTER_MIN;
        	    		return objArr;
        			}
        			
        			if (lastValidIndex < maxValueIndex)
        			{
        				objArr[0] = MAX_VALUE_NOT_SPECIFIED;
        	    		return objArr;
        			}
        			
    				try
    				{
    					objArr[3] = Double.parseDouble(args[maxValueIndex]);
    				}
    				catch (NumberFormatException e)
    				{
    					objArr[0] = MAX_VALUE_INVALID;
        	    		return objArr;
    				}
        		}
        		
        		//return as there should not be any more keywords after --min is specified
        		return objArr;
        		
        	}
        	
        	//Check for the --max option before the --min option, which is an error
        	else if (args[i].equals(MAX_COMMAND_OPTION))
        	{
				objArr[0] = MAX_BEFORE_MIN_ERROR;
	    		return objArr;
        	}
        	
            keywords = keywords + " " + args[i];
        }
        
        objArr[1] = keywords;
    	return objArr;
    }
}