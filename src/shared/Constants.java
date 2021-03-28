package shared;

public class Constants {

	public final static int PORT = 4445;
	public final static int TIMEOUT = 30 * 1000;
	public final static int BUFFER_SIZE = 1024;
	//byte enums
	public final static byte QUERY_FACILITY = 0;
	public final static byte BOOK_FACILITY = 1;
	public final static byte CHANGE_BOOKING = 2;
	public final static byte MONITOR_AVALIABILITY = 3;
	public static String[] days = new String[] {
			"Monday",
			"Tuesday",
			"Wednesday",
			"Thursday",
			"Friday",
			"Saturday",
			"Sunday"
	};
	public static String getDay(byte dayOfWeek) {
		return days[dayOfWeek];
	}
}
