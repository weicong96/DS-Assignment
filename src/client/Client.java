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
import java.util.concurrent.TimeUnit;

import shared.Constants;
import shared.Utils;

public class Client {
	private DatagramSocket socket;
	private InetAddress address;
	
	private int REQUEST_ID = 0;
	
	public void start() {		
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(Constants.TIMEOUT);
			address = InetAddress.getByName("localhost");
			System.out.println("running on port"+ socket.getPort());
		} catch (SocketException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public HashMap<String, Object> sendListFacilityNames(){
		HashMap<String, Object> request = new HashMap<String, Object>();
		request.put("service_type", Constants.LIST_FACILITY);
		
		return this.sendRequest(request);
	}
	public HashMap<String, Object> sendCancelRequest(String confirmID){
		HashMap<String, Object> request = new HashMap<String, Object>();
		request.put("service_type", Constants.CANCEL_BOOKING);
		request.put("confirm_id", confirmID);
		
		return this.sendRequest(request);
	}
	public HashMap<String, Object> sendMonitorRequest(String facilityName, short duration){
		HashMap<String, Object> request = new HashMap<String, Object>();
		request.put("service_type", Constants.MONITOR_AVALIABILITY);
		request.put("facility_name", facilityName);
		request.put("duration", duration);
		return this.sendRequest(request);
	}
	public HashMap<String, Object> sendChangeRequest(String confirmationID, short offset){
		HashMap<String, Object> request = new HashMap<String, Object>();
		request.put("service_type", Constants.CHANGE_BOOKING);
		request.put("confirm_id", confirmationID);
		request.put("offset", offset);
		return this.sendRequest(request);
		
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
	public HashMap<String, Object> listenForReply(int timeout) throws IOException {
		HashMap<String, Object> replyPayload = null;
		byte[] buffer = new byte[Constants.BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		socket.setSoTimeout(timeout);
		socket.receive(packet);
    	replyPayload = Utils.unmarshallPayload(packet.getData());
    	return replyPayload;
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
    			
            	replyPayload = this.listenForReply(Constants.TIMEOUT);
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
