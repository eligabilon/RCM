package br.com.front.server1;

import br.com.backEnd.CamadaSimulacao;
import br.com.backEnd.Log;
import br.com.backEnd.TextAreaOutputStream;
import br.com.entity.Attributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

@SuppressWarnings("Duplicates")
public class InicioServer1 {
    static final int CABECALHO = 4;
    static final int TAMANHO_PACOTE = 320;  // (numSeq:4, dados=320) Bytes : 324 Bytes total
    static final int TAMANHO_JANELA = 10;
    static final int VALOR_TEMPORIZADOR = 1000;
    static final int PORTA_SERVIDOR = 8002;
    static final int PORTA_ACK = 8003;

    private int cont = 0;
    private JPanel jpanelServer1View;
    private JTextField textIP;
    private JTextField textLocalMusica;
    private JTextArea textAreaResult;
    private JButton btnLimpar;
    private JButton btnBaixar;
    private JCheckBox localhostCheckBox;
    private JTextField campoRTT;
    private JTextField campoE;
    private Attributes attributes = new Attributes();

    Log log = new Log();

    Timestamp inicio, tempoFinal;
    int base;    // numero da janela
    int proxNumSeq;   //proximo numero de sequencia na janela
    String caminho;     //diretorio + nome do arquivo
    List<byte[]> listaPacotes;
    Timer timer;
    Semaphore semaforo;
    boolean transferenciaCompleta;

    public InicioServer1() {
        //textfild
        File file = new File("");
        attributes.setDiretorioMusic(file.getAbsolutePath());
        attributes.setNomeMusic(textLocalMusica.getText());

        //checkbox
        localhostCheckBox.addActionListener(new CheckboxClicked());

        //textarea
        attributes.setConsole(textAreaResult.getText());
        //button
        btnLimpar.addActionListener(new ClearBtnClicked());
        btnBaixar.addActionListener(new BaixarBtnClicked());
    }

