import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Slave {
	public static int master = 0;
	static boolean isComplete = false;

	public static void main(String args[]) throws Exception {
		SlaveThreading st = new SlaveThreading();
		Scanner in = new Scanner(System.in);
		System.out.println("Please enter the delay of this slave server in seconds.");
		int delay = in.nextInt();
		st.setSlaveTime(delay);
		long slave_time = Calendar.getInstance().getTimeInMillis() + delay * 1000;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(sdf.format(slave_time));
		try {
			st.ReceiveTimeRequest();
		} catch (Exception e) {
			System.out.println("receive time request from server failed");
		}
		in.close();
		Thread Synchronization = new Thread() {
			public void run() {
				try {
					st.SyncWithMaster();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Synchronization.start();

	}
}

class SlaveThreading {
	String msg = new String();
	// Intializing count
	Random rand = new Random();
	int adjust = 0;
	// delay used to calculate sys time
	long delay_milisec = 0;
	static final long ONE_SEC_IN_MILLI = 1000;
	// checker
	boolean isSynched = false;
	static boolean isComplete = false;
	// checker
	int ID_assigned = 0;
	int nodeCount = 0;

	public void ReceiveTimeRequest() throws Exception {
		InetAddress group = InetAddress.getByName("224.0.0.1");
		// TODO
		MulticastSocket recv_soc = new MulticastSocket(8011);
		MulticastSocket send_soc = new MulticastSocket(8010);
		recv_soc.joinGroup(group);
		byte[] buffer = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buffer, buffer.length);
		recv_soc.receive(recv);
		String message = new String(recv.getData(), 0, recv.getLength());
		// Send System Time back to server
		System.out.println("Here is your time request" + message);

		System.out.println("System time in miliseconds sent to Master");
		long sys_time = Calendar.getInstance().getTimeInMillis() + delay_milisec;
		msg = Long.toString(sys_time);
		DatagramPacket time_response = new DatagramPacket(msg.getBytes(), msg.length(), group, 8010);
		send_soc.send(time_response);

		recv_soc.close();
		send_soc.close();
	}

	public void SyncWithMaster() throws Exception {
		InetAddress group = InetAddress.getByName("224.0.0.1");
		// InetAddress group = InetAddress.getByName("");
		MulticastSocket recv_soc = new MulticastSocket(8011);
		byte[] buf = new byte[1000];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		recv_soc.joinGroup(group);
		while (!isComplete) {
			recv_soc.receive(recv);
			String message = new String(recv.getData(), 0, recv.getLength());
			String[] sync = message.split(":");
			if (sync.length == 4) {
				if (!isSynched) {
					System.out.println("New offset adjustment received from Master");
					System.out.println("Time synchronizing....");
					System.out.println("offset is " + sync[1]);
					// update delay milisec
					delay_milisec += Long.valueOf(sync[1]);
					System.out.println(delay_milisec);
					long udpated_sys_time_in_mili = Calendar.getInstance().getTimeInMillis() + delay_milisec;
					ID_assigned = Integer.valueOf(sync[2]);
					nodeCount = Integer.valueOf(sync[3]);
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
					System.out.println("ID assigned from Master: " + ID_assigned);
					System.out.println("New System Time: " + sdf.format(udpated_sys_time_in_mili));
					isSynched = true;
					System.out.println();
					if (ID_assigned == nodeCount) {
						isComplete = true;
					}
				} else {
					System.out.println("reach");
					System.out.println("New offset adjustment received from Master");
					System.out.println("Time synchronizing....");
					System.out.println("offset is " + sync[0]);
					// update delay milisec
					System.out.println("the delay now is" + delay_milisec);
					delay_milisec += Long.valueOf(sync[0]);
					System.out.println(delay_milisec);
					long udpated_sys_time_in_mili = Calendar.getInstance().getTimeInMillis() + delay_milisec;
					nodeCount = Integer.valueOf(sync[3]);
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
					System.out.println("New System Time: " + sdf.format(udpated_sys_time_in_mili));
					if (Integer.valueOf(sync[2]) == nodeCount) {
						isComplete = true;
					}
				}
			}

		}
		recv_soc.close();
	}

	public void setSlaveTime(int num) {
		delay_milisec = (long) num * ONE_SEC_IN_MILLI;
	}
}