package net.sf.gratia.services;

import net.sf.gratia.util.Logging;

import java.io.*;
import java.net.*;

public class Post
{
    public StringBuffer buffer = new StringBuffer();
    public String destination;
    public Boolean success;
    public String errorMsg;
    public Exception exception;

    public Post(String destination,String command)
    {
	this.destination = destination;
	success = true;
	try
	    {
		buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public Post(String destination,String command,String arg1)
    {
	this.destination = destination;
	success = true;
	try
	    {
		buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public Post(String destination,String command,String arg1,String arg2)
    {
	this.destination = destination;
	success = true;
	try
	    {
		buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public Post(String destination,String command,String arg1,String arg2,String arg3)
    {
	this.destination = destination;
	success = true;
	try
	    {
		buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg3", "UTF-8") + "=" + URLEncoder.encode(arg3,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public Post(String destination,String command,String arg1,String arg2,String arg3,String arg4)
    {
	this.destination = destination;
	success = true;
	try
	    {
		buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg3", "UTF-8") + "=" + URLEncoder.encode(arg3,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg4", "UTF-8") + "=" + URLEncoder.encode(arg4,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public Post(String destination,String command,String arg1,String arg2,String arg3,String arg4,String arg5)
    {
	this.destination = destination;
	success = true;
	try
	    {
		buffer.append(URLEncoder.encode("command", "UTF-8") + "=" + URLEncoder.encode(command,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg1", "UTF-8") + "=" + URLEncoder.encode(arg1,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg2", "UTF-8") + "=" + URLEncoder.encode(arg2,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg3", "UTF-8") + "=" + URLEncoder.encode(arg3,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg4", "UTF-8") + "=" + URLEncoder.encode(arg4,"UTF-8"));
		buffer.append("&");
		buffer.append(URLEncoder.encode("arg5", "UTF-8") + "=" + URLEncoder.encode(arg5,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public void add(String key,String value)
    {
        if (!success) return;

	success = true;
	try
	    {
		buffer.append("&");
		buffer.append(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value,"UTF-8"));
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
	    }
    }

    public String send(boolean printError)
    {
        if (!success) {
            if (printError) exception.printStackTrace();
            return null;
        }

	StringBuffer received = new StringBuffer();
	success = true;
	try
	    {
		URL url = new URL(destination);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		String temp = buffer.toString();
		for (int i = 0; i < temp.length(); i++)
		    writer.write(temp,i,1);
		// writer.write(buffer.toString());
		writer.flush();
    
		// Get the response

		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null)
		    received.append(line);
		writer.close();
		reader.close();
		return received.toString();
	    }
	catch (Exception e)
	    {
		success = false;
		errorMsg = e.toString();
		exception = e;
                if (printError) e.printStackTrace();
		return null;
	    }
    }

    public String send() {
        return send(false);
    }

    public static void main(String args[])
    {
	Post post = new Post("http://localhost:8080/gratia/rmi","xxxxxxx");
	String response = post.send();
	Logging.log("R: " + response);
    }
}
