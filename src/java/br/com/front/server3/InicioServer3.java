package br.com.front.server3;

import br.com.entity.Attributes;
import br.com.front.server1.InicioServer1;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class InicioServer3 {
    private JPanel jpanelServer3View;
    private JTextField textIP;
    private JTextField textLocalMusica;
    private JTextArea textAreaResult;
    private JButton btnLimpar;
    private JButton btnBaixar;
    private Attributes attributes = new Attributes();

    public InicioServer3() {
        //textfild
        File file = new File("");

        attributes.setDiretorioMusic(file.getAbsolutePath() + textIP.getText());
        attributes.setNomeMusic(textLocalMusica.getText());

        System.out.println(attributes.getDiretorioMusic());
        //textarea
        attributes.setConsole(textAreaResult.getText());
        //button
        btnLimpar.addActionListener(new InicioServer3.ClearBtnClicked());
        btnBaixar.addActionListener(new InicioServer3.BaixarBtnClicked());
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
                textAreaResult.setText(textIP.getText() + "\n" + attributes.getDiretorioMusic() + "\\" + textLocalMusica.getText());
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
        JFrame frame = new JFrame("InicioServer3");
        frame.setTitle("Tela do SERVIDOR - 3");
        ImageIcon imagemTituloJanela = new ImageIcon("br/com/front/servidor.png");
        frame.setIconImage(imagemTituloJanela.getImage());
        frame.setContentPane(new InicioServer3().jpanelServer3View);
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
