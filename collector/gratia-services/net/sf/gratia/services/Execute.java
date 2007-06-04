package net.sf.gratia.services;

import java.io.*;

class StreamGobbler extends Thread {
    InputStream is;

    String type;

    StreamGobbler(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
                Logging.log(type + ">" + line);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

public class Execute {
    public static int execute(String cmd) {
        try {

            Runtime rt = Runtime.getRuntime();
            Logging.log("Executing: " + cmd);
            Process proc = rt.exec(cmd);
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc
                    .getErrorStream(), "ERROR");

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc
                    .getInputStream(), "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            int exitValue = proc.waitFor();
            Logging.log("exitValueue: " + exitValue);
            return exitValue;
        } catch (Throwable t) {
            t.printStackTrace();
            return 1;
        }
    }

    public static int execute(String cmd[]) {
        String newcommand = cmd[0];

        for (int i = 1; i < cmd.length; i++)
            newcommand = newcommand + " " + cmd[i];
        try {

            Runtime rt = Runtime.getRuntime();
            Logging.log("Executing: " + newcommand);
            Process proc = rt.exec(cmd);
            // any error message?
            StreamGobbler errorGobbler = new StreamGobbler(proc
                    .getErrorStream(), "ERROR");

            // any output?
            StreamGobbler outputGobbler = new StreamGobbler(proc
                    .getInputStream(), "OUTPUT");

            // kick them off
            errorGobbler.start();
            outputGobbler.start();

            // any error???
            int exitValue = proc.waitFor();
            Logging.log("exitValueue: " + exitValue);
            return exitValue;
        } catch (Throwable t) {
            t.printStackTrace();
            return 1;
        }
    }
}
