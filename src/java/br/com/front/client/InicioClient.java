package br.com.front.client;

import br.com.backEnd.JLayer;
import br.com.backEnd.Log;
import br.com.entity.Attributes;

import javax.swing.*;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

@SuppressWarnings("Duplicates")
public class InicioClient {

    static final int CABECALHO = 4;
    static final int TAMANHO_PACOTE = 320 + CABECALHO;
    static final int PORTA_SERVIDOR = 8002;
    static final int PORTA_ACK = 8003;
    long inicio = 0, fim = 0, total = 0;
    Timestamp tempo;

    static boolean CLICK;

    private JPanel jpanelClientView;
    private JTextField textLocalMusica;
    private JTextField textNomeMusica;
    private JRadioButton sequencialRadioButton;
    private JRadioButton aleatorioRadioButton;
    private JTextArea textAreaResult;
    private JButton btnLimpar;
    private JButton btnBaixar;
    private static Attributes attributes = new Attributes();
    public static AudioClip music;
    private int saiFora = 0;

    //instância um objeto da classe Random usando o construtor básico
    Random gerador = new Random();
    JLayer.MP3Musica musica = new JLayer.MP3Musica();
    Log log = new Log();
    private static List<String> msgConsole = new ArrayList<>();
    private int ackAleatorio, seqAleatorio;

    public InicioClient() {
        //textfild
        attributes.setDiretorioMusic(textLocalMusica.getText());
        attributes.setNomeMusic(textNomeMusica.getText());
        //textarea
        log.logCliente(textAreaResult.getText());
        //button
        btnLimpar.addActionListener(new ClearBtnClicked());
        btnBaixar.addActionListener(new BaixarBtnClicked());
    }

