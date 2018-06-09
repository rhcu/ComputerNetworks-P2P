/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =======================   WARNING  ============================
 *      SINCE THERE ARE NO MUTEX LOCKS OR SEMAPHORES USED 
 *      IN THIS PROJECT, RACE CONDITION WILL PROBABLY OCCUR,
 *      SINCE WE HAVE A SHARED VARIABLE BETWEEN DIFFEREN THREADS
 * 
 * 
 * WorkerRunnable. Contains all the logic, associated with the thread 
 * @author aidarbek, assiya
 */
public class PeerServerRunnable implements Runnable {

    protected Socket clientSocket = null;
    protected String current_user = null;
    protected boolean greeted = false;
    protected HashMap < String, LinkedList < String > > map;
    protected LinkedList < String > peerHistory;
    protected int fileSharedNum = 0;
    protected String IP;
    protected String port;
    protected int score;
    protected int numOfRequests;
    protected int numOfUploads;
    private String path;
    private LinkedList < String > files;

    public PeerServerRunnable(Socket clientSocket, LinkedList < String > files, String path) {
        this.clientSocket = clientSocket;
        this.files = files;
        this.path = path;
    }

    @Override
    public void run() {
        try {
            InputStream input = clientSocket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            OutputStream output = clientSocket.getOutputStream();
            PrintWriter out = new PrintWriter(output, true);
            while (true) {
                String text = in .readLine();
                if (text == null)
                    continue;
                String result = null;
                String name = null;
                String type = null;
                try {
                    if (text.startsWith("DOWNLOAD: ")) {
                        text = text.replace("DOWNLOAD: ", "");
                        String[] info_file = text.split(",");
                        int p = ThreadLocalRandom.current().nextInt(1, 100 + 1);
                        if (p >= 50) {
                            result = "NO!";
                            out.println(result);
                        } else {
                            for (String f: this.files) {
                                if (f.contains(info_file[0]) && f.contains(info_file[1]) && f.contains(info_file[2])) {
                                    result = "FILE: " + f;
                                    f = f.replace("<", "");
                                    f = f.replace(">", "");
                                    String[] fileData = f.split(",");
                                    name = fileData[0];
                                    type = fileData[1].replace(" ", "");
                                }
                            }
                            if (result == null)
                                result = "NOT FOUND";
                        }
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    result = "Wrong type of query";
                }
                out.println(result);
                if (result.contains("FILE: ")) {
                    result = result.replace("FILE: <", "");
                    String[] info_file_to_send = result.split(", ");

                    File myFile = new File(this.path + info_file_to_send[0] + "." + info_file_to_send[1]);
                    byte[] mybytearray = new byte[(int) myFile.length()];
                    FileInputStream fis;
                    BufferedInputStream bis;
                    OutputStream os;

                    fis = new FileInputStream(myFile);
                    bis = new BufferedInputStream(fis);
                    bis.read(mybytearray, 0, mybytearray.length);
                    os = this.clientSocket.getOutputStream();
                    System.out.println("Sending " + text + "(" + mybytearray.length + " bytes)");
                    os.write(mybytearray, 0, mybytearray.length);
                    os.flush();
                }
            }
        } catch (IOException e) {
            //report exception somewhere.
            e.printStackTrace();
        }
        try {
            this.clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(WorkerRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}