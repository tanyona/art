<%-- 
    Document   : error-405
    Created on : 28-Feb-2014, 16:54:54
    Author     : Timothy Anyona

Error page for 405 errors (method not allowed)
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page trimDirectiveWhitespaces="true" %>

<!DOCTYPE html>
<html>
    <head>
        <meta charset='utf-8'>
        <title>ART - Method Not Allowed</title>
		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/public/js/bootstrap-3.3.6/css/bootstrap.min.css">
		<link rel="shortcut icon" href="${pageContext.request.contextPath}/public/images/favicon.ico">
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/error-405-inline.jsp"/>
    </body>
</html>