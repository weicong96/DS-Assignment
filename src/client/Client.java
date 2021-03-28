package client;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import shared.Constants;
import shared.Utils;

public class Client {
	private DatagramSocket socket;
	private InetAddress address;
	
	private int REQUEST_ID = 0;
	//private HashMap<Integer, Integer> requests = new HashMap<Integer, Integer>();
	public void start() {		
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(Constants.TIMEOUT);
			address = InetAddress.getByName("localhost");
		} catch (SocketException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public HashMap<String, Object> sendQueryRequest(String facilityName, byte[] days) {
		HashMap<String, Object> request = new HashMap<String, Object>();
		request.put("service_type", Constants.QUERY_FACILITY);
		request.put("facility_name", facilityName);
		request.put("facility_days", days);
		
		return this.sendRequest(request);
	}
	public HashMap<String, Object> sendBookRequest(String facilityName, byte day, String startTime, String endTime){
		HashMap<String, Object> request = new HashMap<String, Object>();
		request.put("service_type", Constants.BOOK_FACILITY);
		request.put("day", (byte)day);
		request.put("start_time", startTime);
		request.put("end_time", endTime);
		request.put("facility_name", facilityName);
		
		return this.sendRequest(request);
	}
	public HashMap<String, Object> sendRequest(HashMap<String, Object> requestPayload) {
		String data = "";
		HashMap<String, Object> replyPayload = null;
		requestPayload.put("message_type", (byte)0);
		requestPayload.put("request_id", this.REQUEST_ID);
		requestPayload.put("fields_length", (byte)(requestPayload.entrySet().size() - 3));
		byte[] buff = Utils.marshallPayload(requestPayload);
		this.REQUEST_ID ++;
		boolean waiting = true;
		while(waiting) {
			try {
				this.socket.send(new DatagramPacket(buff, buff.length, address, Constants.PORT));
    			byte[] buffer = new byte[Constants.BUFFER_SIZE];
    			
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            	socket.receive(packet);
            	
            	replyPayload = Utils.unmarshallPayload(packet.getData());
            	if(((byte) replyPayload.get("service_type")) == ((byte) requestPayload.get("service_type"))) {
            		if(((int) replyPayload.get("reply_id")) == ((int) requestPayload.get("request_id"))) {
            			//same reply so stop sending it
                    	break;
                	}
            	}
			}catch (SocketTimeoutException e) {
				System.out.println("Repeating request, did not receive reply.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return replyPayload;
	}
}
