var xmlHttp;

function getRoles(str)
{
	xmlHttp=GetXmlHttpObject();
	if (xmlHttp==null)
	{
		alert ("Your browser does not support AJAX!");
		return;
	}
	var url="getRoles.jsp";
	url=url+"?selectedVO="+str;
	xmlHttp.onreadystatechange=stateChangedGetRoles;
	xmlHttp.open("GET",url,true);
	xmlHttp.send(null);
}

function stateChangedGetRoles()
{
	if (xmlHttp.readyState==4)
	{
		document.getElementById("displayRoles").innerHTML=xmlHttp.responseText;
	}
	else
	{
		document.getElementById("displayRoles").innerHTML="Please wait. Processing your request";	
	}
}


function confirmRole(str)
{
	xmlHttp=GetXmlHttpObject();
	if (xmlHttp==null)
	{
		alert ("Your browser does not support AJAX!");
		return;
	}
	var url="setRole.jsp";
	url=url+"?selectedRole="+str;
	xmlHttp.onreadystatechange=stateChangedConfirmRole;
	xmlHttp.open("GET",url,true);
	xmlHttp.send(null);
}

function stateChangedConfirmRole()
{
	if (xmlHttp.readyState==4)
	{
		document.getElementById("roleSelected").innerHTML=xmlHttp.responseText;
		parent.location = "./index.html";
		//parent.adminDashboard.location = "./dashboard.jsp";
	}
	else
	{
		document.getElementById("displayRoles").innerHTML="Please wait. Processing your request";	
	}
}

function GetXmlHttpObject()
{
	var xmlHttp=null;
	try
	{
// Firefox, Opera 8.0+, Safari
		xmlHttp=new XMLHttpRequest();
	}
	catch (e)
	{
// Internet Explorer
		try
		{
			xmlHttp=new ActiveXObject("Msxml2.XMLHTTP");
		}
		catch (e)
		{
			xmlHttp=new ActiveXObject("Microsoft.XMLHTTP");
		}
	}
	return xmlHttp;
}
