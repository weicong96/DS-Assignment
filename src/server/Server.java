package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import shared.Constants;
import shared.Utils;

public class Server {
	private DatagramSocket socket;
	private byte[] buffer;
	private boolean running = true;
	private Facility[] facilities;
	private char padString = '_';
	
	private HashMap<String, HashMap<String, Object>> histories = new HashMap<String, HashMap<String, Object>>();
	
	public Server(int port) {
		try {
			socket = new DatagramSocket(port);
			buffer = new byte[Constants.BUFFER_SIZE];
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.facilities = new Facility[] {
				new Facility("TR+78"),
				new Facility("TR+18"),
				new Facility("LT29"),
				new Facility("LT28"),
				new Facility("LHS TR+08")				
		};
	} 
	
	public void sendReply(DatagramSocket socket, HashMap<String, Object> reply, String identifier, DatagramPacket request) {
		reply.put("message_type", ((byte)1));
		reply.put("fields_length", ((byte)(reply.keySet().size() - 3)));
		
		byte[] replyBuffer = Utils.marshallPayload(reply);
        DatagramPacket packet = new DatagramPacket(replyBuffer, replyBuffer.length, request.getAddress(), request.getPort());
        try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start() {
		while(running) {
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
			try {
				socket.receive(packet);
				
				HashMap<String, Object> requestPayload = Utils.unmarshallPayload(this.buffer);
				String uniqueID =  packet.getAddress().getHostAddress()+"_"+((int)requestPayload.get("request_id"));
				
				byte serviceType = (byte)requestPayload.get("service_type");
				
				HashMap<String, Object> reply = new HashMap<String, Object>();
				reply.put("service_type", (byte) serviceType);
				reply.put("reply_id", (Integer) requestPayload.get("request_id"));
				String facilityName;
				int facilityIndex = -1;
				switch(serviceType) {
					case Constants.QUERY_FACILITY:
						facilityName = (String) requestPayload.get("facility_name");
						facilityIndex = -1;
						for(int i = 0; i < this.facilities.length; i++) {
							if(this.facilities[i].getName().equals(facilityName)) {
								facilityIndex = i;
								break;
							}
						}
						if(facilityIndex == -1) {
							//indicate success is false
							reply.put("success", ((byte)0));
							reply.put("error_message", "Error: Facility with the name "+requestPayload.get("facility_name")+" not found");
							break;
						}else {
							reply.put("success", ((byte) 1));
						}
						
						byte[] days = (byte[])requestPayload.get("facility_days");
						
						for(int i = 0; i < this.facilities.length; i++) {
							reply.put("timeslots_avaliable_"+i, facilities[facilityIndex].getBookingIntervals((byte)i));
						}
						
						break;
					case Constants.BOOK_FACILITY:
						facilityName = (String) requestPayload.get("facility_name");
						String startTime = (String) requestPayload.get("start_time");
						String endTime = (String) requestPayload.get("end_time");
						byte day = (byte) requestPayload.get("day");
						
						facilityIndex = -1;
						for(int i = 0; i < this.facilities.length;i++) {
							if(this.facilities[i].getName().equals(facilityName)) {
								facilityIndex = i;
								break;
							}
						}
						Booking booking = new Booking(startTime, endTime, day);
						boolean conflictBooking = this.facilities[facilityIndex].hasSameBooking(booking);
						if(conflictBooking)
							System.out.println("duplicated booking");
						this.facilities[facilityIndex].addBooking(booking);
						System.out.println(this.facilities[facilityIndex].getBookings().size());
						break;
				}
				this.sendReply(socket, reply, uniqueID, packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
