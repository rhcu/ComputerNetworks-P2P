/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;




import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;


public class P2p_ui extends JFrame implements ActionListener {
    private JButton search; //Buttons
    private JButton dload;
    private JButton close;
    private JButton clear;
    private static int port;
    private static String path = "files/";
    private static String download_path = "downloads/";
    // FT data
    private static String ft_host = "10.3.21.7";
    private static int ft_port = 1488;
    // For FT
    private Socket echoSocket;
    private PrintWriter out;
    private BufferedReader in ;
    private BufferedReader stdIn;

    private static String host = "localhost";

    private JList jl; // List that will show found files
    private JLabel label; //Label "File Name
    private LinkedList < String > files;
    private JTextField tf, tf2; // Two textfields: one is for typing a file name, the other is just to show the selected file
    DefaultListModel listModel; // Used to select items in the list of found files

    public static LinkedList < String > listFiles() {
        File folder; // All files stored in this folder
        folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        LinkedList < String > result = new LinkedList < > ();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String filename = listOfFiles[i].getName();
                String extension = "", name = "";
                int j = filename.lastIndexOf('.');
                if (j > 0) {
                    extension = filename.substring(j + 1);
                    name = filename.substring(0, j);
                }
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                String file_format = "<" + name + ", " + extension + ", " +
                    listOfFiles[i].length() + ", " + sdf.format(listOfFiles[i].lastModified()) + ", " + host + ", " + port + ">";
                System.out.println("File " + file_format);
                result.add(file_format);
            }
        }

        return result;
    }
    public P2p_ui() throws IOException {

        super("Example GUI");
        try {
            InetAddress tmp = InetAddress.getLocalHost();
            host = (String) tmp.getHostAddress();
            System.out.println("IP of my system is := " + host);
        } catch (UnknownHostException ex) {
            Logger.getLogger(FT.class.getName()).log(Level.SEVERE, null, ex);
        }


        System.out.println("Enter port: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Scanner s = new Scanner(System.in);
        int tmp_port = Integer.parseInt(br.readLine());
        this.port = tmp_port;

        System.out.println("Enter filepath to files to share: ");
        path = br.readLine();
        System.out.println(path);
        
        System.out.println("Enter file tracker IP: ");
        ft_host = br.readLine();
        System.out.println(ft_host);

        this.files = listFiles();

        try {
            echoSocket = new Socket(this.ft_host, this.ft_port);
            out =
                new PrintWriter(echoSocket.getOutputStream(), true); in =
            new BufferedReader(
                new InputStreamReader(echoSocket.getInputStream()));
            stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in));
        } catch (Exception ex) {
            System.out.println(ex);
            return;
        }
        out.println("HELLO");
        System.out.println("FT server returned: " + in .readLine());

        for (String record: this.files) {
            out.println(record);
            System.out.println( in .readLine());
        }

        Server srv = new Server(port, files, path);
        Thread th = new Thread(srv);
        th.start();


        setLayout(null);
        setSize(500, 600);

        label = new JLabel("File name:");
        label.setBounds(50, 50, 80, 20);
        add(label);

        tf = new JTextField();
        tf.setBounds(130, 50, 220, 20);
        add(tf);

        search = new JButton("Search");
        search.setBounds(360, 50, 80, 20);
        search.addActionListener(this);
        add(search);

        listModel = new DefaultListModel();
        jl = new JList(listModel);

        JScrollPane listScroller = new JScrollPane(jl);
        listScroller.setBounds(50, 80, 300, 300);

        add(listScroller);

        dload = new JButton("Download");
        dload.setBounds(200, 400, 130, 20);
        dload.addActionListener(this);
        add(dload);

        tf2 = new JTextField();
        tf2.setBounds(200, 430, 250, 20);
        add(tf2);

        close = new JButton("Close");
        close.setBounds(360, 470, 80, 20);
        close.addActionListener(this);
        add(close);

        clear = new JButton("Clear");
        clear.setBounds(360, 80, 80, 20);
        clear.addActionListener(this);
        add(clear);

        setVisible(true);
    }
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == search) {
            String fileName = tf.getText();
            if ("".equals(fileName)) {
                listModel.addElement("Empty filename. Try again.");
            } else {
                listModel.removeAllElements();
                out.println("SEARCH: " + fileName);
                int i = 0;

                try {
                    String fileInfo = "";
                    while ((fileInfo = in .readLine()) != null) {
                        System.out.println(fileInfo);
                        if (fileInfo.length() == 0) break;
                        if (fileInfo.contains("FOUND")) {
                            if (fileInfo.startsWith("NOT")) {
                                fileInfo = "";
                                break;
                            }
                        } else {
                            if (fileInfo.length() <= 1) {
                                break;
                            }
                            fileInfo = fileInfo.replace("<", "");
                            fileInfo = fileName + "," + fileInfo;
                            fileInfo = "<" + fileInfo;
                            listModel.insertElementAt(fileInfo, i);
                            i++;
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(P2p_ui.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (e.getSource() == dload) { //If download button is pressed get the selected value from the list and show it in text field
            String fileToDownload = jl.getSelectedValue().toString();
            if (fileToDownload.equals("Empty filename. Try again.")) {
                tf2.setText("You haven't specified the right filename");
            } else {
                System.out.println("User wants to download: " + fileToDownload);

                fileToDownload = fileToDownload.replace("<", "");
                fileToDownload = fileToDownload.replace(">", "");
                String[] fileData = fileToDownload.split(",");

                String name = fileData[0];
                String type = fileData[1];
                String size = fileData[2];
                String last_mod_data = fileData[3];
                String IP = fileData[4];
                String port = fileData[5];

                try {
                    Socket p_echoSocket = new Socket(IP.replace(" ", ""), Integer.parseInt(port.replace(" ", "")));
                    PrintWriter p_out =
                        new PrintWriter(p_echoSocket.getOutputStream(), true);
                    BufferedReader p_in =
                        new BufferedReader(
                            new InputStreamReader(p_echoSocket.getInputStream()));
                    p_out.println("DOWNLOAD: " + name + "," + type + "," + size);
                    String result = p_in.readLine();
                    if (result.startsWith("NO!")) {
                        tf2.setText(fileToDownload + " couldn't download");
                        this.out.println("SCORE of " + IP.replace(" ", "") + ":" + port.replace(" ", "") + " : 0");
                    } else {
                        tf2.setText(fileToDownload + " donwloaded");
                        this.out.println("SCORE of " + IP.replace(" ", "") + ":" + port.replace(" ", "") + " : 1");
                        // From http://www.rgagnon.com/javadetails/java-0542.html
                        int bytesRead;
                        int current = 0;
                        FileOutputStream fos = null;
                        BufferedOutputStream bos = null;
                        int fileSize = Integer.parseInt(size.replace(" ", ""));
                        byte[] mybytearray = new byte[fileSize];
                        InputStream is = p_echoSocket.getInputStream();
                        fos = new FileOutputStream(download_path + name + "." + type.replace(" ", ""));
                        bos = new BufferedOutputStream(fos);
                        bytesRead = is.read(mybytearray, 0, mybytearray.length);
                        current = bytesRead;
                        do {
                            bytesRead = is.read(mybytearray, current, (mybytearray.length - current));
                            if (bytesRead >= 0) current += bytesRead;
                        } while (current < fileSize);

                        bos.write(mybytearray, 0, current);
                        bos.flush();

                        System.out.println(result);
                    }
                    p_echoSocket.close();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        } else if (e.getSource() == close) { //If close button is pressed exit
            out.println("BYE");
            System.exit(0);
        } else if (e.getSource() == clear) {
            listModel.removeAllElements();
            tf2.setText("");
            tf.setText("");
        }

    }
    public static void main(String[] args) {
        try {
            P2p_ui ex = new P2p_ui();
            ex.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close the window if x button is pressed
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}