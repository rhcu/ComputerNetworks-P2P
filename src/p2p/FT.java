/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aidarbek Suleimenov & Assiya Khuzyakhmetova
 */
public class FT {

    public static void main(String[] args) {
        int portNumber = 1488;
        HashMap < String, LinkedList < String >> map = new HashMap < > ();
        LinkedList < String > peerHistory = new LinkedList < > ();
        HashMap < String, Integer > scoresMap = new HashMap < > ();
        HashMap < String, Integer > numReq = new HashMap < > ();
        InetAddress IP;
        try {
            IP = InetAddress.getLocalHost();

            System.out.println("IP of my system is := " + IP.getHostAddress());

        } catch (UnknownHostException ex) {
            Logger.getLogger(FT.class.getName()).log(Level.SEVERE, null, ex);
        }
        // Hardcoded list of existing groups
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Server started successfully");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new WorkerRunnable(socket, map, peerHistory, scoresMap, numReq)).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(FT.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}