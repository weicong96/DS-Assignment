package shared;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;
import java.util.TimeZone;

public class Utils {
	
	// parse minute of day to HH:MM format
	public static String parseMinuteOfDay(short minuteOfDay) {
		int hour = (minuteOfDay / 60);
		int minute = (minuteOfDay % 60);
		
		return getFormat(hour, minute);
	}
	// format hour and minute properly with 0 to 23 and 0 to 60 range
	public static String getFormat(int hour, int minute) {
		hour = hour % 24;
		minute = minute % 60;
		return String.format("%02d:%02d", hour, minute);
	}
	// parse from HH:MM format to minute of day
	public static short parseTextToMinuteOfDay(String text) {
		String[] parts = text.split(":");
		
		byte hours = Byte.parseByte(parts[0]);
		byte mins = Byte.parseByte(parts[1]);
		short offsetDuration = (short)(hours * 60 + mins);
		return offsetDuration;
	}
	//for merging an array
	public static byte[] combineByteArray(byte[] a, byte[] b) {
		byte[] combined = new byte[a.length + b.length];
		for (int i = 0; i < combined.length; ++i){
		    combined[i] = i < a.length ? a[i] : b[i - a.length];
		}
		return combined;
	}
	// convert short to 2 bytes
	public static byte[] toBytes(short i) {
		  byte[] result = new byte[2];

		  result[0] = (byte) (i >> 8);
		  result[1] = (byte) (i /*>> 0*/);
		  
		  return result;
	}
	// convert int to 4 bytes
	public static byte[] toBytes(int i) {
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i /*>> 0*/);
	  
