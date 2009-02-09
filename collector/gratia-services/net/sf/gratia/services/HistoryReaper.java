package net.sf.gratia.services;

import net.sf.gratia.util.XP;

import net.sf.gratia.util.Configuration;

import net.sf.gratia.util.Logging;

import java.util.*;
import java.util.concurrent.locks.*;
import java.sql.*;
import java.io.*;
import java.text.*;

import org.apache.tools.bzip2.*;
import com.ice.tar.*;

public class HistoryReaper implements Runnable {
    Properties p;

    String filenames[] = new String[0];

    private static Lock reaperLock = new ReentrantLock();
    private static ReadWriteLock statusLock = new ReentrantReadWriteLock();

    private static Boolean in_progress = false;

    public static Boolean inProgress() {
        try {
            statusLock.readLock().lock();
            return in_progress;
        }
        finally {
            statusLock.readLock().unlock();
        }
    }

    private static void setRunStatus(Boolean status) {
        statusLock.writeLock().lock();
        try {
            in_progress = status;
        }
        finally {
            statusLock.writeLock().unlock();
        }
    }

    public HistoryReaper() {
      p = net.sf.gratia.util.Configuration.getProperties();
    }

    public void run() {
        reaperLock.lock();
        try {
            setRunStatus(true);
            Logging.fine("HistoryReaper: Starting.");
            getDirectories();
            Logging.fine("HistoryReaper: Complete.");
        }
        finally {
            setRunStatus(false);
            reaperLock.unlock();
        }
    }

    public void deleteDirectory(String path)
    {
       try
       {
          File file = new File(path);
          XP.forceDelete(file, false);
          Logging.log("HistoryReaper: Deleted Directory: " + path);
       }
       catch (Exception ignore)
       {
       }
    }

    private static void addFiles(File file, TarOutputStream tos,String strip) throws IOException, FileNotFoundException
    {
        if (file.isDirectory()) {
            //Create an array with all of the files and subdirectories
            //of the current directory.
            String[] fileNames = file.list();
            if (fileNames != null) {
                //Recursively add each array entry to make sure that we get
                //subdirectories as well as normal files in the directory.
                for (int i=0; i<fileNames.length; i++)  {
                    addFiles(new File(file, fileNames[i]),tos,strip);
                }
            }
            file.delete();
        }
        //Otherwise, a file so add it as an entry to the tar file.
        else {
            byte[] buf = new byte[1024];
            int len;
            //Create a new Tar entry with the file's name.
            String filename = file.toString();
            filename = filename.substring(strip.length()+1);
            TarEntry tarEntry = new TarEntry(filename);

            tarEntry.setSize(file.length());
            //Create a buffered input stream out of the file
            //we're trying to add into the Tar
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            tos.putNextEntry(tarEntry);
            //Read bytes from the file and write into the Tar.
            while( (len= in.read(buf)) != -1 )
                tos.write(buf,0,len);
            //Close the input stream.
            in.close();
            //Close this entry in the Tar stream.
            tos.closeEntry();

            file.delete();
        }
    }

    public void compressDirectory(String dir) 
    {
        File directory = new File(dir);
        File filelist[] = directory.listFiles();

        int compress_ops = 0;
        for (int i = 0; i < filelist.length; i++)
            {
                File file = filelist[i];
                if (file.isDirectory()) {

                    try {
                        String saveFile = file.getAbsolutePath() + ".tar.bz2";

                        FileOutputStream fos = new FileOutputStream(saveFile);
                        fos.write(((String)"BZ").getBytes());

                        CBZip2OutputStream gzos = new CBZip2OutputStream(fos);

                        TarOutputStream tos = new TarOutputStream(gzos);
                        //tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
                        addFiles(file,tos,dir);
                        tos.close();
                        ++compress_ops;
                        if ((compress_ops % 5) == 0) {
                            GCHelper.runGC(); // Trigger manual GC because we
                            // don't seem to be doing it often
                            // enough in this tight loop otherwise.
                        }
                    } catch (Exception e) {
                        Logging.warning("HistoryReaper: Compression or deletion of "+file.getAbsolutePath()+" failed\nError: "+e.getMessage());
                    }
                }
            }
    }
    
