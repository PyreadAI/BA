import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Master {

	public static void main(String[] args) throws Exception {
		Scanner in = new Scanner(System.in);
		MasterThreading daemon = new MasterThreading();
		System.out.println("Please enter the number of the slave servers in the system. (must be greater than zero)");
		int num = in.nextInt();
		System.out.println("Please enter the delay of the master server in seconds.");
		int delay = in.nextInt();
		daemon.setNodeCount(num);
		daemon.setDelaySec(delay);
		in.close();
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

class MasterThreading {

	String msg = new String();
	Random rand = new Random();
	int count = rand.nextInt(49) + 1;
	long adjust = 0;
	int slave_serv_num = 1;
	long offset = 0;
	int nodeCount = 0;
	long delay_milisec = 0;
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
			int ID = slave_serv_num;
			if (slave_serv_num == nodeCount) {
				syncComplete = true;
			}
			System.out.println("New Slave Server Node registered");
			slave_serv_num++;
			long slave_time = Long.valueOf(message);
			long master_time = Calendar.getInstance().getTimeInMillis()+ delay_milisec;
			long diff = slave_time - master_time;
			System.out.println("diff in sec: " + diff/1000);
			System.out.println("slave servers curr: " + slave_serv_num);
			System.out.println("System time from slave server received, Calculating diff and new adjustment...");
			// int diff = Integer.valueOf(message);
			// System.out.println("Offset of " + diff + " received. Calculating average....");

			adjust = diff / slave_serv_num;
			// count = count + adjust;
			offset = adjust - diff;
			System.out.println("adjust is: " + adjust);
			System.out.println("offset is: " + offset);
			msg = adjust + ":" + offset + ":" + ID + ":" + nodeCount;
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			delay_milisec += adjust;
			System.out.println("Average calculated. New master system time: " + sdf.format(Calendar.getInstance().getTimeInMillis()+ delay_milisec));
			System.out.println("Sending slave servers the new offset for adjustment....");
			System.out.println();
			Thread.sleep(1000);
			DatagramPacket sync = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
			send_soc.send(sync);

		}
		recv_soc.close();
		send_soc.close();
	}

	public void SendMsg() throws Exception {
		Calendar time = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println("Server Clock: " + sdf.format(time.getTimeInMillis() + delay_milisec));
		System.out.println(count);

		InetAddress group = InetAddress.getByName("224.0.0.1");
		//TODO
		MulticastSocket s_soc = new MulticastSocket(6789);
		while (!syncComplete) {
			// msg = Integer.toString(count);
			msg = "TIME_REQUEST";
			DatagramPacket time_request = new DatagramPacket(msg.getBytes(), msg.length(), group, 6789);
			s_soc.send(time_request);
			Thread.sleep(2000);
		}
		s_soc.close();
	}

	public void setNodeCount(int num) throws Exception {
		nodeCount = num;
	}

	public void setDelaySec(int num){
		delay_milisec = (long)num * ONE_SEC_IN_MILLI;
	}

}