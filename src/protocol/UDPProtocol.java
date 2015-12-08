package protocol;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by cuongpham on 11/25/15.
 * This class implements functions of the UDP protocol
 */
public class UDPProtocol {

    /**
     * This function will create the UDP connection
     * @param unicastSocket     the socket will be used for sending and receiving packets
     * @param port              the port a process will use for sending and receving packets
     */
    public static void connection(DatagramSocket unicastSocket, int port) {
        try {
            unicastSocket = new DatagramSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This function is used for sending a string from a process to another (unicast sending)
     * @param unicastSocket
     * @param message
     * @param inetAddress
     * @param port
     */
    public static void sendUnicastString(DatagramSocket unicastSocket, String message, InetAddress inetAddress, int port) {
        if (unicastSocket != null) {
            try {
                byte[] buf = new byte[256];
                buf = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, port);
                unicastSocket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * This function is used for receiving a packet from network
     * @param unicastSocket
     * @return the received packet
     */
    public static DatagramPacket receiveUnicast(DatagramSocket unicastSocket) {
        try {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            unicastSocket.receive(receivePacket);
            return receivePacket;
        } catch (Exception e) {
            //e.printStackTrace();
        	System.out.println("waiting");
        }
        return null;
    }

    /**
     * This function is used for sending an object from a process to another (unicast sending)
     * @param unicastSocket
     * @param obj
     * @param inetAddress
     * @param port
     */
    public static void sendUnicastObject(DatagramSocket unicastSocket, Object obj, InetAddress inetAddress, int port) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            byte[] sendData = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, inetAddress, port);
            unicastSocket.send(sendPacket);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This function is used for closing the UDP connection
     * @param unicastSocket
     */
    public static void close(DatagramSocket unicastSocket) {
        if (unicastSocket != null) {
            try {
                unicastSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
