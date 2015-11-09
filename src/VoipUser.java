import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class VoipUser {

    ByteArrayOutputStream byteOutputStream;
    AudioFormat adFormat;
    TargetDataLine targetDataLine;
    AudioInputStream InputStream;
    SourceDataLine sourceLine;
    boolean stopaudioCapture = false;
    private Socket clientSocket;
    private PrintWriter clientOutputPrintWriter;
    private BufferedReader serverResponseReader;
    private ServerSocket welcomingSocket;
    private Socket serverSocket;
    private String name;

    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleInbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
    }

    public static void main(String args[]) {
        new VoipUser().initiate();
    }

    public void initiate() {
        System.out.println("hello and welcome to itasilat");
        System.out.println("to initiate a call press 1");
        System.out.println("to wait for a call press 2");
        BufferedReader modeSelector = new BufferedReader(
                new InputStreamReader(System.in));

        int mode = 0;
        try {
            mode = Integer.parseInt(modeSelector.readLine());
        } catch (Exception e) {
            System.out.println("please enter a correct number");
        }
        if (mode == 1) {
            initiateCall();
        } else if (mode == 2) {
            listenForConnection();
        }

    }

    public void initiateCall() {
        BufferedReader connectionInfoReader = new BufferedReader(
                new InputStreamReader(System.in));
        int portNumber = 0;
        String hostName = "";
        String userName = "";
        try {
            System.out.println("Please Enter the port number :");
            portNumber = Integer.parseInt(connectionInfoReader.readLine());
            System.out.println("Please Enter the hostname :");
            hostName = connectionInfoReader.readLine();

            System.out.print("Please Enter username :\n");
            userName = connectionInfoReader.readLine().trim();


            clientSocket = new Socket(hostName, portNumber);

            clientOutputPrintWriter = new PrintWriter(
                    clientSocket.getOutputStream(), true);
            serverResponseReader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));

            name = userName;
            clientOutputPrintWriter.println(name);


        } catch (NumberFormatException e) {
            System.err.print("Port Number invalid");
            e.printStackTrace();
            System.exit(1);
        } catch (UnknownHostException e) {
            System.err.printf("Can't get Host [%s]", hostName);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.printf("Couldn't get I/O for the connection to [%s]",
                    hostName);
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listenForConnection() {
        try {
            welcomingSocket = new ServerSocket(5050);
            while (true) {

                serverSocket = welcomingSocket.accept();

                System.out.println("someone is connected");
                BufferedReader inFromSocketReader = new BufferedReader(new InputStreamReader(
                        serverSocket.getInputStream()));
                String msg = inFromSocketReader.readLine();
                System.out.println(msg);
                /*ConnectedClient connectedClient = new ConnectedClient(
                        serverSocket);
                connectedClients.add(connectedClient);*/

            }
        } catch (Exception e) {
            System.out.println("an error occurred");
            e.printStackTrace();
        }

    }

    public void runVOIP() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(9786);
            byte[] receiveData = new byte[10000];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                System.out.println("RECEIVED: " + receivePacket.getAddress().getHostAddress() + " " + receivePacket.getPort());
                try {
                    byte audioData[] = receivePacket.getData();
                    InputStream byteInputStream = new ByteArrayInputStream(audioData);
                    AudioFormat adFormat = getAudioFormat();
                    InputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());
                    DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);
                    sourceLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                    sourceLine.open(adFormat);
                    sourceLine.start();
                    Thread playThread = new Thread(new PlayThread());
                    playThread.start();
                } catch (Exception e) {
                    System.out.println(e);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class PlayThread extends Thread {

        byte tempBuffer[] = new byte[10000];

        public void run() {
            try {
                int cnt;
                while ((cnt = InputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
                    if (cnt > 0) {
                        sourceLine.write(tempBuffer, 0, cnt);
                    }
                }
                //  sourceLine.drain();
                // sourceLine.close();
            } catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    }

    class CaptureThread extends Thread {

        byte tempBuffer[] = new byte[10000];

        public void run() {

            byteOutputStream = new ByteArrayOutputStream();
            stopaudioCapture = false;
            try {
                DatagramSocket clientSocket = new DatagramSocket(8786);
                InetAddress IPAddress = InetAddress.getByName("127.0.0.1");
                while (!stopaudioCapture) {
                    int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                    if (cnt > 0) {
                        DatagramPacket sendPacket = new DatagramPacket(tempBuffer, tempBuffer.length, IPAddress, 9786);
                        clientSocket.send(sendPacket);
                        byteOutputStream.write(tempBuffer, 0, cnt);
                    }
                }
                byteOutputStream.close();
            } catch (Exception e) {
                System.out.println("CaptureThread::run()" + e);
                System.exit(0);
            }
        }
    }
}