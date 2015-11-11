

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {

    private String hostName;
    private int portNumber;
    private int serverPortNumber;

    private Socket clientSocket;
    private PrintWriter clientOutputPrintWriter;
    private BufferedReader serverResponseReader;
    private BufferedReader clientStdin;
    private Thread clientReadingThread, clientWritingThread;
    private Thread serverListener, console;

    private BufferedReader inFromSocketReader;
    private PrintWriter outFromSocketPrinter;
    private BufferedReader ConsoleReader;

    private String lastMsg = "";
    private Boolean msgModified = false;
    private boolean waitingInput = false;

    public static void main(String args[]) {
        new Client();
    }

    BufferedReader connectionInfoReader = new BufferedReader(
            new InputStreamReader(System.in));

    public Client() {
        BufferedReader ConsoleReader = new BufferedReader(
                new InputStreamReader(System.in));
        try {

            clientReadingThread = new Thread(this);// reads what the client write
            clientWritingThread = new Thread(this);// writes what other client sends
            serverListener = new Thread(this);//listens for call requests
            console = new Thread(this);


            serverListener.start();

        } catch (NumberFormatException e) {
            System.err.print("Port Number invalid");
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Joining the server using my username
     *
     * @param name the client's <strong>Unique</strong> name
     */
    public void call() {
        try {

            System.out.println("Please Enter the port number :");
            portNumber = Integer.parseInt(connectionInfoReader.readLine());
            System.out.println("Please Enter the hostname/IP address :");
            hostName = connectionInfoReader.readLine();
            System.out.println("checking ....");
            //create a socket with the server
            clientSocket = new Socket(hostName, portNumber);
            System.out.println("he is online");

            clientOutputPrintWriter = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            serverResponseReader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
            clientStdin = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Please Enter username :\n");
            String username = connectionInfoReader.readLine().trim();
            clientOutputPrintWriter.println(username);
            clientOutputPrintWriter.println(clientSocket.getInetAddress().toString());
            clientOutputPrintWriter.println(serverPortNumber);


            int msgResponse = Integer.parseInt(serverResponseReader.readLine().trim());
            if (msgResponse == 1) {
                System.out.println("he is welling to take your call");
                new VUServer().runVOIP(9786);
                new VUClient(8786, 9786, "localhost").captureAudio();

            } else if (msgResponse == 2) {
                System.out.println("sorry .. he cancelled");
            } else {
                System.out.println("._.");
            }
            //clientReadingThread.start();
            //clientWritingThread.start();

        } catch (UnknownHostException e) {
            System.err.printf("Can't get Host [%s]", hostName);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.printf("Couldn't get I/O for the connection to [%s]",
                    hostName);
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            //-----------------clientReadingThread----------------------------
            if (Thread.currentThread() == clientReadingThread) {
                String messageFromServer;
                do {
                    messageFromServer = serverResponseReader.readLine();
                    System.out.println(messageFromServer);
                } while (!((messageFromServer.equals("BYE") || messageFromServer
                        .equals("QUIT"))));

            }
            //-----------------clientwritingThread----------------------------
            else if (Thread.currentThread() == clientWritingThread) {

                String messageToServer;
                do {
                    messageToServer = clientStdin.readLine();
                    System.out.println("msg is : " + messageToServer);
                    clientOutputPrintWriter.println(messageToServer);
                } while (!((messageToServer.equals("BYE") || messageToServer
                        .equals("QUIT"))));
            }
            //-----------------server listener ----------------------------
            else if (Thread.currentThread() == serverListener) {
                System.out.println("Please Enter the port number that you would like to listen too:");
                serverPortNumber = Integer.parseInt(connectionInfoReader.readLine());
                ServerSocket welcomingSocket = new ServerSocket(serverPortNumber);
                System.out.println("you are now listening to port [ " + serverPortNumber + " ]");
                System.out.println("if you want to call anyone just type call");
                console.start();
                while (true) {

                    Socket otherEndSocket = welcomingSocket.accept();
                    System.out.println("some User connected");
                    inFromSocketReader = new BufferedReader(new InputStreamReader(
                            otherEndSocket.getInputStream()));
                    outFromSocketPrinter = new PrintWriter(
                            otherEndSocket.getOutputStream(), true);

                    String username = inFromSocketReader.readLine();
                    String ip = inFromSocketReader.readLine();
                    portNumber = Integer.parseInt(inFromSocketReader.readLine());

                    System.out.println("user + [ " + username + " ] would like to call you ");

                    System.out.println("would you like to take this call");
                    System.out.println("press 1 for yes or 2 for no");
                    int response = 0;
                    waitingInput = true;
                    while (true) {
                        try {
                            if (msgModified) {
                                response = Integer.parseInt(lastMsg);
                                msgModified = false;
                                if (response == 1 || response == 2) {
                                    outFromSocketPrinter.println(response);
                                    new VUServer().runVOIP(7786);
                                    new VUClient(6786, 7786, "localhost").captureAudio();
                                    waitingInput = false;
                                    break;
                                } else {
                                    System.out.println("not a correct code ");
                                }
                            }
                        } catch (Exception e) {

                        }
                    }
                }

            }
            //----------------- commands listener ----------------------------
            else if (Thread.currentThread() == console) {
                String msg;
                while (true) {
                    msg = connectionInfoReader.readLine().trim();
                    if (msg.equals("call")) {
                        call();
                        break;
                    } else if (waitingInput) {
                        lastMsg = msg;
                        msgModified = true;
                    } else {
                        System.out.println("not a known command");

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            /*Utilities.cleanResources(clientOutputPrintWriter, clientSocket,
                    clientStdin, serverResponseReader);
            System.exit(0); // Terminating !!*/
        }
    }
}
