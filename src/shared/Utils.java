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
	public static String getFormat(int hour, int minute) {
		hour = hour % 24;
		minute = minute % 60;
		return String.format("%02d:%02d", hour, minute);
	}
	public static short parseTextToMinuteOfDay(String text) {
		String[] parts = text.split(":");
		
		byte hours = Byte.parseByte(parts[0]);
		byte mins = Byte.parseByte(parts[1]);
		short offsetDuration = (short)(hours * 60 + mins);
		return offsetDuration;
	}
	public static byte[] combineByteArray(byte[] a, byte[] b) {
		byte[] combined = new byte[a.length + b.length];
		for (int i = 0; i < combined.length; ++i){
		    combined[i] = i < a.length ? a[i] : b[i - a.length];
		}
		return combined;
	}
	public static byte[] toBytes(short i) {
		  byte[] result = new byte[2];

		  result[0] = (byte) (i >> 8);
		  result[1] = (byte) (i /*>> 0*/);
		  
		  return result;
	}
	public static byte[] toBytes(int i) {
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i /*>> 0*/);
	  
	  return result;
	}
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
				System.out.println("Encoding "+ field);
				
				short[] data = (short[])payload.get(field);
				result = combineByteArray(result, new byte[] {3});
				result = combineByteArray(result, toBytes(data.length));
				for(int i = 0; i < data.length; i++) {
					byte[] byteValues = toBytes(data[i]);
					System.out.println(byteValues.length+ " byte values ");
					result = combineByteArray(result, byteValues);
				}
			}else if (payload.get(field).getClass().equals(Byte.class)) {
				result = combineByteArray(result, new byte[] {4});
				result = combineByteArray(result, new byte[] {(byte)payload.get(field)});
			}else if (payload.get(field).getClass().equals(Short.class)) {
				result = combineByteArray(result, new byte[] {5});
				result = combineByteArray(result, toBytes((short) payload.get(field)));
			}
		}
		return result;
	}
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
					System.out.println("decoded value"+fieldPropertyName+" as "+propertyValue);
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
					
					for(int i = 0; i < (shortArrayLength); i++) {
						shortValues[i] = ByteBuffer.wrap(Arrays.copyOfRange(data, byte_offset, byte_offset + 2)).getShort(); 
						byte_offset += 2;
					}
					payload.put(fieldPropertyName, shortValues);
					break;
				case 4:
					payload.put(fieldPropertyName, (byte)data[byte_offset]);
					byte_offset++;
					break;
				case 5:
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
