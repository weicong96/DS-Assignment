package shared;

public class Constants {

	public final static int PORT = 2220;
	// 2 second timeout 
	public final static int TIMEOUT = 2* 1000;
	
	//1024 byte buffer size in sending and receiving
	public final static int BUFFER_SIZE = 1024;
	
	//byte enums, java does not support byte enums
	public final static byte QUERY_FACILITY = 0;
	public final static byte BOOK_FACILITY = 1;
	public final static byte CHANGE_BOOKING = 2;
	public final static byte MONITOR_AVALIABILITY = 3;
	public final static byte CANCEL_BOOKING = 4;
	public final static byte LIST_FACILITY = 5;
	
	// list of days
	public static String[] days = new String[] {
			"Monday",
			"Tuesday",
			"Wednesday",
			"Thursday",
			"Friday",
			"Saturday",
			"Sunday"
	};
	//translate byte day to string day
	public static String getDay(byte dayOfWeek) {
		return days[dayOfWeek-1];
	}
	
	public enum INVOCATION_TYPE{
		AT_MOST_ONCE,
		AT_LEAST_ONCE
	}
}
