var xmlHttp

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
		document.getElementById("getRoles").innerHTML=xmlHttp.responseText;
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
		document.getElementById("confirmRole").innerHTML=xmlHttp.responseText;
		parent.adminDashboard.location = "./dashboard.jsp";
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
