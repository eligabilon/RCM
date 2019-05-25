package br.com.backEnd;


import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class JLayer {

//	public static void main(String[] args) {
//
//		// STRING COM O CAMINHO DO ARQUIVO MP3 A SER TOCADO
//		String path = "C:\\Users\\Gabilon\\Desktop\\trabalho-redes\\src\\java\\br\\com\\backEnd\\violao.mp3";
//
//		// INSTANCIAÇÃO DO OBJETO FILE COM O ARQUIVO MP3
//		File mp3File = new File(path);
//
//		// INSTANCIAÇÃO DO OBJETO MP3Música DA CLASS INTERNA
//		MP3Música Música = new MP3Música();
//		Música.tocar(mp3File);
//
//		// CHAMA O METODO QUE TOCA A Música
//		Música.start();
//	}

    /**
     * ====================================================================
     * CLASS INTERNA MP3 Música QUE EXTENDE DE THREAD PARA TRABALHAR
     * PERFEITAMENTE NA APLICAÇÂO SEM TRAVAMENTO NA EXECUÇÃO
     * ====================================================================
     */
    public static class MP3Musica extends Thread {

        // OBJETO PARA O ARQUIVO MP3 A SER TOCADO
        private File mp3;

        // OBJETO PLAYER DA BIBLIOTECA JLAYER QUE TOCA O ARQUIVO MP3
        private Player player;

        /**
         * CONSTRUTOR RECEBE O OBJETO FILE REFERECIANDO O ARQUIVO MP3 A SER
         * TOCADO E ATRIBUI AO ATRIBUTO DA CLASS
         *
         * @param mp3
         */
        public void tocar(File mp3) {
            this.mp3 = mp3;
        }

        /**
         * ===============================================================
         * ======================================METODO RUN QUE TOCA O MP3
         * ===============================================================
         */
        public void run() {
            try {
                FileInputStream fis = new FileInputStream(mp3);
                BufferedInputStream bis = new BufferedInputStream(fis);

                this.player = new Player(bis);
                System.out.println("Tocando Música!");

                this.player.play();
                System.out.println("Terminado Música!");

            } catch (Exception e) {
                System.out.println("Problema ao tocar Música" + mp3);
                e.printStackTrace();
            }
        }
    }
}