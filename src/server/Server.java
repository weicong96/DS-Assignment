package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import shared.Constants;
import shared.Constants.INVOCATION_TYPE;
import shared.Utils;

public class Server {
	private DatagramSocket socket;
	private byte[] buffer;
	private INVOCATION_TYPE invocationType;
	private boolean running = true;
	private Facility[] facilities;
	private int numberOfSent = 0;
	private HashMap<String, HashMap<String, Object>> histories = new HashMap<String, HashMap<String, Object>>();
	//facility name indexed by list of monitors
	private HashMap<String, ArrayList<Monitor>> monitors = new HashMap<String, ArrayList<Monitor>>();
	public Server(int port, INVOCATION_TYPE type) {
		try {
			socket = new DatagramSocket(port);
			buffer = new byte[Constants.BUFFER_SIZE];
			this.invocationType = type;
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
        	histories.put(identifier, reply);
        	if(numberOfSent % 3 == 0) {
    			socket.send(packet);
        	}
        	this.numberOfSent ++;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean sendReplyEarly(String identifier, DatagramPacket request) {
		if(this.histories.containsKey(identifier)) {
			HashMap<String, Object> reply = this.histories.get(identifier);
			this.sendReply(socket, reply, identifier, request);
			System.out.println("early sending");
			return true;
		}
		return false;
	}
	public void informAllMonitors(Facility facility) {
		//check and see if its expired, if yes remove it
		for(Entry<String, ArrayList<Monitor>> entry : this.monitors.entrySet()) {
		    String key = entry.getKey();
		    ArrayList<Monitor> value = entry.getValue();
		    
		    for(int i = 0; i < value.size(); i++) {
		    	if(value.get(i).getExpiry() <= Calendar.getInstance().getTimeInMillis()) {
		    		value.remove(i);
		    	}
		    }
		}
		
		ArrayList<Monitor> monitors = this.monitors.getOrDefault(facility.getName(), new ArrayList<Monitor>());
		for(int i = 0 ; i < monitors.size(); i++) {
			String address = monitors.get(i).getClientAddress();
			int port = monitors.get(i).getClientPort();
			System.out.println("Notifying : "+ address+ " port: "+port);
			HashMap<String, Object> reply = new HashMap<String, Object>();
			reply.put("service_type", Constants.MONITOR_AVALIABILITY);
			String uniqueID = address+":"+port+"_"+monitors.get(i).getRequestId();
					
			reply.put("reply_id", monitors.get(i).getRequestId());
			
			int facilityIndex = this.getFacilityIndexByName(facility.getName());
			for(byte j = 0; j < 7; j++) {
				reply.put("timeslots_avaliable_"+j, facilities[facilityIndex].getBookingIntervals(j));
			}		
			
			byte[] buffer = new byte[Constants.BUFFER_SIZE];
			
			DatagramPacket packet;
			try {
				packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(address), port);

				this.sendReply(socket, reply, uniqueID, packet);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public int getFacilityIndexByName(String facilityName) {
		int facilityIndex = -1;
		for(int i = 0; i < this.facilities.length;i++) {
			if(this.facilities[i].getName().equals(facilityName)) {
				facilityIndex = i;
				break;
			}
		}
		return facilityIndex;
	}
	public int getFacilityByConfirmID(String confirmID) {
		int facilityIndex = -1;
		for(int i = 0; i < this.facilities.length;i++) {
			boolean found = false;
			for(int j = 0; j < this.facilities[i].getBookings().size(); j++) {
				if(this.facilities[i].getBookings().get(j).getConfirmID().equals(confirmID)) {
					found = true;
					break;
				}
			}
			if(found) {
				facilityIndex = i;
				break;
			}
		}
		return facilityIndex;
		
	}
	public void start() {
		while(running) {
            DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
			try {
				socket.receive(packet);
				
				HashMap<String, Object> requestPayload = Utils.unmarshallPayload(this.buffer);
				String clientDetails =packet.getAddress().getHostAddress()+":"+packet.getPort(); 
				int requestId = (int) requestPayload.get("request_id");
				String uniqueID =  clientDetails+"_"+(requestId);
				if(this.invocationType == INVOCATION_TYPE.AT_MOST_ONCE) {
					boolean sent = this.sendReplyEarly(uniqueID, packet);
					if(sent) {
						continue;
					}
				}
				
				byte serviceType = (byte)requestPayload.get("service_type");
				
				HashMap<String, Object> reply = new HashMap<String, Object>();
				reply.put("service_type", (byte) serviceType);
				reply.put("reply_id", (Integer) requestPayload.get("request_id"));
				String facilityName, confirmID;
				int facilityIndex = -1;
				switch(serviceType) {
					case Constants.QUERY_FACILITY:
						facilityName = (String) requestPayload.get("facility_name");
						facilityIndex = this.getFacilityIndexByName(facilityName);
						if(facilityIndex == -1) {
							//indicate success is false
							reply.put("success", ((byte)0));
							reply.put("error_message", "Error: Facility with the name "+requestPayload.get("facility_name")+" not found");
							break;
						}else {
							reply.put("success", ((byte) 1));
						}
						
						byte[] days = (byte[])requestPayload.get("facility_days");
						
						for(int i = 0; i < days.length; i++) {
							reply.put("timeslots_avaliable_"+days[i], facilities[facilityIndex].getBookingIntervals(days[i]));
						}
						
						break;
					case Constants.BOOK_FACILITY:
						facilityName = (String) requestPayload.get("facility_name");
						String startTime = (String) requestPayload.get("start_time");
						String endTime = (String) requestPayload.get("end_time");
						byte day = (byte) requestPayload.get("day");
				
						facilityIndex = this.getFacilityIndexByName(facilityName);
						if(facilityIndex == -1) {
							//indicate success is false
							reply.put("success", ((byte)0));
							reply.put("error_message", "Error: Facility with the name "+requestPayload.get("facility_name")+" not found");
							break;
						}else {
							reply.put("success", ((byte) 1));
						}
						
						Booking booking = new Booking(startTime, endTime, day);
						int conflictBooking = this.facilities[facilityIndex].checkAndGetConflictBooking(booking);
						if(conflictBooking != -1) {
							reply.put("success", ((byte)0));
							
							//TODO: Print where is the booking overlap
							reply.put("error_message", "Error: Booking overlaps with another existing booking");
							break;
						}
						confirmID = this.facilities[facilityIndex].addBooking(booking);
						reply.put("success", ((byte)1));
						reply.put("confirm_id", confirmID);

						this.informAllMonitors(this.facilities[facilityIndex]);
						break;
					case Constants.CHANGE_BOOKING:
						confirmID = (String) requestPayload.get("confirm_id");
						short offset = (short) requestPayload.get("offset");
						
						for(int i =0 ; i < this.facilities.length;i++) {
							int bookingIndex = this.facilities[i].getBookingWithConfirmationID(confirmID);
							if(bookingIndex != -1) {
								// found booking with same confirmation id, see if can advanced first:
								Booking currentBooking = this.facilities[i].getBookings().get(bookingIndex);
								Booking newBooking = new Booking(currentBooking.getStartTime(), currentBooking.getEndTime(), currentBooking.getDay());
								newBooking.setConfirmID(currentBooking.getConfirmID());
								newBooking.offsetBooking(offset);
								int overlapBooking = this.facilities[i].checkAndGetConflictBooking(newBooking);
								if(overlapBooking != -1) {
									reply.put("success", ((byte)0));
									
									reply.put("error_message", "Error: New booking overlaps with another existing booking");
									break;
								}
								this.facilities[i].replaceBooking(bookingIndex, newBooking);
								this.informAllMonitors(this.facilities[i]);
								
								break;
							}
						}
						break;
					case Constants.MONITOR_AVALIABILITY:
						facilityName = (String) requestPayload.get("facility_name");
						short duration = (short) requestPayload.get("duration");
						facilityIndex = getFacilityIndexByName(facilityName);
						if(facilityIndex == -1) {
							//indicate success is false
							reply.put("success", ((byte)0));
							reply.put("error_message", "Error: Facility with the name "+requestPayload.get("facility_name")+" not found");
							break;
						}else {
							reply.put("success", ((byte) 1));
						}
						Monitor monitor = new Monitor(packet.getAddress().getHostAddress(), packet.getPort(), (Calendar.getInstance().getTimeInMillis()/1000l) + (duration * 60), requestId);
						
						ArrayList<Monitor> monitors = this.monitors.getOrDefault(facilityName, new ArrayList<Monitor>());
						monitors.add(monitor);
						this.monitors.put(facilityName, monitors);
						
						break;
					case Constants.CANCEL_BOOKING:
						String confirmationID = (String) requestPayload.get("confirm_id");
						facilityIndex = getFacilityByConfirmID(confirmationID);
						if(facilityIndex == -1) {
							//indicate success is false
							reply.put("success", ((byte)0));
							reply.put("error_message", "Error: Booking with confirmation ID "+confirmationID+" not found");
							break;
						}else {
							reply.put("success", ((byte) 1));
						}
						ArrayList<Booking> bookings = this.facilities[facilityIndex].getBookings();
						int bookingIndex = -1;
						for(int i = 0; i < bookings.size(); i++) {
							if(bookings.get(i).getConfirmID().equals(confirmationID)) {
								bookingIndex = i;
								break;
							}
						}
						bookings.remove(bookingIndex);
						this.facilities[facilityIndex].setBookings(bookings);
						break;
					case Constants.LIST_FACILITY:
						String[] facilitiesNames = new String[this.facilities.length];
						for(int i = 0 ; i < facilitiesNames.length; i++) {
							facilitiesNames[i] = this.facilities[i].getName();
						}
						reply.put("facility_names", facilitiesNames);
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
