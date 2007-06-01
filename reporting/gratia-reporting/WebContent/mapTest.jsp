<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="net.sf.gratia.reporting.ReportingConfiguration"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<LINK href="stylesheet.css" type="text/css" rel="stylesheet">
<title>Insert title here</title>
</head>
<body> 
	<jsp:include page="common.jsp" />
	<%
	ReportingConfiguration reportingConfiguration = (ReportingConfiguration)session.getAttribute("reportingConfiguration");
	 %>
	<!-- BEGIN MAP DISPLAY -->
    <table height=100% width=100%>
        	<tr height=300>
          		<td class="normal" align="center">
            		<img src= "images/USA.png" usemap="#areas" border="0">
          			<map name="areas">          
						<area href='mapTest.jsp?id=1' shape='circ' coords='443,147,3'>
						<area href='mapTest.jsp?id=3' shape='circ' coords='439,149,3'>
						<area href='mapTest.jsp?id=51' shape='circ' coords='443,152,3'>
						<area href='mapTest.jsp?id=2' shape='circ' coords='0,0,3'>
						<area href='mapTest.jsp?id=CALTECH' shape='circ' coords='139,215,3'>
						<area href='mapTest.jsp?id=64' shape='circ' coords='138,220,3'>
						<area href='mapTest.jsp?id=NERSC' shape='circ' coords='108,162,3'>
						<area href='mapTest.jsp?id=58' shape='circ' coords='107,167,3'>
						<area href='mapTest.jsp?id=BUFFALO' shape='circ' coords='509,100,3'>
						<area href='mapTest.jsp?id=12' shape='circ' coords='505,102,3'>
						<area href='mapTest.jsp?id=27' shape='circ' coords='509,105,3'>
						<area href='mapTest.jsp?id=8' shape='circ' coords='429,134,3'>
						<area href='mapTest.jsp?id=FNAL' shape='circ' coords='428,132,3'>
						<area href='mapTest.jsp?id=13' shape='circ' coords='427,132,3'>
						<area href='mapTest.jsp?id=21' shape='circ' coords='425,132,3'>
						<area href='mapTest.jsp?id=35' shape='circ' coords='424,134,3'>
						<area href='mapTest.jsp?id=47' shape='circ' coords='424,135,3'>
						<area href='mapTest.jsp?id=62' shape='circ' coords='425,137,3'>
						<area href='mapTest.jsp?id=61' shape='circ' coords='426,137,3'>
						<area href='mapTest.jsp?id=63' shape='circ' coords='428,137,3'>
						<area href='mapTest.jsp?id=67' shape='circ' coords='429,135,3'>
						<area href='mapTest.jsp?id=UIOWA' shape='circ' coords='394,140,3'>
						<area href='mapTest.jsp?id=11' shape='circ' coords='145,231,3'>
						<area href='mapTest.jsp?id=SDSC' shape='circ' coords='144,236,3'>
						<area href='mapTest.jsp?id=IU' shape='circ' coords='447,162,3'>
						<area href='mapTest.jsp?id=59' shape='circ' coords='446,167,3'>
						<area href='mapTest.jsp?id=PSU' shape='circ' coords='519,135,3'>
						<area href='mapTest.jsp?id=16' shape='circ' coords='570,113,3'>
						<area href='mapTest.jsp?id=17' shape='circ' coords='565,113,3'>
						<area href='mapTest.jsp?id=52' shape='circ' coords='565,118,3'>
						<area href='mapTest.jsp?id=57' shape='circ' coords='570,118,3'>
						<area href='mapTest.jsp?id=UWM' shape='circ' coords='429,118,3'>
						<area href='mapTest.jsp?id=OU' shape='circ' coords='346,219,3'>
						<area href='mapTest.jsp?id=40' shape='circ' coords='345,224,3'>
						<area href='mapTest.jsp?id=SLAC' shape='circ' coords='108,175,3'>
						<area href='mapTest.jsp?id=22' shape='circ' coords='0,-2,3'>
						<area href='mapTest.jsp?id=45' shape='circ' coords='0,2,3'>
						<area href='mapTest.jsp?id=23' shape='circ' coords='351,251,3'>
						<area href='mapTest.jsp?id=65' shape='circ' coords='577,91,3'>
						<area href='mapTest.jsp?id=TTU' shape='circ' coords='302,242,3'>
						<area href='mapTest.jsp?id=ALBANY' shape='circ' coords='552,99,3'>
						<area href='mapTest.jsp?id=UNM' shape='circ' coords='254,221,3'>
						<area href='mapTest.jsp?id=BINGHAM' shape='circ' coords='536,112,3'>
						<area href='mapTest.jsp?id=CHICAGO' shape='circ' coords='436,139,3'>
						<area href='mapTest.jsp?id=43' shape='circ' coords='435,144,3'>
						<area href='mapTest.jsp?id=UFL' shape='circ' coords='507,270,3'>
						<area href='mapTest.jsp?id=60' shape='circ' coords='506,275,3'>
						<area href='mapTest.jsp?id=42' shape='circ' coords='351,150,3'>
						<area href='mapTest.jsp?id=UNL' shape='circ' coords='350,155,3'>
						<area href='mapTest.jsp?id=WISC' shape='circ' coords='416,117,3'>
						<area href='mapTest.jsp?id=48' shape='circ' coords='415,122,3'>
						<area href='mapTest.jsp?id=VANDERBILT' shape='circ' coords='452,202,3'>
						<area href='mapTest.jsp?id=HAMPTONU' shape='circ' coords='548,172,3'>
						<area href='mapTest.jsp?id=UTEXAS' shape='circ' coords='345,282,3'>
						<area href='mapTest.jsp?id=46' shape='circ' coords='0,0,3'>
						<area href='mapTest.jsp?id=50' shape='circ' coords='0,0,3'>
						<area href='mapTest.jsp?id=FIU' shape='circ' coords='538,316,3'>
						<area href='mapTest.jsp?id=WAYNE' shape='circ' coords='473,122,3'>
						<area href='mapTest.jsp?id=55' shape='circ' coords='0,0,3'>
						<area href='mapTest.jsp?id=DARTMOUTH' shape='circ' coords='561,81,3'>
						<area href='mapTest.jsp?id=MIT' shape='circ' coords='578,99,3'>
						<area href='mapTest.jsp?id=RICE' shape='circ' coords='373,289,3'>
						<area href='mapTest.jsp?id=1' shape='circ' coords='442,150,6' style='visibility:hidden' onmouseover='Show(1)' onmouseout='Hide(1)'>
						<area href='mapTest.jsp?id=3' shape='circ' coords='442,150,6' style='visibility:hidden' onmouseover='Show(1)' onmouseout='Hide(1)'>
						<area href='mapTest.jsp?id=PURDUE' shape='circ' coords='442,150,6' style='visibility:hidden' onmouseover='Show(1)' onmouseout='Hide(1)'>
						<area href='mapTest.jsp?id=2' shape='circ' coords='0,0,6' style='visibility:hidden' onmouseover='Show(2)' onmouseout='Hide(2)'>
						<area href='mapTest.jsp?id=4' shape='circ' coords='139,218,6' style='visibility:hidden' onmouseover='Show(3)' onmouseout='Hide(3)'>
						<area href='mapTest.jsp?id=64' shape='circ' coords='139,218,6' style='visibility:hidden' onmouseover='Show(3)' onmouseout='Hide(3)'>
						<area href='mapTest.jsp?id=5' shape='circ' coords='108,165,6' style='visibility:hidden' onmouseover='Show(4)' onmouseout='Hide(4)'>
						<area href='mapTest.jsp?id=58' shape='circ' coords='108,165,6' style='visibility:hidden' onmouseover='Show(4)' onmouseout='Hide(4)'>
						<area href='mapTest.jsp?id=6' shape='circ' coords='508,103,6' style='visibility:hidden' onmouseover='Show(5)' onmouseout='Hide(5)'>
						<area href='mapTest.jsp?id=12' shape='circ' coords='508,103,6' style='visibility:hidden' onmouseover='Show(5)' onmouseout='Hide(5)'>
						<area href='mapTest.jsp?id=27' shape='circ' coords='508,103,6' style='visibility:hidden' onmouseover='Show(5)' onmouseout='Hide(5)'>
						<area href='mapTest.jsp?id=8' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=9' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=13' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=21' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=35' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=47' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=62' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=61' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=63' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=67' shape='circ' coords='427,135,6' style='visibility:hidden' onmouseover='Show(6)' onmouseout='Hide(6)'>
						<area href='mapTest.jsp?id=10' shape='circ' coords='394,140,6' style='visibility:hidden' onmouseover='Show(7)' onmouseout='Hide(7)'>
						<area href='mapTest.jsp?id=11' shape='circ' coords='145,234,6' style='visibility:hidden' onmouseover='Show(8)' onmouseout='Hide(8)'>
						<area href='mapTest.jsp?id=49' shape='circ' coords='145,234,6' style='visibility:hidden' onmouseover='Show(8)' onmouseout='Hide(8)'>
						<area href='mapTest.jsp?id=14' shape='circ' coords='447,165,6' style='visibility:hidden' onmouseover='Show(9)' onmouseout='Hide(9)'>
						<area href='mapTest.jsp?id=59' shape='circ' coords='447,165,6' style='visibility:hidden' onmouseover='Show(9)' onmouseout='Hide(9)'>
						<area href='mapTest.jsp?id=PSU' shape='circ' coords='519,135,6' style='visibility:hidden' onmouseover='Show(10)' onmouseout='Hide(10)'>
						<area href='mapTest.jsp?id=16' shape='circ' coords='568,116,6' style='visibility:hidden' onmouseover='Show(11)' onmouseout='Hide(11)'>
						<area href='mapTest.jsp?id=17' shape='circ' coords='568,116,6' style='visibility:hidden' onmouseover='Show(11)' onmouseout='Hide(11)'>
						<area href='mapTest.jsp?id=52' shape='circ' coords='568,116,6' style='visibility:hidden' onmouseover='Show(11)' onmouseout='Hide(11)'>
						<area href='mapTest.jsp?id=57' shape='circ' coords='568,116,6' style='visibility:hidden' onmouseover='Show(11)' onmouseout='Hide(11)'>
						<area href='mapTest.jsp?id=18' shape='circ' coords='429,118,6' style='visibility:hidden' onmouseover='Show(12)' onmouseout='Hide(12)'>
						<area href='mapTest.jsp?id=19' shape='circ' coords='346,222,6' style='visibility:hidden' onmouseover='Show(13)' onmouseout='Hide(13)'>
						<area href='mapTest.jsp?id=40' shape='circ' coords='346,222,6' style='visibility:hidden' onmouseover='Show(13)' onmouseout='Hide(13)'>
						<area href='mapTest.jsp?id=20' shape='circ' coords='108,175,6' style='visibility:hidden' onmouseover='Show(14)' onmouseout='Hide(14)'>
						<area href='mapTest.jsp?id=22' shape='circ' coords='0,0,6' style='visibility:hidden' onmouseover='Show(15)' onmouseout='Hide(15)'>
						<area href='mapTest.jsp?id=45' shape='circ' coords='0,0,6' style='visibility:hidden' onmouseover='Show(15)' onmouseout='Hide(15)'>
						<area href='mapTest.jsp?id=UTA' shape='circ' coords='351,251,6' style='visibility:hidden' onmouseover='Show(16)' onmouseout='Hide(16)'>
						<area href='mapTest.jsp?id=BU' shape='circ' coords='577,91,6' style='visibility:hidden' onmouseover='Show(17)' onmouseout='Hide(17)'>
						<area href='mapTest.jsp?id=25' shape='circ' coords='302,242,6' style='visibility:hidden' onmouseover='Show(18)' onmouseout='Hide(18)'>
						<area href='mapTest.jsp?id=26' shape='circ' coords='552,99,6' style='visibility:hidden' onmouseover='Show(19)' onmouseout='Hide(19)'>
						<area href='mapTest.jsp?id=28' shape='circ' coords='254,221,6' style='visibility:hidden' onmouseover='Show(20)' onmouseout='Hide(20)'>
					</map>
				</td> 
         	</tr>                 
			<tr>
				<td align=center>
					<%
					String location = request.getParameter("id");
					if(location == null)
						location = "ALL";
					 %>
					<iframe border=0 height="600" width="850" src="viewer.jsp?__report=<%=reportingConfiguration.getReportsFolder() %>fakelocationreport.rptdesign&Location=<%=location %>" frameborder="0"></iframe>
				</td>
			</tr>
	</table>
</body>
</html>
