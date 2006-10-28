<%--

  Modifications:

  2005 Sep 30: Hacked up to use CSS for layout. -- DJ Gregor

--%>
<%--
  This page is included by other JSPs to create a uniform header. 
  It expects that a <base> tag has been set in the including page
  that directs all URLs to be relative to the servlet context.
  
  This include JSP takes two parameters:
    title (required): used in the middle of the header bar
    location (optional): used to "dull out" the item in the menu bar
      that has a link to the location given  (for example, on the
      outage/index.jsp, give the location "outages")
--%>

<%@page language="java"
	contentType="text/html"
	session="true"
	import="java.text.DateFormat,
	        java.text.SimpleDateFormat,
		java.util.Date,
		org.opennms.netmgt.config.NotifdConfigFactory
		"
%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%!
    static DateFormat dateFormatter = DateFormat.getDateInstance( DateFormat.MEDIUM );
    static SimpleDateFormat timeFormatter = new SimpleDateFormat("H:mm z");
    
    public void init() throws ServletException {
        try {
            NotifdConfigFactory.init();
        } catch (Throwable t) {
	    // notice status will be unknown if the factory can't be initialized
	}
    }
%>

<%
    Date now = new Date(); 
    pageContext.setAttribute("date", dateFormatter.format(now));
    pageContext.setAttribute("time", timeFormatter.format(now));

    String noticeStatus;
    try {
        noticeStatus = NotifdConfigFactory.getInstance().getPrettyStatus();
    } catch (Throwable t) {
        noticeStatus = "<font color=\"ff0000\">Unknown</font>";
    }
    pageContext.setAttribute("noticeStatus", noticeStatus);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html>
<head>
  <title>
    <c:forEach var="headTitle" items="${paramValues.headTitle}">
      <c:out value="${headTitle}" escapeXml="false"/> |
    </c:forEach>
    OpenNMS Web Console
  </title>
  <meta http-equiv="Content-type" content="text/html; charset=utf-8" />
  <c:forEach var="meta" items="${paramValues.meta}">
    <c:out value="${meta}" escapeXml="false"/>
  </c:forEach>
  <base href="<%=org.opennms.web.Util.calculateUrlBase( request )%>" />
  <link rel="stylesheet" type="text/css" href="css/styles.css" media="screen" />
	<link rel="stylesheet" type="text/css" href="css/print.css" media="print" />
  <script type="text/javascript" src="js/global.js"></script>
</head>
<body>

<c:choose>
	<c:when test="${param.quiet == 'true'}">
	<!-- No visual header is being displayed -->
	</c:when>

	<c:otherwise>

	<!-- Header -->
	<div id="header">
		<h1 id="headerlogo"><a href="index.jsp"><img src="images/logo.png" alt="OpenNMS Web Console Home"/></a></h1>  
		<div id="headerinfo">
			<h2><c:out value="${param.title}"/></h2>
			<p align="right">
				<c:choose>
					<c:when test="${!empty pageContext.request.remoteUser}">
						User: <strong><c:out value="${pageContext.request.remoteUser}"/></strong> (Notices <c:out value="${noticeStatus}" escapeXml="false"/>)
						- <a href="j_acegi_logout">Log out</a>
					</c:when>
					<c:otherwise>
						User: &hellip;
					</c:otherwise>
				</c:choose>
				<br />
				<c:out value="${date}"/> &nbsp; <c:out value="${time}"/>
			</p>
		</div>
		<hr />
		<c:choose>
			<c:when test="${param.nonavbar != 'true'}">
				<div id="headernavbarright">
                  <jsp:include page="/navBar.htm" flush="false"/>
				</div>
			</c:when>
			<c:otherwise>
				<div id="headernavbarright">
					<div class="navbar">
						<ul>
							<li class="last"><a href="http://www.opennms.org/index.php/FAQ">FAQs</a></li>
						</ul>
					</div>
				</div>
			</c:otherwise>
		</c:choose>
	  <div class="spacer"><!-- --></div>
	</div>
	</c:otherwise>
</c:choose>

<!-- Body -->
<div id="content">
<c:if test="${param.nonavbar != 'true'}">	
   <h2><a href="index.jsp">Home</a>
   <c:forEach var="breadcrumb" items="${paramValues.breadcrumb}">
     <c:if test="${breadcrumb != ''}">
           / <c:out value="${breadcrumb}" escapeXml="false"/>
     </c:if>
   </c:forEach></h2>
</c:if>