    //construtor
    public InicioServer1(int portaDestino, int portaEntrada, String caminho, String enderecoIp) {
        base = 0;
        proxNumSeq = 0;
        this.caminho = caminho;
        listaPacotes = new ArrayList<>(TAMANHO_JANELA);
        transferenciaCompleta = false;
        DatagramSocket socketSaida, socketEntrada;
        semaforo = new Semaphore(1);
        log.logServidor("Servidor: porta de destino: " + portaDestino + ", porta de entrada: " + portaEntrada + ", caminho: " + caminho);

        try {
            //criando sockets
            socketSaida = new DatagramSocket();
            socketEntrada = new DatagramSocket(portaEntrada);

            //criando threads para processar os dados
            InicioServer1.ThreadEntrada tEntrada = new InicioServer1.ThreadEntrada(socketEntrada);
            InicioServer1.ThreadSaida tSaida = new InicioServer1.ThreadSaida(socketSaida, portaDestino, portaEntrada, enderecoIp);
            tEntrada.start();
            tSaida.start();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    //fim do construtor

    public class Temporizador extends TimerTask {

        public void run() {
            try {
                semaforo.acquire();
                log.logServidor("Servidor: Tempo expirado!");
                proxNumSeq = base;  //reseta numero de sequencia
                semaforo.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //para iniciar ou parar o temporizador
    public void manipularTemporizador(boolean novoTimer) {
        if (timer != null) {
            timer.cancel();
        }
        if (novoTimer) {
            timer = new Timer();
            timer.schedule(new InicioServer1.Temporizador(), VALOR_TEMPORIZADOR);
        }
    }

    public class ThreadSaida extends Thread {

        private DatagramSocket socketSaida;
        private int portaDestino;
        private InetAddress enderecoIP;
        private int portaEntrada;

        //construtor
        public ThreadSaida(DatagramSocket socketSaida, int portaDestino, int portaEntrada, String enderecoIP) throws UnknownHostException {
            this.socketSaida = socketSaida;
            this.portaDestino = portaDestino;
            this.portaEntrada = portaEntrada;
            this.enderecoIP = InetAddress.getByName(enderecoIP);
        }

        //cria o pacote com numero de sequencia e os dados
        public byte[] gerarPacote(int numSeq, byte[] dadosByte) {
            byte[] numSeqByte = ByteBuffer.allocate(CABECALHO).putInt(numSeq).array();
            ByteBuffer bufferPacote = ByteBuffer.allocate(CABECALHO + dadosByte.length);
            bufferPacote.put(numSeqByte);
            bufferPacote.put(dadosByte);
            return bufferPacote.array();
        }

        public void run() {
            try {
                FileInputStream fis = new FileInputStream(new File(caminho));

                try {
                    while (!transferenciaCompleta) {    //envia pacotes se a janela nao estiver cheia
                        if (proxNumSeq < base + (TAMANHO_JANELA * TAMANHO_PACOTE)) {
                            semaforo.acquire();
                            if (base == proxNumSeq) {   //se for primeiro pacote da janela, inicia temporizador
                                manipularTemporizador(true);
                            }
                            byte[] enviaDados = new byte[CABECALHO];
                            boolean ultimoNumSeq = false;

                            if (proxNumSeq < listaPacotes.size()) {
                                enviaDados = listaPacotes.get(proxNumSeq);
                            } else {
                                byte[] dataBuffer = new byte[TAMANHO_PACOTE];
                                int tamanhoDados = fis.read(dataBuffer, 0, TAMANHO_PACOTE);
                                if (tamanhoDados == -1) {   //sem dados para enviar, envia pacote vazio
                                    ultimoNumSeq = true;
                                    enviaDados = gerarPacote(proxNumSeq, new byte[0]);
                                } else {    //ainda ha dados para enviar
                                    byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, tamanhoDados);
                                    enviaDados = gerarPacote(proxNumSeq, dataBytes);
                                }
                                listaPacotes.add(enviaDados);
                                log.logServidor("QTD:PACOTE*****************" + listaPacotes.size() + "*****************");
                            }
                            //enviando pacotes
                            socketSaida.send(new DatagramPacket(enviaDados, enviaDados.length, enderecoIP, portaDestino));
                            //sleep(20);
//                            log.logServidor("Cliente: Numero de sequencia enviado " + proxNumSeq);
                            inicio = new Timestamp(System.currentTimeMillis());
                            String date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS").format(inicio.getTime());
                            log.logServidor("Inicio Transação: " + date);

                            //atualiza numero de sequencia se nao estiver no fim
                            if (!ultimoNumSeq) {
                                proxNumSeq += TAMANHO_PACOTE;
                            }
                            semaforo.release();
                        }
                        sleep(20);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    manipularTemporizador(false);
                    socketSaida.close();
                    fis.close();
                    log.logServidor("Servidor: Socket de saida fechado!");
                    tempoFinal = new Timestamp(System.currentTimeMillis());
                    String date = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS").format(tempoFinal.getTime());
                    log.logCliente("Fim Transação: " + date);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public class ThreadEntrada extends Thread {

        private DatagramSocket socketEntrada;

        //construtor
        public ThreadEntrada(DatagramSocket socketEntrada) {
            this.socketEntrada = socketEntrada;
        }

        //retorna ACK
        int getnumAck(byte[] pacote) {
            byte[] numAckBytes = Arrays.copyOfRange(pacote, 0, CABECALHO);
            return ByteBuffer.wrap(numAckBytes).getInt();
        }

        public void run() {
            try {
                byte[] recebeDados = new byte[CABECALHO];  //pacote ACK sem dados
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
                try {
                    while (!transferenciaCompleta) {
                        socketEntrada.receive(recebePacote);
                        int numAck = getnumAck(recebeDados);
                        log.logServidor("Cliente: Ack recebido " + numAck);
                        //se for ACK duplicado
                        if (base == numAck + TAMANHO_PACOTE) {
                            semaforo.acquire();
                            manipularTemporizador(false);
                            proxNumSeq = base;
                            semaforo.release();
                        } else if (numAck == -2) {
                            transferenciaCompleta = true;
                        } //ACK normal
                        else {
                            base = numAck + TAMANHO_PACOTE;
                            semaforo.acquire();
                            if (base == proxNumSeq) {
                                manipularTemporizador(false);
                            } else {
                                manipularTemporizador(true);
                            }
                            semaforo.release();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    socketEntrada.close();
                    log.logServidor("Cliente: Socket de entrada fechado!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private class CheckboxClicked implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(localhostCheckBox.isSelected()){
                textIP.setText("127.0.0.1");
            }else{
                textIP.setText("");
            }
        }
    }

    private void imprimeStatus(String texto) {
        // Create an instance of javax.swing.JTextArea control
        JTextArea txtConsole = new JTextArea();

        // Now create a new TextAreaOutputStream to write to our JTextArea control and wrap a
        // PrintStream around it to support the println/printf methods.
        PrintStream out = new PrintStream(new TextAreaOutputStream(txtConsole));

        // redirect standard output stream to the TextAreaOutputStream
        System.setOut(out);

        // redirect standard error stream to the TextAreaOutputStream
        System.setErr(out);
        System.out.println(texto);
        EventQueue.invokeLater(() -> textAreaResult.setCaretPosition(textAreaResult.getText().length() - 1));
    }

    //acao do botao limpar
    private class ClearBtnClicked implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            textIP.setText("");
            textLocalMusica.setText("");
        }
    }

    //acao do botao baixar
    private class BaixarBtnClicked implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!verificarCampos()) {
                log.logServidor(textIP.getText() + "\n" + attributes.getDiretorioMusic()+"\\"+textLocalMusica.getText());
                log.logServidor("AGUARDE...");
                InicioServer1 server = new InicioServer1(PORTA_SERVIDOR, PORTA_ACK, attributes.getDiretorioMusic()+"\\"+textLocalMusica.getText(), textIP.getText());
                imprimeStatus(textAreaResult.getText());
                CamadaSimulacao.CalculaTempo(Long.valueOf(campoE.getText()), Long.valueOf(campoRTT.getText()), Long.valueOf(campoE.getText()));
            }
        }
    }

    private boolean verificarCampos() {
        boolean ret = false;
        String msg1 = "";
        String msg2 = "";
        if (textIP.getText() == null || textIP.getText().isEmpty()) {
            msg1 = "1 - Digite o IP destino.";
            ret = true;
        }
        if (textLocalMusica.getText() == null || textLocalMusica.getText().isEmpty()) {
            msg2 = "2 - Digite o nome da Música que será transferida.";
            ret = true;
        }

        if (ret) {
            JOptionPane.showMessageDialog(null, msg1 + "\n" + msg2, "Campos nulos ou em branco...", JOptionPane.ERROR_MESSAGE);
        }

        return ret;
    }

    public static void main(String[] args) {
        layoutNimbus();
        JFrame frame = new JFrame("InicioServer1");
        frame.setTitle("Tela do SERVIDOR - 1");
        ImageIcon imagemTituloJanela = new ImageIcon("br/com/front/servidor.png");
        frame.setIconImage(imagemTituloJanela.getImage());
        frame.setContentPane(new InicioServer1().jpanelServer1View);
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
