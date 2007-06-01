<%@ page language="java" session="false" contentType="text/html; charset=windows-1252"%>
<%@ page import="java.util.*, java.io.*"%>

<%
   // Get the input file name, passed as a URL parameter. If file name is null do nothing.
   
	String inputFile = request.getParameter("csvFile");
	if(inputFile == null)
		inputFile = "";
		
	if(inputFile.trim().length() > 0)
	{
		// If a file is specified download it.
		
		File fileToDownload = new File (inputFile);
		String fileName = fileToDownload.getName();
		
		try {
		
  			// - Set the http content type to "application/download"
  			// - Initialize the http content-disposition header to
   			//    indicate a file attachment with a default filename
   			//    the same as the input file.
			// - Validate the data after download
		 
		 
			response.setContentType("application/download"); 
   			response.setHeader("Content-Disposition","Attachment; filename=\"" + fileName + "\"");
			response.setHeader("cache-control", "must-revalidate");

   			// response.setHeader("cache-control", "no-cache"); // IE does not work if set
			// response.setDateHeader ("Expires", 0);	    // IE does not work if set
			
			FileInputStream fileInputStream = new FileInputStream(fileToDownload);
			ServletOutputStream fileOutputStream = response.getOutputStream();
			
			byte[] buf = new byte[4 * 1024];		// 4kb buffer to read/write at a time
			int len = 0;
			while ((len=fileInputStream.read(buf)) !=-1)	// read input file 4kb at a time
			{
				fileOutputStream.write(buf,0,len);	// write output file
			}

			fileOutputStream.flush();			// Flush output buffer stream 
   			fileOutputStream.close();			// Close output file
   			fileInputStream.close();			// Close input file
   		}
		catch(Exception e) 					// file IO errors
   			{
   			e.printStackTrace();
			}
	}

%>
