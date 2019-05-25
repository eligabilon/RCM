package br.com.entity;

public class Attributes {

    private String diretorioMusic;
    private String nomeMusic;
    private static String console = "";

    public Attributes() {
    }

    public Attributes(String console) {
        this.setConsole(console);
    }

    public String getDiretorioMusic() {
        return diretorioMusic;
    }

    public void setDiretorioMusic(String diretorioMusic) {
        this.diretorioMusic = diretorioMusic;
    }

    public String getNomeMusic() {
        return nomeMusic;
    }

    public void setNomeMusic(String nomeMusic) {
        this.nomeMusic = nomeMusic;
    }

    public String getConsole() {
        return console;
    }

    public void setConsole(String console) {
        this.console = console;
    }
}
