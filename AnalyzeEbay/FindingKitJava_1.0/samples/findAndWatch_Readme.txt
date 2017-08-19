The findAndWatch sample is a composite sample which shows how
FindingKit and eBay Trading SDK can be used together to create applications.

Here are a few notes about this sample:
1. Environment Requirements
This sample is a java web application, it needs a java application server to run,
it has been tested in Apache Tomcat 5.5 with JRE 1.6.

2. Configuration
The sample needs to be configured before it can be run,
you need to fill in following information in the web.xml file
 > eBay developer application id
 > eBay Finding server address
 > eBay Token
 > eBay Trading server address

3. Dependencies
Besides Jsp and Servlet dependencies, the sample also depends on following libraries
 > finding.jar (FindingKit 1.0 library)
 > log4j-1.2.16.jar
 > ebaycalls.jar (eBay Trading SDK e705 library)
 > ebaysdkcore.jar (eBay Trading SDK e705 library)
 > slf4j-api.1.6.1.jar
 > slf4j-log4j12-1.6.1.jar

4. Application Flow
 > Find items by invoking FindItemsByKeywords call of eBay Finding service.
 > Add item to watch list by invoking AddToWatchList API of eBay Trading API.
 > Show Watch List by invoking GetMyeBayBuying API of eBay Trading API.