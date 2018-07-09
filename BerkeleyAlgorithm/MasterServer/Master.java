import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Master {

	public static void main(String[] args) throws Exception {
		Scanner in = new Scanner(System.in);
		MasterThread daemon = new MasterThread();
		System.out.println("Please enter the number of the slave servers in the system.");
		int num = in.nextInt();
		System.out.println("Please enter the delay of the master server in seconds.");
		int delay = in.nextInt();
		daemon.setNodeCount(num);
		daemon.setDelaySec(delay);
		Thread Send = new Thread() {
			public void run() {
				try {
					daemon.ReceiveMsg();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		Thread Receive = new Thread() {
			public void run() {
				try {
					daemon.SendMsg();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Send.start();
		Receive.start();
	}
}

class MasterThread {

	String msg = new String();
	Random rand = new Random();
	int count = rand.nextInt(49) + 1;
	int adjust = 0;
	int slave_serv = 1;
	int offset = 0;
	int nodeCount = 0;
	long delay_sec = 0;
	static final long ONE_SEC_IN_MILLI = 1000;
	boolean syncComplete = false;

	// for receiving server time from the slave nodes and calculating the average
	public void ReceiveMsg() throws Exception {
		//give a multicast address
		InetAddress group = InetAddress.getByName("224.0.0.1");
		//provide multicastSocket TODO
		MulticastSocket recv_soc = new MulticastSocket(6788);
		MulticastSocket send_soc = new MulticastSocket(6789);
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		recv_soc.joinGroup(group);
		while (!syncComplete) {
			recv_soc.receive(recv);
			String message = new String(recv.getData(), 0, recv.getLength());
			int ID = slave_serv;
			if (slave_serv == nodeCount) {
				syncComplete = true;
			}
			System.out.println("New Slave Server Node registered");
			slave_serv++;
			int diff = Integer.valueOf(message);
			System.out.println("Offset of " + diff + " received. Calculating average....");
			adjust = diff / slave_serv;
			count = count + adjust;
			offset = adjust - diff;
			msg = adjust + ":" + offset + ":" + ID + ":" + nodeCount;
			System.out.println("Average calculated. New count: " + count);
			System.out.println("Sending slave servers the new offset....");
			System.out.println();
			Thread.sleep(1000);
			DatagramPacket sync = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
			send_soc.send(sync);

		}
	}

	public void SendMsg() throws Exception {
		Calendar time = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println("Server Clock: " + sdf.format(time.getTimeInMillis() + delay_sec * ONE_SEC_IN_MILLI));
		System.out.println(count);

		InetAddress group = InetAddress.getByName("224.0.0.1");
		//TODO
		MulticastSocket s_soc = new MulticastSocket(6789);
		while (!syncComplete) {
			msg = Integer.toString(count);
			DatagramPacket time_request = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
			s_soc.send(time_request);
			Thread.sleep(2000);
		}
	}

	public void setNodeCount(int num) throws Exception {
		nodeCount = num;
	}

	public void setDelaySec(int num){
		delay_sec = (long)num;
	}

}