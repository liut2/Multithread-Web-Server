
/*
 * Copyright (c) 1995, 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 
import java.text.DateFormat;
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class WebServer {
    public static void main(String[] args) throws IOException {
        
        if (args.length != 2) {
            System.err.println("Usage: java WebServer maxConnections RootDir");
            System.exit(1);
        }
        //declare the server socket
        ServerSocket serverSocket = null;
        
        //init the connection parameter
        int maxConnections = Integer.parseInt(args[0]);
        int curConnections = 0;
        Queue<WorkerThread> queue = new LinkedList<WorkerThread>();
        try{
            serverSocket = new ServerSocket(8888);
        }catch(Exception e){
            System.out.println("error in starting server socket" + e);
        }
        
        try{
            while (true){
                if (curConnections < maxConnections){
                    Socket clientSocket = serverSocket.accept();
                    WorkerThread thread = new WorkerThread(clientSocket, args[1]);
                    queue.offer(thread);
                    thread.start();
                    curConnections++;
                }else{
                    WorkerThread threadToDelete = queue.poll();
                    threadToDelete.clientSocket.close();
                    curConnections--;
                    System.out.println("we killed the thread");
                    Socket clientSocket = serverSocket.accept();
                    WorkerThread thread = new WorkerThread(clientSocket, args[1]);
                    queue.offer(thread);
                    thread.start();
                    curConnections++;
                }
                System.out.println("this is thread " + curConnections);
            }
        }catch (IOException e){
            Thread.currentThread().interrupt();//preserve the message
            return;
        }
        
        
        
    }
}
class BadRequestException extends Exception {

}
class WorkerThread extends Thread{
    public Socket clientSocket;
    private String path;
    public WorkerThread(Socket clientSocket, String path){
        this.clientSocket = clientSocket;
        this.path = path;
    }
    @Override
    public void run(){
        PrintWriter out = null;
        BufferedReader in = null;
        try{
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + 8888 + " or listening for a connection");
            System.out.println(e.getMessage());
        }
        try {
            //init the output string for html
            System.out.println("new thread!!!!!!!!!!!");
            String webpage = "";
            String line = "";
            int offset = 0;
            String input = "";
            //init the img hashset
            HashSet<String> imageSet = new HashSet<String>();
            String[] imageList = {"jpeg", "jpg", "jif", "jfif",     
    "tif", "tiff", "png", "bmp", "gif", "ico"};
            for (String image : imageList){
                imageSet.add(image);
            }
            //out = new PrintWriter(clientSocket.getOutputStream(), true);
            //in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String[] split = null;
            while (true){
                input = in.readLine();
                
                if(input != null){
                    split = input.split(" ");
                } else {
                    continue;
                }
                
                if (split.length != 3){
                    throw new BadRequestException();
                }
                String requestMethod = split[0];
                if (!requestMethod.equals("GET") && !requestMethod.equals("HEAD")){
                    throw new BadRequestException();
                }

                
                String[] temp = split[1].split("\\.");
                String type = temp[temp.length - 1];
                
                //if it's img type
                if (imageSet.contains(type)){
                    
                    OutputStream os = clientSocket.getOutputStream();
                    InputStream inputStream = Files.newInputStream(Paths.get(path + split[1]));
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[1024];
                    int totRead = 0;
                    int numRead = 0;
                    while ((numRead = inputStream.read(data,0,data.length)) != -1) {
                        totRead += numRead;
                        buffer.write(data,0,numRead);
                    }
                    if (requestMethod.equals("HEAD") || requestMethod.equals("GET")){
                        String[] response = {"HTTP/1.1 200 OK\r\n", "Date: " + new Date().toString(), "Content-Type: img/" + type +"\r\n", "Content-Length: " + totRead + "\r\n", "\r\n"}; // not even close to complete
                        for (int i = 0; i < response.length; i++){
                            for (Byte b : response[i].getBytes()) {
                                os.write(b);
                            }
                        }
                    }
                    
                    if (requestMethod.equals("GET")){
                        buffer.writeTo(os);
                    }
                    
                    
                }else{
                    String pathTail = "";
                    if(split[1].endsWith("/")) {
                        File test = new File(path + split[1] + "index.html");
                        File test1 = new File(path + split[1] + "index.htm");
                        File test2 = new File(path + split[1] + "index.php");

                        if(test.exists()) {
                            pathTail = split[1] + "index.html";
                        } else if(test1.exists()){
                            pathTail = split[1] + "index.htm";
                        } else if(test2.exists()) {
                            pathTail = split[1] + "index.php";
                        }
                    } else {
                        pathTail = split[1];
                    }
                    FileReader fileReader = new FileReader(path + pathTail);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    webpage = "";
                    offset = 0;
                    while ((line = bufferedReader.readLine()) != null){
                        webpage += line;
                        offset += line.getBytes().length; // 1 is for line separator
                    }
                    //close the bufferreader after reading.
                    bufferedReader.close();
                    //initial line
                    if (requestMethod.equals("GET") || requestMethod.equals("HEAD")){
                        out.println("HTTP/1.1 200 OK");
                        //headers\
                        out.println("Date: " + new Date().toString());
                        String[] pathTailSplit = pathTail.split("\\.");
                        out.println("Content-Type: text/" + pathTailSplit[pathTailSplit.length - 1]);
                        
                        out.println("Content-Length: " + offset);
                        //blank line
                        out.println("");
                    }
                    if (requestMethod.equals("GET")){
                        //main body
                        out.println(webpage);
                    }
                    
                }
                
                out.flush();
                //close the socket after file writing.
                //clientSocket.close();
                //catch the file not found exception
            }
            
        } catch(FileNotFoundException e) {
            //PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            //BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //initial line
            String fileNotFound = "<p>File not found</p>";
            out.println("HTTP/1.1 404 Not Found");
            //headers
            out.println("Date: " + new Date().toString());
            out.println("Content-Type: text/html");
            out.println("Content-Length: " + fileNotFound.getBytes().length);
            //blank line
            out.println("");
            //main body
            out.println(fileNotFound);
        } catch (SocketException e){
            System.out.println(e.getMessage());
        }catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + 8888 + " or listening for a connection");
            System.out.println(e.getMessage());
        } catch (BadRequestException e){
            String badRequest = "<p>Bad Request</p>";
            out.println("HTTP/1.1 400 Bad Request");
            //headers
            out.println("Date: " + new Date().toString());
            out.println("Content-Type: text/html");
            out.println("Content-Length: " + badRequest.getBytes().length);
            //blank line
            out.println("");
            //main body
            out.println(badRequest);
        }
        
    }
}