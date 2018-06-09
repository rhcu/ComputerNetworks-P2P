/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
public class WorkerRunnable implements Runnable {

    protected Socket clientSocket = null;
    protected String current_user = null;
    protected boolean greeted = false;
    protected HashMap < String, LinkedList < String > > map;
    protected HashMap < String, Integer > scoresMap;
    protected LinkedList < String > peerHistory;
    protected int fileSharedNum = 0;
    protected String IP;
    protected String port;
    protected int numOfRequests;
    protected int numOfUploads;
    protected HashMap < String, Integer > numReq;

    public WorkerRunnable(Socket clientSocket, HashMap < String, LinkedList < String > > map, LinkedList < String > peerHistory, HashMap < String, Integer > scoresMap,HashMap < String, Integer > numReq) {
        this.clientSocket = clientSocket;
        this.map = map;
        this.peerHistory = peerHistory;
        this.numOfRequests = 0;
        this.numOfUploads = 0;
        this.scoresMap = scoresMap;
        this.numReq = numReq;
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

                String result;
                try {
                    result = processString(text);
                } catch (StringIndexOutOfBoundsException e) {
                    result = "Wrong type of query. Please, follow protocol rules.";
                }
                out.println(result);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        try {
            this.clientSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(WorkerRunnable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private String processString(String query) {
        //return null;
        if (query.startsWith("HELLO") && this.greeted == false) {
            this.greeted = true;
            return "HI";
        }
        if (this.greeted && query.startsWith("<")) {
            if (this.fileSharedNum >= 5) {
                return "Limit of files exceeded. Only 5 files are allowed.";
            }
            int comma = 0;
            for (int i = 0; i < query.length(); i++) {
                if (query.charAt(i) == ',') {
                    comma++;
                }
            }
            System.out.println(query);
            String incorrect_usage_message = "INCORRECT FORMAT OF DATA. Usage: <filename, file type, size, last modified date, IP, port>";
            if (comma != 5) {
                return incorrect_usage_message;
            }
            try {
                addFile(query);
            } catch (Exception ex) {
                System.out.println(ex);
                return incorrect_usage_message;
            }
            return "Accepted file #" + this.fileSharedNum;
        }
        if (this.greeted && query.startsWith("SEARCH") && this.fileSharedNum > 0) {
            String filename = query.replace("SEARCH: ", "");
            if (map.containsKey(filename)) {
                LinkedList < String > results = map.get(filename);
                if (results.size() == 0)
                    return "NOT FOUND\n";
                String result = "FOUND:\n";
                for (String record: results) {
                    System.out.println(this.getAddress(record));
                    if (scoresMap.containsKey(this.getAddress(record)) && numReq.containsKey(this.getAddress(record)))
                        result += record.replace(">", ", %" + (int)(100*((float)this.scoresMap.get(this.getAddress(record))/(float)this.numReq.get(this.getAddress(record)))) + " >") + "\n";
                    else
                        result += record.replace(">", ", 0 >") + "\n";
                    
                }
                return result;
            } else {
                return "NOT FOUND\n";
            }
        }
        if (this.greeted && query.startsWith("SCORE") && this.fileSharedNum > 0) {
            query = query.replace("SCORE of ", "");
            String arr[] = query.split(":");

            String key = arr[0].replace(" ", "") + ":" + arr[1].replace(" ", "");

            int score = Integer.parseInt(arr[2].replace(" ", ""));
            
            this.numOfRequests += 1;
            this.numOfUploads += score;

            if (!this.scoresMap.containsKey(key)) {
                this.scoresMap.put(key, score);
            } else {
                this.scoresMap.put(key, this.scoresMap.get(key) + score);
            }
            if (!this.numReq.containsKey(key)) {
                this.numReq.put(key, 1);
            } else {
                this.numReq.put(key, this.numReq.get(key) + 1);
            }
        }
        
        if (this.greeted && query.startsWith("BYE") && this.fileSharedNum > 0) {
            this.peerHistory.add("<" + IP + "," + port + "," + this.numOfUploads + ">");
            // show scores and peerHistory in log
            System.out.println("PEER: " + "<" + IP + "," + port + ", Score: " + this.numOfUploads + ", Number of Requests: " + numOfRequests + ">");
            
            Iterator it = this.map.entrySet().iterator();
            while (it.hasNext()) {
                HashMap.Entry pair = (HashMap.Entry) it.next();
                LinkedList < String > files = (LinkedList < String > ) pair.getValue();
                for (String file: files) {
                    if (file.contains(IP) && file.contains(port)) {
                        files.remove(file);
                    }
                }
            }
            try {
                this.clientSocket.close();
            } catch (IOException ex) {
                Logger.getLogger(WorkerRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "";
    }
    
    private void addFile(String file) {
        this.fileSharedNum++;
        file = file.replace("<", "");
        file = file.replace(">", "");
        String[] fileData = file.split(",");
        String name = fileData[0];
        String type = fileData[1];
        String size = fileData[2];
        String last_mod_data = fileData[3];
        String IP = fileData[4];
        String port = fileData[5];
        this.IP = IP;
        this.port = port;
        String key = this.IP.replace(" ", "") + ":" + this.port.replace(" ", "");
        System.out.println(key);
        this.scoresMap.put(key, 0);

        String tableData = "<" + type + "," + size + "," + last_mod_data + "," + IP + "," + port + ">";
        if (map.containsKey(name)) {
            map.get(name).add(tableData);
        } else {
            LinkedList < String > files = new LinkedList < > ();
            files.add(tableData);
            map.put(name, files);
        }
    }
    private String getAddress(String record) {
        record = record.replace("<", "");
        record = record.replace(">", "");
        String[] fileData = record.split(",");

        String type = fileData[0];
        String size = fileData[1];
        String last_mod_data = fileData[2];
        String addr = fileData[3].replace(" ", "") + ":" + fileData[4].replace(" ", "");
        record = "<" + type + "," + size + "," + last_mod_data + "," + fileData[3] + "," + fileData[4] + ">";
        return addr;
    }

}