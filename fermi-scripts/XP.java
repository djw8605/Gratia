import java.io.*;
import java.util.*;
import java.text.*;
import java.util.zip.*;
import java.net.*;

public class XP
{

		public String directorySeparator = "/";
		
		public XP()
		{
		}
		
		//
		// directory support
		//

		public String[] getDirectoryList(String directory)
		{
				String filelist[] = null;
				String results[] = new String[0];
				int i = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						{
								try
										{
												String temp = directory + directorySeparator + filelist[i];
												File file = new File(temp);
												if (file.isDirectory())
														{
																results = append(results,temp);
														}
										}
								catch (Exception ignore)
										{
										}
						}
				return results;
		}
		
		public String[] getDirectoryListRecursive(String directory)
		{
				String filelist[] = null;
				String results[] = new String[0];
				int i = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						{
								try
										{
												String temp = directory + directorySeparator + filelist[i];
												File file = new File(temp);
												if (file.isDirectory())
														{
																results = append(results,temp);
																results = append(results,getDirectoryListRecursive(temp));
														}
										}
								catch (Exception ignore)
										{
										}
						}
				return results;
		}
		
		public String[] getFileListRecursive(String directory,String filetype)
		{
				String filelist[] = null;
				String results[] = new String[0];
				int i = 0;
				int j = 0;
				int imax = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						{
								try
										{
												String temp = directory + directorySeparator + filelist[i];
												File file = new File(temp);
												if (file.isDirectory())
														results = append(results,getFileListRecursive(temp,filetype));
												else if (temp.toLowerCase().endsWith(filetype.toLowerCase()))
														results = append(results,temp);
										}
								catch (Exception ignore)
										{
												ignore.printStackTrace();
										}
						}
				String newResults[] = new String[0];
				for (i = 0; i < results.length; i++)
						{
								if (results[i] != null)
										newResults = append(newResults,results[i]);
						}
								
				return newResults;
		}
		
		public String[] getFileListRecursive(String directory,String filetypes[])
		{
				String filelist[] = null;
				String results[] = new String[0];
				int i = 0;
				int j = 0;
				int imax = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						{
								try
										{
												String temp = directory + directorySeparator + filelist[i];
												File file = new File(temp);
												if (file.isDirectory())
														results = append(results,getFileListRecursive(temp,filetypes));
												else
														{
																for (j = 0; j < filetypes.length; j++)
																		if (temp.toLowerCase().endsWith(filetypes[j].toLowerCase()))
																				results = append(results,temp);
														}
										}
								catch (Exception ignore)
										{
												ignore.printStackTrace();
										}
						}
				String newResults[] = new String[0];
				for (i = 0; i < results.length; i++)
						if (results[i] != null)
								newResults = append(newResults,results[i]);
				return newResults;
		}
		
		public String[] getFileList(String directory)
		{
				String filelist[] = null;
				String results[] = new String[0];
				int i = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						{
								try
										{
												String temp = directory + directorySeparator + filelist[i];
												File file = new File(temp);
												if (file.isDirectory())
														results = append(results,getFileList(temp));
												else
														results = append(results,temp);
										}
								catch (Exception ignore)
										{
										}
						}
				return results;
		}
		
		public String[] getFileList(String directory,String fileType)
		{
				String filelist[] = null;
				Vector files = new Vector();
				int i = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						if (filelist[i].toLowerCase().endsWith(fileType))
								files.addElement(filelist[i]);
				filelist = new String[files.size()];
				for (i = 0; i < files.size(); i++)
						filelist[i] = (String) files.elementAt(i);
				return filelist;
		}
		
