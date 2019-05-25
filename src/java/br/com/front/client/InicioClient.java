package br.com.front.client;

import br.com.backEnd.Servidor;
import br.com.entity.Attributes;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InicioClient {

    static final int CABECALHO = 4;
    static final int TAMANHO_PACOTE = 324 + CABECALHO;
    static final int PORTA_SERVIDOR = 8002;
    static final int PORTA_ACK = 8003;

    private JPanel jpanelClientView;
    private JTextField textLocalMusica;
    private JTextField textNomeMusica;
    private JRadioButton sequencialRadioButton;
    private JRadioButton aleatorioRadioButton;
    private JTextArea textAreaResult;
    private JButton btnLimpar;
    private JButton btnBaixar;
    private static Attributes attributes = new Attributes();
    
    private List<String> msgConsole = new ArrayList<>();

    public InicioClient() {
        //textfild
        attributes.setDiretorioMusic(textLocalMusica.getText());
        attributes.setNomeMusic(textNomeMusica.getText());
        //textarea
        msgConsole.add(textAreaResult.getText());
        //button
        btnLimpar.addActionListener(new ClearBtnClicked());
        btnBaixar.addActionListener(new BaixarBtnClicked());
    }

    //construtor
    public InicioClient(int portaEntrada, int portaDestino, String caminho) {
        DatagramSocket socketEntrada, socketSaida;
        msgConsole.add("Servidor: porta de entrada: " + portaEntrada + ", " + "porta de destino: " + portaDestino + ".");

        int ultimoNumSeq = -1;
        int proxNumSeq = 0;  //proximo numero de sequencia
        boolean transferenciaCompleta = false;  //flag caso a transferencia nao for completa

        //criando sockets
        try {
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();
            msgConsole.add("Servidor Conectado...");
            textAreaResult.setText(msgConsole.toString());
            try {
                byte[] recebeDados = new byte[TAMANHO_PACOTE];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);

                FileOutputStream fos = null;

                while (!transferenciaCompleta) {
                    int i = 0;
                    socketEntrada.receive(recebePacote);
                    InetAddress enderecoIP = recebePacote.getAddress();

                    int numSeq = ByteBuffer.wrap(Arrays.copyOfRange(recebeDados, 0, CABECALHO)).getInt();
                    msgConsole.add("Servidor: Numero de sequencia recebido " + numSeq);

                    //se o pacote for recebido em ordem
                    if (numSeq == proxNumSeq) {
                        //se for ultimo pacote (sem dados), enviar ack de encerramento
                        if (recebePacote.getLength() == CABECALHO) {
                            byte[] pacoteAck = gerarPacote(-2);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                            transferenciaCompleta = true;
                            msgConsole.add("Servidor: Todos pacotes foram recebidos! Arquivo criado!");
                        } else {
                            proxNumSeq = numSeq + TAMANHO_PACOTE - CABECALHO;  //atualiza proximo numero de sequencia
                            byte[] pacoteAck = gerarPacote(proxNumSeq);
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                            msgConsole.add("Servidor: Ack enviado " + proxNumSeq);
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
                    } else {    //se pacote estiver fora de ordem, mandar duplicado
                        byte[] pacoteAck = gerarPacote(ultimoNumSeq);
                        socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                        msgConsole.add("Servidor: Ack duplicado enviado " + ultimoNumSeq);
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
                msgConsole.add("Servidor: Socket de entrada fechado!");
                msgConsole.add("Servidor: Socket de saida fechado!");
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        textAreaResult.setText(msgConsole.toString()+"\n");
        System.out.println(msgConsole.toString());
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
                msgConsole.add(textLocalMusica.getText() + "\n" + textNomeMusica.getText()
                        + "\n" + "Sequêncial " + sequencialRadioButton.isSelected() + "\n" + "Aleatório " + aleatorioRadioButton.isSelected());

                InicioClient client = new InicioClient(PORTA_SERVIDOR, PORTA_ACK, textLocalMusica.getText() + textNomeMusica.getText());
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
            msg3 = "3 - Selecione o rádio button como 'Sequêncial' ou 'Aleatório'.";
            ret = true;
        }

        if (ret) {
            JOptionPane.showMessageDialog(null, msg1 + "\n" + msg2 + "\n" + msg3, "Campos nulos ou em branco...", JOptionPane.ERROR_MESSAGE);
        }

        return ret;
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
