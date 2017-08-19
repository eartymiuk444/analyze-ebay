<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ page errorPage="error.jsp" %>
<%@ page import="java.util.List" %>
<%@ page import="com.ebay.sample.Constants"%>
<%@ page import="com.ebay.services.finding.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<title>Find Items</title>
<link href="style/style.css" rel="stylesheet" type="text/css">
<%
List<SearchItem> items = (List<SearchItem>)request.getAttribute(Constants.SEARCH_RESULT);
String keyword = (String)request.getAttribute(Constants.QUERY_KEYWORD);
%>
</head>
<body>
<div>
<form  method="post">
	Keyword:<input type="text" name="keyword" value="<%=keyword %>">
			<input type="submit" name="submit" >
		
</form>
</div>
<br><br>
 
<%if(items != null) { %>
	<table>
		<tr>
		    <th>Item Id</th>
			<th>Gallery</th>
			<th>Title(Click to view item on eBay)</th>
			<th>Current Price</th>
			<th>Time Left</th>
			<th>Add To Watch</th>
		</tr>
		<%
		
		for(SearchItem item : items){
		%>
		<tr>
		    <td><%=item.getItemId() %></td>
			<td><%if (item.getGalleryURL() != null) { %><img src="<%=item.getGalleryURL() %>"/> <% } else {
				%>No Gallery <%} %>
			</td>
			<td><a href="<%=item.getViewItemURL() %>" ><%=item.getTitle() %></a></td>
			<td><%=item.getSellingStatus().getCurrentPrice().getValue() + " USD" %></td>
			<td><%=item.getSellingStatus().getTimeLeft() %></td>
			<td><a href="watching.do?itemId=<%=item.getItemId() %>">Add To Watch</a></td>
			
		</tr>
		<%}%>
	</table>
<%} %>

</body>
</html>