		public String[] getFileList(String directory,String filetypes[])
		{
				String filelist[] = null;
				Vector files = new Vector();
				int i = 0;
				int j = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return new String[0];
						}
				for (i = 0; i < filelist.length; i++)
						for (j = 0; j < filetypes.length; j++)
								if (filelist[i].toLowerCase().endsWith(filetypes[j]))
										files.addElement(filelist[i]);
				filelist = new String[files.size()];
				for (i = 0; i < files.size(); i++)
						filelist[i] = (String) files.elementAt(i);
				return filelist;
		}
		
		public void deleteFileList(String directory,String fileType)
		{
				String filelist[] = null;
				Vector files = new Vector();
				int i = 0;
				
				try
						{
								filelist = (new File(directory)).list();
						}
				catch (Exception e)
						{
								System.out.println("XP: Error Retrieving FileList: " + e);
								return;
						}
				for (i = 0; i < filelist.length; i++)
						if (filelist[i].toLowerCase().endsWith(fileType))
								files.addElement(filelist[i]);
				filelist = new String[files.size()];
				for (i = 0; i < files.size(); i++)
						filelist[i] = (String) files.elementAt(i);
				for (i = 0; i < filelist.length; i++)
						{
								try
										{
												(new File(directory + directorySeparator + filelist[i])).delete();
										}
								catch (Exception ignore)
										{
										}
						}
		}
		
		public void deleteFileList(String arg1[])
		{
				int i = 0;
				
				for (i = 0; i < arg1.length; i++)
						{
								try
										{
												(new File(arg1[i])).delete();
										}
								catch (Exception ignore)
										{
										}
						}
		}
		
		//
		// serialization support
		//

		public Object getObject(byte buffer[])
		{
				try
						{
								ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(buffer));
								Object result = input.readObject();
								return result;
						}
				catch (Exception e)
						{
								System.out.println("XP: readObject Error: " + e);
								return null;
						}
		}
  
		public Object getObject(FileInputStream inputStream)
		{
				try
						{
								ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(inputStream));
								Object result = input.readObject();
								return result;
						}
				catch (Exception e)
						{
								System.out.println("XP: readObject Error: " + e);
								return null;
						}
		}
  
		public Object getObject(ObjectInputStream input)
		{
				try
						{
								Object result = input.readObject();
								return result;
						}
				catch (Exception e)
						{
								// System.out.println("XP: getObject Error: " + e);
								return null;
						}
		}
  
		public Object getObject(String path)
		{
				try
						{
								ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path)));
								Object result = input.readObject();
								input.close();
								return result;
						}
				catch (Exception e)
						{
								System.out.println("XP: readObject Error: " + e + " Path: " + path);
								return null;
						}
		}
  
		public Object getObject(String path,int bufferSize)
		{
				try
						{
								ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(path),bufferSize));
								Object result = input.readObject();
								input.close();
								return result;
						}
				catch (Exception e)
						{
								System.out.println("XP: readObject Error: " + e + " Path: " + path);
								return null;
						}
		}
  
		public boolean putObject(String path,Object object)
		{
				try
						{
								ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
								output.writeObject(object);
								output.close();
								return true;
						}
				catch (Exception e)
						{
								System.out.println("XP: putObject Error: " + e + " Path: " + path);
								return false;
						}
		}
  
		public boolean putObject(String path,Object object,int bufferSize)
		{
				try
						{
								ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(path),bufferSize));
								output.writeObject(object);
								output.close();
								return true;
						}
				catch (Exception e)
						{
								System.out.println("XP: putObject Error: " + e + " Path: " + path);
								return false;
						}
		}
  
		public boolean putObject(ObjectOutputStream output,Object object)
		{
				try
						{
								output.writeObject(object);
								return true;
						}
				catch (Exception e)
						{
								System.out.println("XP: putObject Error: " + e);
								return false;
						}
		}
  
		//
		// compression support
		//

		public byte[] compress(Serializable object)
		{
				byte buffer[] = toByteArray(object);
				byte results[] = new byte[0];
				
				try
						{
								ByteArrayOutputStream bstream = new ByteArrayOutputStream(buffer.length);
								GZIPOutputStream gstream = new GZIPOutputStream(bstream);
								ObjectOutputStream stream = new ObjectOutputStream(gstream);
								stream.writeObject(object);
								stream.close();
								return bstream.toByteArray();
						}
				catch (Exception e)
						{
								System.out.println("XP: compress Error: " + e);
								return results;
						}
		}
		
		public Object decompress(byte input[])
		{
				Object result = null;
				try
						{
								ByteArrayInputStream bstream = new ByteArrayInputStream(input);
								GZIPInputStream gstream = new GZIPInputStream(bstream);
								ObjectInputStream stream = new ObjectInputStream(gstream);
								result = stream.readObject();
								stream.close();
								return result;
						}
				catch (Exception e)
						{
								System.out.println("XP: decompress Error: " + e);
								return result;
						}
		}
								
		//
		// file support utilities
		//

		public String get(String path)
		{
				try
						{
								File file = new File(path);
								FileInputStream input = new FileInputStream(file);
								byte[] buffer = new byte[(int) file.length()];
								input.read(buffer,0,(int) file.length());
								input.close();
								return new String(buffer);
						}
				catch (Exception e)
						{
								// System.out.println("File Not Found: " + path + " !!" + "\n");
								return "File Not Found: " + path + " !!" + "\n";
						}
		}
  
		public String get(URL url)
		{
				int i = 0;
				
				try
						{
								InputStream input = url.openStream();
								byte[] buffer = new byte[16 * 4096];
								int status;
								while ((status = input.read(buffer,i++,1)) != -1);
								input.close();
								return new String(buffer,0,i - 1);
						}
				catch (Exception e)
						{
								System.out.println("get(URL): Error: " + e);
								return "File Not Found: " + url + " !!" + "\n";
						}
		}
  
		public byte[] getBinary(String path)
		{
				return getBinary(path,4096);
		}
  
		public byte[] getBinary(String path,int bufferSize)
		{
				try
						{
								File file = new File(path);
								FileInputStream input = new FileInputStream(file);
								BufferedInputStream stream = new BufferedInputStream(input,bufferSize);
								byte[] buffer = new byte[(int) file.length()];
								stream.read(buffer,0,(int) file.length());
								stream.close();
								return buffer;
						}
				catch (Exception e)
						{
								System.out.println("XP: getBinary: " + e);
								e.printStackTrace();
								return null;
						}
		}
  
		public boolean save(String path,String contents)
		{
				try
						{
								FileOutputStream output = new FileOutputStream(path);
								output.write(contents.getBytes());
								output.close();
								return true;
						}
				catch (Exception e)
						{
								e.printStackTrace();
								return false;
						}
		}

		public boolean append(String path,String contents)
		{
				try
						{
								RandomAccessFile output = new RandomAccessFile(path,"rw");
								output.seek(output.length());
								output.write(contents.getBytes());
								output.close();
								return true;
						}
				catch (Exception e)
						{
								System.out.println("Error: " + e + " path: " + path);
								return false;
						}
		}

		public boolean saveBinary(String path,byte[] contents)
		{
				int bufferSize = contents.length;
				
				if (bufferSize > 4 * 4096)
						bufferSize = 4 * 4096;
				
				try
						{
								FileOutputStream output = new FileOutputStream(path);
								BufferedOutputStream stream = new BufferedOutputStream(output,bufferSize);
								stream.write(contents,0,contents.length);
								stream.close();
								return true;
						}
				catch (Exception e)
						{
								System.out.println("saveBinary: Error: " + path);
								e.printStackTrace();
								return false;
						}
		}

		public boolean appendBinary(String path,byte[] contents)
		{
				try
						{
								RandomAccessFile output = new RandomAccessFile(path,"rw");
								output.seek(output.length());
								output.write(contents);
								output.close();
								return true;
						}
				catch (Exception e)
						{
								System.out.println("Error: " + e + " path: " + path);
								return false;
						}
		}

		public boolean appendBinary(RandomAccessFile output,byte[] contents)
		{
				try
						{
								output.seek(output.length());
								output.write(contents);
								return true;
						}
				catch (Exception e)
						{
								System.out.println("Error: " + e);
								return false;
						}
		}

		//
		// parsing support
		//

		public String replaceAll(String source,String from,String to)
		{
				int index;
				int start = 0;
				
				if (from.length() == 0)
						return source;
				
				while (true)
						{
								index = source.indexOf(from,start);
								if (index == -1)
										return source;
								source = source.substring(0,index) + to + source.substring(index + from.length());
								start = index + to.length();
						}
								
				/*
					if (from.length() == 0) return source;
					while ((index = source.indexOf(from)) != -1)
					source = source.substring(0,index) + to + source.substring(index + from.length());
					return source;
				*/
		}
  
		public String replace(String source,String from,String to)
		{
				if (from.length() == 0) return source;
				int index = source.indexOf(from);
				if (index != -1)
						{
								StringBuffer buffer = new StringBuffer(source.length() + to.length());
								buffer.append(source.substring(0,index));
								buffer.append(to);
								buffer.append(source.substring(index + from.length()));
								return buffer.toString();
						}
				else
						return source;
		}
  
		public String replaceRange(String source,String key1,String key2,String to)
		{
				StringBuffer buffer = new StringBuffer();
    
				int index1 = source.indexOf(key1);
				int index2 = source.indexOf(key2);
    
				if ((index1 == -1) || (index2 == -1))
						return source;
				if (index2 < index1)
						return source;
    
				buffer.append(source.substring(0,index1));
				buffer.append(to);
				buffer.append(source.substring(index2 + key2.length()));

				return buffer.toString();
		}
  
		public XSpan find(String source,String key1,int start)
		{
				int index = source.indexOf(key1,start);
				if (index == -1)
						return null;
				return new XSpan(index,index + key1.length());
		}

		public XSpan find(String source,String key1)
		{
				return find(source,key1,0);
		}
  
		public XSpan find(String source,String key1,String key2,int start)
		{
				int index1 = source.indexOf(key1,start);
				int index2 = source.indexOf(key2,start);
				if ((index1 == -1) || (index2 == -1))
						return null;
				if (index1 > index2)
						return null;
				return new XSpan(index1,index2 + key2.length());
		}
  
		public XSpan find(String source,String key1,String key2)
		{
				return find(source,key1,key2,0);
		}

		public XSpan find(String source,String key1,String key2,String key3,int start)
		{
				int index2 = source.indexOf(key2,start);
				if (index2 == -1)
						return null;
				int index1 = source.lastIndexOf(key1,index2);
				int index3 = source.indexOf(key3,index2);

				if ((index1 == -1) || (index2 == -1) || (index3 == -1))
						return null;
				if ((index1 < index2) && (index2 < index3))
						return new XSpan(index1,index3 + key3.length());
				else
						return null;
		}
  
		public XSpan find(String source,String key1,String key2,String key3)
		{
				return find(source,key1,key2,key3,0);
		}

		public XSpan findCell(String source,String variable)
		{
				return find(source,"<TD",variable,"</TD>");
		}
  
		public XSpan findRow(String source,String variable)
		{
				return find(source,"<TR",variable,"</TR>");
		}
  
		public XSpan findOption(String source,String variable)
		{
				return find(source,"<OPT",variable,"</OPTION>");
		}
  
		public String replace(String source,XSpan location)
		{
				StringBuffer buffer = new StringBuffer(source.length() + location.replace.length());
				buffer.append(source.substring(0,location.from));
				buffer.append(location.replace);
				buffer.append(source.substring(location.to));
				return buffer.toString();
		}

		//
		// double support
		//

		public String toString(double arg1[])
		{
				return toString(arg1,"0.000E0");
		}
		
		public String toString(double arg1[],String formatString)
		{
				StringBuffer buffer = new StringBuffer();
				DecimalFormat format = new DecimalFormat(formatString);
				for (int i = 0; i < arg1.length - 1; i++)
						{
								buffer.append(format.format(arg1[i]));
								buffer.append(" ");
						}
				buffer.append(format.format(arg1[arg1.length - 1]));
				return buffer.toString();
		}
		
		public void dump(double arg1[])
		{
				System.out.println(toString(arg1));
		}
		
		public String toString(double arg1[][])
		{
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < arg1.length; i++)
						buffer.append(toString(arg1[i]) + "\n");
				return buffer.toString();
		}
		
		public void dump(double arg1[][])
		{
				for (int i = 0; i < arg1.length; i++)
						dump(arg1[i]);
		}
		
		public void dumpToFile(String path,double arg1[][])
		{
				int i,j;
				try
						{
								FileOutputStream output = new FileOutputStream(path);
								for (i = 0; i < arg1.length; i++)
										{
												StringBuffer buffer = new StringBuffer();
												for (j = 0; j < arg1[i].length - 1; j++)
														{
																buffer.append("" + arg1[i][j]);
																buffer.append(" ");
														}
												buffer.append("" + arg1[i][arg1[i].length - 1]);
												String line = buffer.toString() + "\n";
												output.write(line.getBytes());
										}
								output.close();
						}
				catch (Exception e)
						{
								e.printStackTrace();
						}
		}

		public double[] copy(double values[])
		{
				double result[] = new double[values.length];
				for (int i = 0; i < values.length; i++)
						result[i] = values[i];
				return result;
		}
		
		public double[] subset(double input[],int index[])
		{
				double output[] = new double[index.length];
				int i = 0;
				int j = 0;
				for (i = 0,j = 0; i < index.length; i++)
						{
								if (index[i] < input.length)
										output[j++] = input[index[i]];
								else
										output[j++] = Double.MAX_VALUE;
						}
				return output;
		}
		
		public double[][] subset(double input[][],int index[])
		{
				double output[][] = new double[input.length][index.length];
				int i = 0;
				int j = 0;

				for (i = 0; i < input.length; i++)
						for (j = 0; j < index.length; j++)
								{
										if (index[j] < input[i].length)
												output[i][j] = input[i][index[j]];
										else
												output[i][j] = Double.MAX_VALUE;
								}
				return output;
		}
		
		public double[][] append(double arg1[][],double arg2[])
		{
				double result[][] = new double[arg1.length + 1][arg2.length];
				System.arraycopy(arg1,0,result,0,arg1.length);
				result[result.length - 1] = arg2;
				return result;
		}
		
		public double[][] deleteColumn(double arg1[][],int index)
		{
				double result[][] = new double[arg1.length][arg1[0].length - 1];
				int i = 0;
				int j = 0;
				for (i = 0; i < arg1.length; i++)
						{
								int jindex = 0;
								for (j = 0; j < arg1[i].length; j++)
										if (j != index)
												result[i][jindex++] = arg1[i][j];
						}
				return result;
		}

		public float[][] toFloat(double values[][])
		{
				float result[][] = new float[values.length][values[0].length];
				for (int i = 0; i < values.length; i++)
						for (int j = 0; j < values[i].length; j++)
								result[i][j] = (float) values[i][j];
				return result;
		}

		public float[] toFloat(double values[])
		{
				float result[] = new float[values.length];
				for (int i = 0; i < values.length; i++)
						result[i] = (float) values[i];
				return result;
		}

		public double sum(double values[])
		{
				float result = 0;
				for (int i = 0; i < values.length; i++)
						result+= values[i];
				return result;
		}
		
		public double[] times(double values1[],double values2[])
		{
				double result[] = new double[values1.length];
				for (int i = 0; i < values1.length; i++)
						result[i] = values1[i] * values2[i];
				return result;
		}
		
		public double[] append(double arg1[],double arg2[])
		{
				if (arg1.length == 0)
						return arg2;
				double result[] = new double[arg1.length + arg2.length];
				System.arraycopy(arg1,0,result,0,arg1.length);
				System.arraycopy(arg2,0,result,arg1.length,arg2.length);
				return result;
		}

		public double[] append(double arg1[],double arg2)
		{
				int i,j;
				
				double result[] = new double[arg1.length + 1];
				for (i = 0,j = 0; i < arg1.length; i++,j++)
						result[i] = arg1[i];
				result[result.length - 1] = arg2;
				return result;
		}

		public boolean zerop(double values[])
		{
				for (int i = 0; i < values.length; i++)
						if (Math.abs(values[i]) > 0.00001) return false;
				return true;
		}

		public boolean zerop(double values[],int max)
		{
				for (int i = 0; i < values.length && i < max; i++)
						if (Math.abs(values[i]) > 0.00001) return false;
				return true;
		}

		public boolean nanp(double value)
		{
				String temp = "" + value;
				if (temp.equals("NaN"))
						return true;
				return false;
		}

		public Vector part(double input[],int maxArrayLength)
		{
				Vector results = new Vector();
				int index = 0;
				int i = 0;
				int j = 0;
				for (i = 0; i < input.length; i = i + maxArrayLength)
						{
								double temp[] = new double[maxArrayLength];
								index = 0;
								results.add(temp);
								for (j = i;(j < (i + maxArrayLength)) && (j < input.length); j++)
										temp[index++] = input[j];
						}
				return results;
		}
		
		public double[] rest(double input[],int start)
		{
				int index = 0;
				
				if (input.length <= start)
						return input;
				double result[] = new double[input.length - start];
				for (int i = start; i < input.length; i++)
						result[index++] = input[i];
				return result;
		}
				
		public double[][] toArray(String data)
		{
				StringTokenizer st1 = new StringTokenizer(data,"\n\r");
				Vector temp1 = new Vector();
				int width = 0;
				int i = 0;
				int j = 0;
				while (st1.hasMoreTokens())
						{
								String line = st1.nextToken();
								Vector temp2 = new Vector();
								temp1.add(temp2);
								StringTokenizer st2 = new StringTokenizer(line);
								while (st2.hasMoreTokens())
										temp2.add(st2.nextToken());
								width = temp2.size();
						}
				double result[][] = new double[temp1.size()][width];
				for (i = 0; i < temp1.size(); i++)
						{
								Vector temp2 = (Vector) temp1.elementAt(i);
								for (j = 0; j < temp2.size(); j++)
										result[i][j] = Double.parseDouble((String) temp2.elementAt(j));
						}
				return result;
		}
				
		//
		// float operations
		//

		public String toString(float arg1[])
		{
				return toString(arg1,"0.000E0");
		}
		
		public String toString(float arg1[],String formatString)
		{
				StringBuffer buffer = new StringBuffer();
				DecimalFormat format = new DecimalFormat(formatString);
				for (int i = 0; i < arg1.length - 1; i++)
						{
								buffer.append(format.format(arg1[i]));
								buffer.append(" ");
						}
				buffer.append(format.format(arg1[arg1.length - 1]));
				return buffer.toString();
		}
		
		public void dump(float arg1[])
		{
				System.out.println(toString(arg1));
		}
		
		public String toString(float arg1[][])
		{
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < arg1.length; i++)
						buffer.append(toString(arg1[i]) + "\n");
				return buffer.toString();
		}
		
		public void dump(float arg1[][])
		{
				for (int i = 0; i < arg1.length; i++)
						dump(arg1[i]);
		}
		
		public float[] subset(float input[],int index[])
		{
				float output[] = new float[index.length];
				int i = 0;
				int j = 0;
				for (i = 0,j = 0; i < index.length; i++)
						{
								if (index[i] < input.length)
										output[j++] = input[index[i]];
								else
										output[j++] = Float.MAX_VALUE;
						}
				return output;
		}
		
		public float[] copy(float values[])
		{
				float result[] = new float[values.length];
				for (int i = 0; i < values.length; i++)
						result[i] = values[i];
				return result;
		}
		
		public float[][] copy(float values[][])
		{
				float result[][] = new float[values.length][values[0].length];
				for (int i = 0; i < values.length; i++)
						for (int j = 0; j < values[i].length; j++)
								result[i][j] = values[i][j];
				return result;
		}

		public double[] toDouble(float values[])
		{
				double result[] = new double[values.length];
				for (int i = 0; i < values.length; i++)
						result[i] = (double) values[i];
				return result;
		}
		
		public double[][] toDouble(float values[][])
		{
				double result[][] = new double[values.length][values[0].length];
				for (int i = 0; i < values.length; i++)
						for (int j = 0; j < values[i].length; j++)
								result[i][j] = (double) values[i][j];
				return result;
		}

		public float sum(float values[])
		{
				float result = 0;
				for (int i = 0; i < values.length; i++)
						result+= values[i];
				return result;
		}
		
		public float[][] append(float arg1[][],float arg2[])
		{
				float result[][] = new float[arg1.length + 1][arg2.length];
				System.arraycopy(arg1,0,result,0,arg1.length);
				result[result.length - 1] = arg2;
				return result;
		}
		
		public float[] append(float arg1[],float arg2[])
		{
				int i,j;
				
				float result[] = new float[arg1.length + arg2.length];
				for (i = 0,j = 0; i < arg1.length; i++,j++)
						result[i] = arg1[i];
				for (i = 0; i < arg2.length; i++,j++)
						result[j] = arg2[i];
				return result;
		}

		public float[] append(float arg1[],float arg2)
		{
				float results[] = new float[arg1.length + 1];
				System.arraycopy(arg1,0,results,0,arg1.length);
				results[results.length - 1] = arg2;
				return results;
		}

		public boolean zerop(float values[])
		{
				for (int i = 0; i < values.length; i++)
						if (Math.abs(values[i]) > 0.100) return false;
				return true;
		}

		public boolean zerop(float values[],int max)
		{
				for (int i = 0; i < values.length && i < max; i++)
						if (Math.abs(values[i]) > 0.100) return false;
				return true;
		}

		public boolean nanp(float value)
		{
				String temp = "" + value;
				if (temp.equals("NaN"))
						return true;
				return false;
		}

		public float min(float v1,float v2)
		{
				if (v1 > v2)
						return v2;
				return v1;
		}
		
		public float min(float v1,float v2,float v3)
		{
				if (v1 > min(v2,v3))
						return min(v2,v3);
				return v1;
		}
		
		public float[] toFloatArray(Vector input)
		{
				float results[] = new float[0];
				for (Enumeration enumeration = input.elements(); enumeration.hasMoreElements();)
						results = append(results,(float[]) enumeration.nextElement());
				return results;
		}
		
		public Vector part(float input[],int maxArrayLength)
		{
				Vector results = new Vector();
				int index = 0;
				int i = 0;
				int j = 0;
				for (i = 0; i < input.length; i = i + maxArrayLength)
						{
								float temp[] = new float[maxArrayLength];
								index = 0;
								results.add(temp);
								for (j = i;(j < (i + maxArrayLength)) && (j < input.length); j++)
										temp[index++] = input[j];
						}
				return results;
		}
		
		//
		// integer ops
		//

		public String toString(int arg1[])
		{
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < arg1.length - 1; i++)
						{
								buffer.append("" + arg1[i]);
								buffer.append(" ");
						}
				buffer.append("" + arg1[arg1.length - 1]);
				return buffer.toString();
		}
		
		public void dump(int arg1[])
		{
				System.out.println(toString(arg1));
		}
		
		public String toString(int arg1[][])
		{
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < arg1.length; i++)
						buffer.append(toString(arg1[i]) + "\n");
				return buffer.toString();
		}
		
		public void dump(int arg1[][])
		{
				for (int i = 0; i < arg1.length; i++)
						dump(arg1[i]);
		}

		//
		// dump functions
		//

		public void dump(String message,double values[])
		{
				DecimalFormat format1 = new DecimalFormat("0000");
				DecimalFormat format2 = new DecimalFormat("0.000E0");
				for (int i = 0; i < values.length; i++)
						System.out.println(message + ": Index: " + format1.format(i) + " V: " + format2.format(values[i]));
		}
		
		public void dump(String message,float values[])
		{
				DecimalFormat format1 = new DecimalFormat("0000");
				DecimalFormat format2 = new DecimalFormat("###0.###E0");
				for (int i = 0; i < values.length; i++)
						System.out.println(message + " " + "Index: " + format1.format(i) + " Value: " + format2.format(values[i]));
		}
		
		//
		// utility functions
		//

		public int[] chop(int arg1[], int maxLength)
		{
				int i = 0;
				
				if (arg1.length <= maxLength)
						return arg1;
				
				int result[] = new int[maxLength];
				for (i = 0; i < maxLength; i++)
						result[i] = arg1[i];
				return result;
		}
		
		public int[] copy(int values[])
		{
				int result[] = new int[values.length];
				for (int i = 0; i < values.length; i++)
						result[i] = values[i];
				return result;
		}
		
		public long[] copy(long values[])
		{
				long result[] = new long[values.length];
				for (int i = 0; i < values.length; i++)
						result[i] = values[i];
				return result;
		}
		
		public int[][] copy(int values[][])
		{
				int result[][] = new int[values.length][values[0].length];
				for (int i = 0; i < values.length; i++)
						for (int j = 0; j < values[i].length; j++)
								result[i][j] = values[i][j];
				return result;
		}
		
		public byte[][] copy(byte values[][])
		{
				byte result[][] = new byte[values.length][values[0].length];
				for (int i = 0; i < values.length; i++)
						for (int j = 0; j < values[i].length; j++)
								result[i][j] = values[i][j];
				return result;
		}
		
		public double[][] copy(double values[][])
		{
				double result[][] = new double[values.length][values[0].length];
				for (int i = 0; i < values.length; i++)
						for (int j = 0; j < values[i].length; j++)
								result[i][j] = values[i][j];
				return result;
		}

		public int sum(int values[])
		{
				int result = 0;
				for (int i = 0; i < values.length; i++)
						result+= values[i];
				return result;
		}
		
		public float[] times(float values1[],float values2[])
		{
				float result[] = new float[values1.length];
				for (int i = 0; i < values1.length; i++)
						result[i] = values1[i] * values2[i];
				return result;
		}
		
		public int[] times(int values1[],int values2[])
		{
				int result[] = new int[values1.length];
				for (int i = 0; i < values1.length; i++)
						result[i] = values1[i] * values2[i];
				return result;
		}
		
		public byte[] append(byte arg1[],byte arg2[])
		{
				int i,j;
				byte result[] = new byte[arg1.length + arg2.length];
				
				for (i = 0,j = 0; i < arg1.length; i++,j++)
						result[j] = arg1[i];
				for (i = 0; i < arg2.length; i++,j++)
						result[j] = arg2[i];
				return result;
		}
		
		public int[] append(int arg1[],int arg2[])
		{
				int i,j;
				
				int result[] = new int[arg1.length + arg2.length];
				for (i = 0,j = 0; i < arg1.length; i++,j++)
						result[i] = arg1[i];
				for (i = 0; i < arg2.length; i++,j++)
						result[j] = arg2[i];
				return result;
		}

		public int[] append(int arg1[],int arg2)
		{
				int i,j;
				
				int result[] = new int[arg1.length + 1];
				for (i = 0,j = 0; i < arg1.length; i++,j++)
						result[i] = arg1[i];
				result[result.length - 1] = arg2;
				return result;
		}

		public long[] append(long arg1[],long arg2[])
		{
				int i,j;
				
				long result[] = new long[arg1.length + arg2.length];
				for (i = 0,j = 0; i < arg1.length; i++,j++)
						result[i] = arg1[i];
				for (i = 0; i < arg2.length; i++,j++)
						result[j] = arg2[i];
				return result;
		}

		public long[] append(long arg1[],long arg2)
		{
				int i,j;
				
				long result[] = new long[arg1.length + 1];
				for (i = 0,j = 0; i < arg1.length; i++)
						result[i] = arg1[i];
				result[result.length - 1] = arg2;
				return result;
		}

		public long[] appendUnique(long arg1[],long arg2[])
		{
				Hashtable table = new Hashtable();
				int i;
				
				for (i = 0; i < arg1.length; i++)
						table.put(new Long(arg1[i]),new Long(arg1[i]));
				
				for (i = 0; i < arg2.length; i++)
						table.put(new Long(arg2[i]),new Long(arg2[i]));
				
				long results[] = new long[table.size()];
				int index = 0;
				
				for (Enumeration enumeration = table.elements(); enumeration.hasMoreElements();)
						{
								Long value = (Long) enumeration.nextElement();
								results[index] = value.longValue();
								index++;
						}
				
				return results;
		}

		public long[] unique(long arg1[])
		{
				int i,j,index;
				long value;
				index = 0;
				boolean flag;
				
				long result[] = new long[arg1.length];
				
				for (i = 0; i < arg1.length; i++)
						{
								flag = true;
								value = arg1[i];
								for (j = 0; j < index; j++)
										if (result[j] == value)
												flag = false;
								if (flag)
										{
												result[index] = value;
												index++;
										}
						}
				long result2[] = new long[index];
				System.arraycopy(result,0,result2,0,index);
				return result2;
		}

		public int[] unique(int arg1[])
		{
				int i,j,index;
				int value;
				index = 0;
				boolean flag;
				
				int result[] = new int[arg1.length];
				
				for (i = 0; i < arg1.length; i++)
						{
								flag = true;
								value = arg1[i];
								for (j = 0; j < index; j++)
										if (result[j] == value)
												flag = false;
								if (flag)
										{
												result[index] = value;
												index++;
										}
						}
				int result2[] = new int[index];
				System.arraycopy(result,0,result2,0,index);
				return result2;
		}

		public long[] chop(long input[],int maxLength)
		{
				if (input.length < maxLength)
						return input;
				long results[] = new long[maxLength];
				for (int i = 0; i < maxLength; i++)
						results[i] = input[i];
				return results;
		}
		
		public long[] remove(long arg1[],long arg2[])
		{
				Hashtable temp = new Hashtable();
				int i = 0;
				for (i = 0; i < arg2.length; i++)
						temp.put(new Long(arg2[i]),new Long(arg2[i]));
				long results[] = new long[arg1.length];
				int index = 0;
				for (i = 0; i < arg1.length; i++)
						if (temp.get(new Long(arg1[i])) == null)
								results[index++] = arg1[i];
				long finalResults[] = new long[index];
				System.arraycopy(results,0,finalResults,0,index);
				return finalResults;
		}
		
		public Vector part(String input[],int maxArrayLength)
		{
				Vector results = new Vector();
				int index = 0;
				int i = 0;
				int j = 0;
				for (i = 0; i < input.length; i = i + maxArrayLength)
						{
								String temp[] = new String[maxArrayLength];
								index = 0;
								results.add(temp);
								for (j = i;(j < (i + maxArrayLength)) && (j < input.length); j++)
										temp[index++] = input[j];
						}
				return results;
		}
		
		public String[] chop(String input[],int maxLength)
		{
				if (input.length < maxLength)
						return input;
				String results[] = new String[maxLength];
				for (int i = 0; i < maxLength; i++)
						results[i] = input[i];
				return results;
		}
		
		public String[] append(String arg1[],String arg2[])
		{
				if (arg1.length == 0)
						return arg2;
				String results[] = new String[arg1.length + arg2.length];
				int i = 0;
				int j = 0;
				for (i = 0; i < arg1.length; i++)
						results[j++] = arg1[i];
				for (i = 0; i < arg2.length; i++)
						results[j++] = arg2[i];
				return results;
		}
		
		public String[] append(String arg1,String arg2[])
		{
				String results[] = new String[arg2.length + 1];
				int i = 0;
				int j = 0;
				results[j++] = arg1;
				for (i = 0; i < arg2.length; i++)
						results[j++] = arg2[i];
				return results;
		}
		
		public String[] append(String arg1[],String arg2)
		{
				String results[] = new String[arg1.length + 1];
				int i = 0;
				int j = 0;
				for (i = 0; i < arg1.length; i++)
						results[j++] = arg1[i];
				results[j++] = arg2;
				return results;
		}
		
		public String[] unique(String arg1[])
		{
				Hashtable table = new Hashtable();
				int i = 0;
				for (i = 0; i < arg1.length; i++)
						if (arg1[i] != null)
								table.put(arg1[i],arg1[i]);
				String results[] = new String[table.size()];
				int index = 0;
				for (Enumeration enumeration = table.keys(); enumeration.hasMoreElements();)
						{
								String temp = (String) enumeration.nextElement();
								results[index++] = temp;
						}
				return results;
		}
		
		public String[] delete(String arg1[],String arg2)
		{
				int i = 0;
				int imax = 0;
				for (i = 0; i < arg1.length; i++)
						if (arg1[i].compareTo(arg2) != 0)
								imax++;
				String results[] = new String[imax];
				imax = 0;
				for (i = 0; i < arg1.length; i++)
						if (arg1[i].compareTo(arg2) != 0)
								results[imax++] = arg1[i];
				return results;
		}
		
		public String[] search(String arg1[],String arg2,boolean endsWith)
		{
				int i = 0;
				String result[] = new String[0];
				
				for (i = 0; i < arg1.length; i++)
						{
								if (endsWith)
										{
												if (arg1[i].toLowerCase().endsWith(arg2.toLowerCase()))
														result = append(result,arg1[i]);
										}
								else if (arg1[i].toLowerCase().indexOf(arg2.toLowerCase()) > -1)
										result = append(result,arg1[i]);
						}
				return result;
		}
		
		public String[] search(String arg1[],String arg2)
		{
				return search(arg1,arg2,false);
		}
		
		public String[] sort(String arg1[])
		{
				Arrays.sort(arg1);
				return arg1;
		}
		
		//
		// conversion support
		//

		public long[] toLong(int input[])
		{
				long results[] = new long[input.length];
				for (int i = 0; i < input.length; i++)
						results[i] = input[i];
				return results;
		}
		
		public byte[] toByteArray(Object object)
		{
				return toByteArray(object,4096);
		}
		
		public byte[] toByteArray(Object object,int bufferSize)
		{
				try
						{
								ByteArrayOutputStream output = new ByteArrayOutputStream(bufferSize);
								ObjectOutputStream stream = new ObjectOutputStream(output);
								stream.writeObject(object);
								stream.close();
								return output.toByteArray();
						}
				catch (Exception e)
						{
								System.out.println("Error: " + e);
								return new byte[0];
						}
		}
		
		public Object fromByteArray(byte data[])
		{
				try
						{
								ByteArrayInputStream input = new ByteArrayInputStream(data);
								ObjectInputStream stream = new ObjectInputStream(input);
								return stream.readObject();
						}
				catch (Exception e)
						{
								System.out.println("Error: " + e);
								return "";
						}
		}

		public Object duplicate(Object input)
		{
				return fromByteArray(toByteArray(input));
		}
		
		//
		// other stuff
		//

		public long random(long[] values)
		{
				int index = (int) (Math.random() * values.length);
				return values[index];
		}
		
		public int random(int max)
		{
				return (int) (Math.random() * max);
		}
		
		//
		// exception handling
		//

		public String parseException(Exception e)
		{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				StringBuffer buffer = new StringBuffer();
				String result = "";
				String cr = "\n";
				XP xp = new XP();
				
				e.printStackTrace(pw);
				pw.flush();

				buffer = sw.getBuffer();
				for (int i = 0; i < buffer.length(); i++)
						buffer.setCharAt(i,translate(buffer.charAt(i)));
				result = buffer.toString();
				return result;
		}

		private char translate(char input)
		{
				String string = "!@#$%^&*()_+`-={}[]\\:\";'<>?,./ ";

				if (input == '\n') return input;
				if (input >= 'a' && input <= 'z') return input;
				if (input >= 'A' && input <= 'Z') return input;
				if (input >= '0' && input <= '9') return input;
				if (string.indexOf(input) > -1) return input;
				return ' ';
		}


}
