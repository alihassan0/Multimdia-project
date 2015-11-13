import javax.sound.sampled.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class VoipUser {
    boolean stopaudioCapture = false;
    ByteArrayOutputStream byteOutputStream;
    AudioFormat adFormat;
    TargetDataLine targetDataLine;
    AudioInputStream InputStream;
    SourceDataLine sourceLine;
    int portNumber;
    int serverPortNumber;
    String ipAddress;
    public static void main(String args[]) {

    }

    public VoipUser(int portNumber,int serverPortNumber,String ipAddress ) {
        this.portNumber = portNumber;
        this.ipAddress = ipAddress;
        this.serverPortNumber = serverPortNumber;
        runVOIP();
        captureAudio();
    }

    public void captureAudio() {
        try {
            adFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open(adFormat);
            targetDataLine.start();

            Thread captureThread = new Thread(new CaptureThread());
            System.out.println("Capturing started");
            captureThread.start();
        } catch (Exception e) {
            StackTraceElement stackEle[] = e.getStackTrace();
            for (StackTraceElement val : stackEle) {
                System.out.println(val);
            }
            System.exit(0);
        }
    }
    public void runVOIP() {
        Thread receiveThread = new Thread(new RecieveThread());
        receiveThread.start();
    }
    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleInbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
    }

    class CaptureThread extends Thread {

        byte tempBuffer[] = new byte[10000];

        public void run() {

            byteOutputStream = new ByteArrayOutputStream();
            stopaudioCapture = false;
            try {
                DatagramSocket clientSocket = new DatagramSocket(portNumber);
                InetAddress IPAddress = InetAddress.getByName(ipAddress);
                while (!stopaudioCapture) {
                    int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                    if (cnt > 0) {
                        DatagramPacket sendPacket = new DatagramPacket(tempBuffer, tempBuffer.length, IPAddress,9786);
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
                //                sourceLine.drain();
                //             sourceLine.close();
            } catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    }
    class RecieveThread extends Thread {
        public void run() {
            try {
                DatagramSocket serverSocket = new DatagramSocket(serverPortNumber);
                System.out.println("server is running");
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
                        System.out.println("btngan");
                        System.exit(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}