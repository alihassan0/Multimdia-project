

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client extends JFrame implements Runnable {

    private String hostName;
    private int portNumber;
    private int serverPortNumber;

    private Socket clientSocket;
    private PrintWriter clientOutputPrintWriter;
    private BufferedReader serverResponseReader;
    private BufferedReader clientStdin;
    private Thread serverListener, console;

    private BufferedReader inFromSocketReader;
    private PrintWriter outFromSocketPrinter;


    private int mediaServerPort = 9786;
    private int mediaClientPort = 8786;

    public static void main(String args[]) {
        new Client();
    }

    BufferedReader connectionInfoReader = new BufferedReader(
            new InputStreamReader(System.in));

    public Client() {
        initializeWindow();
        initialize();
    }

    public void initializeWindow() {

        final JButton call = new JButton("Call");

        call.setEnabled(true);

        call.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                call.setEnabled(false);
                call();
            }
        });
        getContentPane().add(call);

        getContentPane().setLayout(new FlowLayout());
        setTitle("Capture/Playback Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 100);
        getContentPane().setBackground(Color.white);
        setVisible(true);
    }

    public void initialize() {

        BufferedReader ConsoleReader = new BufferedReader(
                new InputStreamReader(System.in));
        serverListener = new Thread(this);//listens for call requests
        console = new Thread(this);

        serverListener.start();

    }

    //just a handy function to print stuff to the screen
    public static void infoBox(String infoMessage, String titleBar) {
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }

    public void call() {
        try {

            portNumber = 5050;
            System.out.println("Please Enter the hostname/IP address :");
            hostName = JOptionPane.showInputDialog("Please Enter the hostname/IP address :", "192.168.1.2");
            //hostName = connectionInfoReader.readLine();
            System.out.println("checking ....");
            //create a socket with the server
            clientSocket = new Socket(hostName, portNumber);
            System.out.println("he is online");

            clientOutputPrintWriter = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            serverResponseReader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
            clientStdin = new BufferedReader(new InputStreamReader(System.in));

            String username = JOptionPane.showInputDialog("Please Enter username :", "ali");
            clientOutputPrintWriter.println(username);
            clientOutputPrintWriter.println(clientSocket.getInetAddress().toString());
            clientOutputPrintWriter.println(serverPortNumber);


            int msgResponse = Integer.parseInt(serverResponseReader.readLine().trim());
            if (msgResponse == 1) {
                infoBox("he is welling to take your call", ":D");
                new VoipUser(mediaClientPort,mediaServerPort,hostName);
            } else if (msgResponse == 2) {
                infoBox("user busy", ":D");
            } else {
                System.out.println("._.");
            }

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

        if (Thread.currentThread() == serverListener) {
            try {
                serverPortNumber = 5050;
                ServerSocket welcomingSocket = null;
                try {
                    welcomingSocket = new ServerSocket(serverPortNumber);
                } catch (Exception e) {
                    infoBox("sorry .. please close all other instances of this program", "the port is already in use");
                    System.exit(0);
                }

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
                    int n = JOptionPane.showConfirmDialog(
                            this,
                            "user + [ " + username + " ] would like to call you  , Would you like to answer?",
                            "incoming call",
                            JOptionPane.YES_NO_OPTION);
                    int response = n == JOptionPane.YES_OPTION ? 1 : 2;
                    System.out.println(response);

                    outFromSocketPrinter.println(response);
                    if (response == 1) {
                        new VoipUser(mediaClientPort,mediaServerPort,otherEndSocket.getInetAddress().toString().substring(1));
                        System.out.println(otherEndSocket.getInetAddress().toString());
                        break;
                    } else {
                        System.out.println("not a correct code ");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                infoBox("listener error", "error");
            }
        }
        //----------------- console listener ----------------------------
        else if (Thread.currentThread() == console) {
            try{
                String msg;
                while (true) {
                    msg = connectionInfoReader.readLine().trim();
                    if (msg.equals("call")) {
                        call();
                        break;
                    } else if (msg.equals("bye")) {

                    } else {
                        System.out.println("not a known command");

                    }
                }
            }catch (IOException e){
                e.printStackTrace();
                infoBox("console error","error");

            }
        }

    }
}
