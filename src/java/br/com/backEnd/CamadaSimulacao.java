package br.com.backEnd;

import sun.awt.SunHints;

import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class CamadaSimulacao {
    static Random gerador = new Random();
    static long tn;
    public static List <Long> listaTempo = new ArrayList<Long>();

    public static void CalculaTempo(long t, long RTT, long x){
        RTT = 5;
        x = gerador.nextInt(Integer.valueOf(String.valueOf(x)));
        Math.exp(x);
        tn = t + (RTT / 2) + x;
        listaTempo.add(tn);
        Collections.sort(listaTempo);
    }
}