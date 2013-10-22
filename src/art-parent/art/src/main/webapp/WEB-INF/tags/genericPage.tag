<%-- 
    Document   : genericPage
    Created on : 19-Sep-2013, 16:52:05
    Author     : Timothy Anyona

Template for a basic page
Includes bootstrap css
--%>

<%@tag description="Generic Page Template" pageEncoding="UTF-8"%>

<%-- The list of normal or fragment attributes can be specified here: --%>
<%@attribute name="title" required="true"%>
<%@attribute name="headContent" fragment="true" %>
<%@attribute name="pageJavascript" fragment="true" %>
<%@attribute name="pageCss" fragment="true" %>
<%@attribute name="metaContent" fragment="true" %>
<%@attribute name="pageHeader" fragment="true" %>
<%@attribute name="pageFooter" fragment="true" %>

<%-- any content can be specified here e.g.: --%>
<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">

		<title>${title}</title>

		<jsp:invoke fragment="metaContent"/>

		<jsp:invoke fragment="headContent"/>

		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/bootstrap-3.0.0.min.css">
		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/font-awesome-3.2.1.min.css">
		<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/art-3.css">

		<jsp:invoke fragment="pageCss"/>
	</head>
	<body>

		<div id="wrap">
			<jsp:invoke fragment="pageHeader"/>

			<div id="pageContent">
				<div  class="container">
					<jsp:doBody/>
				</div>
			</div>
			<div id="push"></div>
		</div>

		<jsp:invoke fragment="pageFooter"/>

		<!-- javascript placed at the end of the document so that pages load faster -->
		<jsp:invoke fragment="pageJavascript"/>
	</body>
</html>