import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;

public class VUClient{

    boolean stopaudioCapture = false;
    ByteArrayOutputStream byteOutputStream;
    AudioFormat adFormat;
    TargetDataLine targetDataLine;
    AudioInputStream InputStream;
    SourceDataLine sourceLine;
    Graphics g;
    int portNumber;
    int serverPortNumber;
    String ipAddress;
    public static void main(String args[]) {
        new VUClient(8786,9786,"localhost");
    }

    public VUClient(int portNumber,int serverPortNumber,String ipAddress ) {
        this.portNumber = portNumber;
        this.ipAddress = ipAddress;
        this.serverPortNumber = serverPortNumber;
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

    private void playAudio() {
        try {
            byte audioData[] = byteOutputStream.toByteArray();
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
}