    public void getDirectories()
    {
        int i = 0;
        Vector history = new Vector();
        Vector old = new Vector();
        String path = System.getProperties().getProperty("catalina.home") + "/gratia/data";
        path = XP.replaceAll(path, "\\", "/");
        Logging.log("HistoryReaper: Path: " + path);
        String temp[] = XP.getDirectoryList(path);
        for (i = 0; i < temp.length; i++)
            {
                String directory = temp[i];
                if (directory.indexOf("history") > -1) 
                    {
                        history.add(temp[i]);
                    }
                else if (directory.indexOf("old") > -1)
                    {
                        old.add(temp[i]);
                    }
            }
        Logging.log("HistoryReaper: 'old' Directories To Process: " + history.size());
        Logging.log("HistoryReaper: 'history' Directories To Process: " + old.size());
        //
        // figure out which directories to delete
        //
        java.util.Date now = new java.util.Date();
        long nowmilli = now.getTime();

        long historymilli = Long.parseLong(p.getProperty("maintain.history.log"));
        Logging.log("HistoryReaper: using default compress time ("+historymilli+" days)");
        historymilli = historymilli * 24 * 60 * 60 * 1000;
        java.util.Date oldest = new java.util.Date(nowmilli - historymilli);
        Calendar beginning = Calendar.getInstance();
        beginning.setTime(oldest);
        beginning.set(Calendar.SECOND, 0);
        beginning.set(Calendar.MINUTE, 0);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhh");

        long compressmilli = 2;
        try {
            compressmilli = Long.parseLong(p.getProperty("maintain.history.compress"));
            Logging.log("HistoryReaper: using configure compress time ("+compressmilli+" hours)");
        } catch (Exception e) {
            Logging.log("HistoryReaper: using default compress time ("+compressmilli+" hours)");
        }
        compressmilli = compressmilli * 60 * 60 * 1000;
        java.util.Date coldest = new java.util.Date(nowmilli - compressmilli);
        Calendar cbeginning = Calendar.getInstance();
        cbeginning.setTime(coldest);
        cbeginning.set(Calendar.SECOND, 0);
        cbeginning.set(Calendar.MINUTE, 0);
        SimpleDateFormat cformat = new SimpleDateFormat("yyyyMMddhh");

        String beginningHistory = path + "/history-" + format.format(beginning.getTime());
        Logging.log("HistoryReaper: First History Directory To Process: " + beginningHistory);

        String beginningOld = path + "/old-" + format.format(beginning.getTime());
        Logging.log("HistoryReaper: First Old Directory To Process: " + beginningOld);

        String cbeginningHistory = path + "/history-" + cformat.format(cbeginning.getTime());
        Logging.log("HistoryReaper: First History Directory To Compress: " + cbeginningHistory);

        String cbeginningOld = path + "/old-" + cformat.format(cbeginning.getTime());
        Logging.log("HistoryReaper: First Old Directory To Compress: " + cbeginningOld);

        //
        // now - screen/delete older directories
        //
        String oldhistory = path + "/history2";
        String oldold = path + "/old2";
        for (i = 0; i < history.size(); i++)
            {
                String directory = (String)history.elementAt(i);
                if (directory.startsWith(oldhistory) || (directory.compareTo(beginningHistory) < 0))
                    {
                        Logging.fine("HistoryReaper: Deleting Directory: " + directory);
                        deleteDirectory(directory);
                    }
                else if (directory.startsWith(oldold) || directory.compareTo(cbeginningHistory) < 0)
                    {
                        Logging.fine("HistoryReaper: Compressing Directory: " + directory);
                        compressDirectory(directory);
                    }
            }
        for (i = 0; i < old.size(); i++)
            {
                String directory = (String)old.elementAt(i);
                if (directory.compareTo(beginningOld) < 0)
                    {
                        Logging.fine("HistoryReaper: Deleting Directory: " + directory);
                        deleteDirectory(directory);

                    } else if (directory.compareTo(cbeginningOld) < 0)
                    {
                        Logging.fine("HistoryReaper: Compressing Directory: " + directory);
                        compressDirectory(directory);
                    } 
            }
    }

}
