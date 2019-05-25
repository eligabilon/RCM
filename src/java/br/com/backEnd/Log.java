package br.com.backEnd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Log {
    Timestamp nomeLog = new Timestamp(System.currentTimeMillis());
    String nome = new SimpleDateFormat("dd-MM-yyyy_hh:mm:ss").format(nomeLog.getTime());

    public void logCliente(String msgLog) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("leechers-"+nome+".log", true));
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
            bw = new BufferedWriter(new FileWriter("seeders-"+nome+".log", true));
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
