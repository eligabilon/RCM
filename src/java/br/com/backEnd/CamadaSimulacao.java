package br.com.backEnd;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class CamadaSimulacao{
    static Random gerador = new Random();
    static long RTT;
    static long x;
    static long tn;
    static long t;
    public static List <Long> listaTempo = new ArrayList<Long>();
    public static void CalculaTempo(long t){
        RTT = 5;
        x = gerador.nextInt(100);
        Math.exp(x);
        tn = t + (RTT/2) + x;
        listaTempo.add(tn);
        Collections.sort(listaTempo);
    }
}