	  return result;
	}
	// print contents of what is send and receive in both directions
	public static void printContents(HashMap<String, Object> payload, boolean sending) {
		System.out.println("INFO: "+(sending ? "Sending" : "Receiving ")+" the following contents");
		Set<String> fieldsSet = payload.keySet();
		for(String field: fieldsSet) {
			Object value = payload.get(field);
			System.out.print("\t "+field+" : ");
			if(value instanceof byte[]) {
				byte[] byteArray = (byte[])value;
				for(int i = 0; i < byteArray.length;i++) {
					System.out.print("\n\t\t Item "+i+" : "+byteArray[i]);
				}
				System.out.print("\n");
			}else if(value instanceof String[]) {

				String[] stringArray = (String[])value;
				for(int i = 0; i < stringArray.length;i++) {
					System.out.print("\n\t\t Item "+i+" : "+stringArray[i]);
				}
				System.out.print("\n");
			}else if(value instanceof short[]) {
				short[] shortArray = (short[])value;
				for(int i = 0; i < shortArray.length;i++) {
					System.out.print("\n\t\t Item "+i+" : "+shortArray[i]);
				}
				System.out.print("\n");
			}else {
				System.out.print(payload.get(field)+"\n");
			}
		}
	}
	
	//marshall according the request message
	public static byte[] marshallPayload(HashMap<String, Object> _payload) {
		HashMap<String, Object> payload = (HashMap<String, Object>) _payload.clone();
		byte messageType = (byte)payload.get("message_type");
		Integer id;
		if(messageType == 0) {
			id = (Integer)payload.get("request_id");
		}else {
			id = (Integer)payload.get("reply_id");
		}
		byte serviceType = (byte)payload.get("service_type");
		byte fieldsLength = (byte)payload.get("fields_length");
		
		byte[] result = combineByteArray(new byte[]{messageType}, toBytes(id));
		result = combineByteArray(result, new byte[]{serviceType});
		result = combineByteArray(result, new byte[]{fieldsLength});
		
		//don't parse fields that message_type, service_type etc dont get parsed
		Set<String> fieldsSet = payload.keySet();
		fieldsSet.removeAll(Arrays.asList(new String[] { "message_type", "service_type", "fields_length", "request_id", "reply_id"}));
		for(String field : fieldsSet) {
			//add field value first
			result = combineByteArray(result,new byte[] {
					(byte)field.length()
			});
			result = combineByteArray(result, field.getBytes());
			
			if(payload.get(field).getClass().equals(String.class)) {
				String value = (String)payload.get(field);
				
				result = combineByteArray(result, new byte[] {0});
				result = combineByteArray(result, toBytes(value.length()));
				result = combineByteArray(result, value.getBytes());
			}else if(payload.get(field).getClass().equals(Integer.class)) {
				result = combineByteArray(result, new byte[] {1});
				result = combineByteArray(result, toBytes((int)payload.get(field)));
			}else if(payload.get(field) instanceof byte[]) {
				byte[] data = (byte[])payload.get(field);
				result = combineByteArray(result, new byte[] {2});
				result = combineByteArray(result, toBytes(data.length));
				for(int i = 0; i < data.length; i++) {
					result = combineByteArray(result, new byte[] {data[i]});
				}
			}else if(payload.get(field) instanceof short[]) {
				short[] data = (short[])payload.get(field);
				result = combineByteArray(result, new byte[] {3});
				result = combineByteArray(result, toBytes(data.length));
				for(int i = 0; i < data.length; i++) {
					byte[] byteValues = toBytes(data[i]);
					result = combineByteArray(result, byteValues);
				}
			}else if (payload.get(field).getClass().equals(Byte.class)) {
				result = combineByteArray(result, new byte[] {5});
				result = combineByteArray(result, new byte[] {(byte)payload.get(field)});
			}else if (payload.get(field).getClass().equals(Short.class)) {
				result = combineByteArray(result, new byte[] {6});
				result = combineByteArray(result, toBytes((short) payload.get(field)));
			}else if (payload.get(field) instanceof String[]) {
				String[] data = (String[]) payload.get(field);
				
				result = combineByteArray(result, new byte[] {4});
				result = combineByteArray(result, toBytes(data.length));
				for(int i = 0; i < data.length; i++) {
					result = combineByteArray(result, toBytes(data[i].length()));
					result = combineByteArray(result, data[i].getBytes());
				}
			}
		}
		return result;
	}
	
	// umarshall byte into request or reply payload
	public static HashMap<String, Object> unmarshallPayload(byte[] data) {
		HashMap<String, Object> payload = new HashMap<String, Object>();
		payload.put("message_type", (byte) data[0]);
		
		byte[] id_byte = Arrays.copyOfRange(data, 1, 5);
		if(((byte)payload.get("message_type")) == 0) {
			payload.put("request_id", ByteBuffer.wrap(id_byte).getInt());	
		}else {
			payload.put("reply_id", ByteBuffer.wrap(id_byte).getInt());	
		}
		payload.put("service_type", (byte)data[5]);
		payload.put("fields_length", (byte) data[6]);
		
		int field_count = 0;
		int byte_offset = 7;
		while(field_count != (byte) payload.get("fields_length")) {
			byte fieldPropertyLength = (byte)data[byte_offset];
			byte_offset ++;
			
			String fieldPropertyName = new String(Arrays.copyOfRange(data, byte_offset, byte_offset+fieldPropertyLength));
			byte_offset += fieldPropertyLength; 
			
			byte propertyType = (byte)data[byte_offset];
			byte_offset ++;
			
			switch(propertyType) {
				case 0:
					byte[] byteValues = Arrays.copyOfRange(data, byte_offset, byte_offset+4);
					ByteBuffer byteBuffer = ByteBuffer.wrap(byteValues);
					int propertyLength = byteBuffer.getInt();
					byte_offset += 4;
					String propertyValue = new String(Arrays.copyOfRange(data, byte_offset, byte_offset+propertyLength));
					byte_offset += propertyLength;
					payload.put(fieldPropertyName, propertyValue);
					break;
				case 1:
					int value = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset+4)).getInt();
					byte_offset += 4;
					payload.put(fieldPropertyName, value);
					break;
				case 2:
					int arrayLength = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset+4)).getInt();
					byte_offset += 4;
					
					byte[] values = new byte[arrayLength];
					
					for(int i = 0; i < (arrayLength); i++) {
						values[i] = data[i+byte_offset]; 
					}
					payload.put(fieldPropertyName, values);
					byte_offset += arrayLength;
					break;
				case 3:
					int shortArrayLength = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset+4)).getInt();
					byte_offset += 4;
					
					short[] shortValues = new short[shortArrayLength];
					
					for(int i = 0; i < shortArrayLength; i++) {
						shortValues[i] = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset + 2)).getShort(); 
						byte_offset += 2;
					}
					payload.put(fieldPropertyName, shortValues);
					break;
				case 4:
					int stringArrayLength = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset+4)).getInt();
					byte_offset += 4;
					String[] stringValues = new String[stringArrayLength];
					
					for(int i = 0; i < stringArrayLength; i++) {
						int stringValueLength = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset + 4)).getInt();  
						byte_offset += 4;
						
						String stringValue = new String(Arrays.copyOfRange(data, byte_offset, byte_offset+stringValueLength));
						byte_offset += stringValueLength;
						
						stringValues[i] = stringValue;
					}
					payload.put(fieldPropertyName, stringValues);
					break;
				case 5:
					payload.put(fieldPropertyName, (byte)data[byte_offset]);
					byte_offset++;
					break;
				case 6:
					short shortValue = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset+2)).getShort();
					payload.put(fieldPropertyName, shortValue);
					byte_offset += 2;
					break;
				
			} 
			field_count ++;
		}
		
		return payload;
	}
}
