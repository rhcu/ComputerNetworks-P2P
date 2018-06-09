/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aidarbek Suleimenov and Assiya Khuzyakhmetova
 */
public class Server implements Runnable {
    private final String host = "127.0.0.1";
    private final int port;
    private final String path;
    private final LinkedList < String > files;
    public Server(int port, LinkedList < String > files, String path) {
        this.port = port;
        this.files = files;
        this.path = path;
    }
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            System.out.println("Server on client started successfully");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new PeerServerRunnable(socket, files, path)).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(FT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}