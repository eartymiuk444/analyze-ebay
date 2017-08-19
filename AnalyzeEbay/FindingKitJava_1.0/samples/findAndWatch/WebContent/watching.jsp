<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%@ page errorPage="error.jsp" %>
<%@ page import="com.ebay.sample.Constants"%>
<%@ page import="com.ebay.soap.eBLBaseComponents.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<link href="style/style.css" rel="stylesheet" type="text/css">
<title>My Watch List</title>
<%
ItemType[] items = (ItemType[])request.getAttribute(Constants.WATCH_LIST);
%>
</head>
<body>
<% if(items != null){ %>
    <h3>Total number of watching items : <%=items.length %></h3> 
	<table>
		<tbody>
			<tr>
				<th>Item ID</th>
				<th>Gallery</th>
				<th>Title(Click to view item on eBay)</th>
				<th>Current Price</th>
			</tr>
		<% 
			for(ItemType item : items){
		%>
			<tr>
				<td><%=item.getItemID() %></td>
				<td><%if(item.getPictureDetails() != null && item.getPictureDetails().getGalleryURL() != null){%>
					<img src="<%=item.getPictureDetails().getGalleryURL() %>"/>
					<%} else { %> No Gallery <%}%> 
				</td>
				<td><a href="<%=item.getListingDetails().getViewItemURL() %>" ><%=item.getTitle() %></a></td>
				<td><%=item.getSellingStatus().getCurrentPrice().getValue() + " USD" %></td>
			</tr>
		<%}%>
		</tbody>
	</table>
<%} %>
</body>
</html>