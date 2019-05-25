package br.com.backEnd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Log {

    public void logCliente(String msgLog) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("leechers.log", true));
            bw.write(msgLog);
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally { // always close the file
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ioe2) {
                    ioe2.getCause();
                }
            }
        }
    }

    public void logServidor(String msgLog) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("seeders.log", true));
            bw.write(msgLog);
            bw.newLine();
            bw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally { // always close the file
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException ioe2) {
                    ioe2.getCause();
                }
            }
        }
    }
}
