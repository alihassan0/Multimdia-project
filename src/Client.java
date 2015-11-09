

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {

    private String hostName;
    private int portNumber;
    private Socket clientSocket;
    private PrintWriter clientOutputPrintWriter;
    private BufferedReader serverResponseReader;
    private BufferedReader clientStdin;
    private Thread clientReadingThread, clientWritingThread;
    private String name;

    public static void main(String args[]) {
        new Client();
    }

    public Client() {
        BufferedReader connectionInfoReader = new BufferedReader(
                new InputStreamReader(System.in));
        try {
            System.out.println("Please Enter the port number :");
            portNumber = Integer.parseInt(connectionInfoReader.readLine());
            System.out.println("Please Enter the hostname :");
            hostName = connectionInfoReader.readLine();

            // The clientReadingThread is reading the
            // server's response message
            clientReadingThread = new Thread(this);
            clientWritingThread = new Thread(this);
            // The clientWritingThread is writing the
            // client's stding to the server
            clientWritingThread = new Thread(this);

            System.out.print("Please Enter username :\n");
            String username = connectionInfoReader.readLine().trim();
            this.join(username);

        } catch (NumberFormatException e) {
            System.err.print("Port Number invalid");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Joining the server using my username
     *
     * @param name
     *            the client's <strong>Unique</strong> name
     */
    public void join(String name) {
        try {

            clientSocket = new Socket(hostName, portNumber);

            clientOutputPrintWriter = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            serverResponseReader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
            clientStdin = new BufferedReader(new InputStreamReader(System.in));

            this.name = name;
            clientOutputPrintWriter.println(name);

            clientReadingThread.start();
            clientWritingThread.start();

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
            if (Thread.currentThread() == clientReadingThread) {
                String messageFromServer;
                do {
                    messageFromServer = serverResponseReader.readLine();
                    System.out.println(messageFromServer);
                } while (!((messageFromServer.equals("BYE") || messageFromServer
                        .equals("QUIT"))));

            } else {

                String messageToServer;
                do {
                    messageToServer = clientStdin.readLine();
                    clientOutputPrintWriter.println(messageToServer);
                } while (!((messageToServer.equals("BYE") || messageToServer
                        .equals("QUIT"))));
            }
        } catch (Exception ignored) {
        } finally {
            /*Utilities.cleanResources(clientOutputPrintWriter, clientSocket,
                    clientStdin, serverResponseReader);
            System.exit(0); // Terminating !!*/
        }
    }
}