    //construtor
    public InicioClient(int portaEntrada, int portaDestino, String caminho) {
        DatagramSocket socketEntrada, socketSaida;
        log.logCliente("Servidor: porta de entrada: " + portaEntrada + ", " + "porta de destino: " + portaDestino + ".");

        int ultimoNumSeq = -1;
        int proxNumSeq = 0;  //proximo numero de sequencia
        boolean transferenciaCompleta = false;  //flag caso a transferencia nao for completa

        //criando sockets
        try {
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();
            log.logCliente("Servidor Conectado...");
            try {
                byte[] recebeDados = new byte[TAMANHO_PACOTE];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);

                FileOutputStream fos = null;

                while (!transferenciaCompleta) {
                    int i = 0;
                    socketEntrada.receive(recebePacote);
                    fim = System.currentTimeMillis();
                    InetAddress enderecoIP = recebePacote.getAddress();

                    total = fim - inicio;
                    int numSeq = ByteBuffer.wrap(Arrays.copyOfRange(recebeDados, 0, CABECALHO)).getInt();
                    log.logCliente("Servidor: Numero de sequencia recebido " + (!CLICK? (numSeq>0?ackAleatorio=gerador.nextInt(numSeq):0) : numSeq));
                    log.logCliente("RTT: " + total);

                    //se o pacote for recebido em ordem
                    if ((numSeq == proxNumSeq)) {
                        //se for ultimo pacote (sem dados), enviar ack de encerramento
                        if (recebePacote.getLength() == CABECALHO) {
                            byte[] pacoteAck = gerarPacote(-2);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                            transferenciaCompleta = true;
                            log.logCliente("Servidor: Todos pacotes foram recebidos! Arquivo criado!");
                            tempo = new Timestamp(System.currentTimeMillis());
                            String date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(tempo.getTime());
                            log.logCliente("Fim Transação: " + date);
                        } else {
                            proxNumSeq = numSeq + TAMANHO_PACOTE - CABECALHO;  //atualiza proximo numero de sequencia
                            byte[] pacoteAck = gerarPacote(proxNumSeq);
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                            log.logCliente("Servidor: Ack enviado " + (!CLICK ? (proxNumSeq>0?seqAleatorio=gerador.nextInt(proxNumSeq):0) : proxNumSeq));
                            inicio = 0;
                            fim = 0;
                            inicio = System.currentTimeMillis();
                            tempo = new Timestamp(System.currentTimeMillis());
                            String date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(tempo.getTime());
                            log.logCliente("Inicio Transação: " + date);
                        }

                        //se for o primeiro pacote da transferencia
                        if (numSeq == 0 && ultimoNumSeq == -1) {
                            //cria arquivo
                            File arquivo = new File(caminho);
                            if (!arquivo.exists()) {
                                arquivo.createNewFile();
                            }
                            fos = new FileOutputStream(arquivo);
                        }
                        //escreve dados no arquivo
                        fos.write(recebeDados, CABECALHO, recebePacote.getLength() - CABECALHO);

                        ultimoNumSeq = numSeq; //atualiza o ultimo numero de sequencia enviado

                        //toca a musica se o arquivo existir
                        tocarMusicaQdoBaixada(caminho);

                    } else {    //se pacote estiver fora de ordem, mandar duplicado
                        byte[] pacoteAck = gerarPacote(ultimoNumSeq);
                        socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                        log.logCliente("Servidor: Ack duplicado enviado " + ultimoNumSeq);
                    }

                }
                if (fos != null) {
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            } finally {
                socketEntrada.close();
                socketSaida.close();
                log.logCliente("Servidor: Socket de entrada fechado!");
                log.logCliente("Servidor: Socket de saida fechado!");
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
    }

    private void tocarMusicaQdoBaixada(String caminho) {
        File file = new File(caminho);
        if (file.exists() && saiFora == 0) {
            saiFora = 1;
            // INSTANCIAÇÃO DO OBJETO FILE COM O ARQUIVO MP3
            File mp3File = new File(caminho);
            try {
                //sleep(5000);
                //toca musica
                musica.tocar(mp3File);
                // CHAMA O METODO QUE TOCA A MUSICA
                musica.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //fim do construtor

    //gerar pacote de ACK
    public byte[] gerarPacote(int numAck) {
        byte[] numAckBytes = ByteBuffer.allocate(CABECALHO).putInt(numAck).array();
        ByteBuffer bufferPacote = ByteBuffer.allocate(CABECALHO);
        bufferPacote.put(numAckBytes);
        return bufferPacote.array();
    }

    //acao do botao limpar
    private class ClearBtnClicked implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            textLocalMusica.setText("");
            textNomeMusica.setText("");
            sequencialRadioButton.setSelected(false);
            aleatorioRadioButton.setSelected(false);
        }
    }

    //acao do botao baixar
    private class BaixarBtnClicked implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!verificarCampos()) {
                CLICK = sequencialRadioButton.isSelected();
                textAreaResult.append(textLocalMusica.getText() + "\n" + textNomeMusica.getText()
                        + "\n" + "Sequencial " + sequencialRadioButton.isSelected() + "\n" + "Aleatório " + aleatorioRadioButton.isSelected());

                textAreaResult.append(" \n RECEBENDO MÚSICA...");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InicioClient client = new InicioClient(PORTA_SERVIDOR, PORTA_ACK, textLocalMusica.getText() + textNomeMusica.getText());
                        textAreaResult.append("\n MÚSICA RECEBIDA...");
                    }
                }).start();
            }
        }
    }

    private boolean verificarCampos() {
        boolean ret = false;
        String msg1 = "";
        String msg2 = "";
        String msg3 = "";
        if (textLocalMusica.getText() == null || textLocalMusica.getText().isEmpty()) {
            msg1 = "1 - Digite o diretório para onde vai a Música.";
            ret = true;
        }
        if (textNomeMusica.getText() == null || textNomeMusica.getText().isEmpty()) {
            msg2 = "2 - Digite o nome que a Música será salva na sua maquina.";
            ret = true;
        }
        if (!sequencialRadioButton.isSelected() && !aleatorioRadioButton.isSelected()) {
            msg3 = "3 - Selecione o rádio button como 'Sequencial' ou 'Aleatório'.";
            ret = true;
        }

        if (ret) {
            JOptionPane.showMessageDialog(null, msg1 + "\n" + msg2 + "\n" + msg3, "Campos nulos ou em branco...", JOptionPane.ERROR_MESSAGE);
        }

        return ret;
    }

    public static void tocador() {
        try {
            music = Applet.newAudioClip(new File(attributes.getNomeMusic()).toURL());
            music.play();
        } catch (MalformedURLException e) {
            System.out.println("Erro. Verifique o diretorio da Música.");
        }
    }

    public static void main(String[] args) {
        layoutNimbus();
        JFrame frame = new JFrame("InicioClient");
        frame.setTitle("Tela do CLIENTE");
        ImageIcon imagemTituloJanela = new ImageIcon("br/com/front/cliente.png");
        frame.setIconImage(imagemTituloJanela.getImage());
        frame.setContentPane(new InicioClient().jpanelClientView);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    //layout nimbus para melhorar a interface
    private static void layoutNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (UnsupportedLookAndFeelException e) {
            e.getMessage();
        } catch (ClassNotFoundException e) {
            e.getMessage();
        } catch (InstantiationException e) {
            e.getMessage();
        } catch (IllegalAccessException e) {
            e.getMessage();
        }
    }

}

