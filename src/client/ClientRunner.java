package client;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import shared.Constants;
import shared.Utils;

public class ClientRunner {
	public static byte promptForDay(byte serviceType) {
		return ClientRunner.promptForDays(1, serviceType)[0];
	}
	public static byte[] promptForDays(int count, byte operationType) {
		byte dayOfWeek;
		Scanner scanner = new Scanner(System.in);

		ArrayList<Byte> days = new ArrayList<Byte>();
		if(count == -1) {
			count = 7;
		}
		do {
			System.out.println("Which day would you like to "+(operationType == Constants.BOOK_FACILITY ? "book" : "query")+" for? Choose from these days of the week:");
			
			for(int i = 1; i < Constants.days.length+1; i++) {
				if(days.indexOf(((byte)(i))) == -1)
					System.out.println(" "+i+" - "+ Constants.getDay((byte)i));	
			}
			System.out.println(" 0 - Exit this menu");
							
			dayOfWeek = scanner.nextByte();
			scanner.nextLine();
			if(dayOfWeek != 0) {
				days.add((byte) (dayOfWeek)); 	
				System.out.print("Current "+(operationType == Constants.BOOK_FACILITY ? "Booking" : "Query")+" days: ");
				
				for(int i = 0; i < days.size();i++) {
					if(i == (days.size() -1 )) {
						System.out.println(Constants.getDay((byte) days.get(i)));
					}else {
						System.out.print(Constants.getDay((byte) days.get(i))+" ,");
					}
				}	
			}
			count --;	
		}while(dayOfWeek != 0 && count != 0);
		byte[] daysArray = new byte[days.size()];
		for(int i = 0; i < days.size(); i++) {
			daysArray[i] = (byte)(days.get(i)-1);
		}
		return daysArray;
	}
	public static void main(String[] args) {
		Client client = new Client();
		client.start();
		
		System.out.println("Welcome to the CZ4013 facility booking system :)");
		Scanner scanner = new Scanner(System.in);
		String input = "";
		while(!input.equals("q")) {
			System.out.println("Choose from the avaliable options in the menu:");
			System.out.println("1. Query Avaliability of facility");
			System.out.println("2. Book facility for a period of time");
			System.out.println("3. Change facility booking");
			System.out.println("4. Monitor avaliability of facility");
			System.out.println("5. Cancel facility booking");
			System.out.println("6. List all facilities");
			System.out.println("Enter 'q' to quit");
			
			input = scanner.nextLine();
			System.out.println("Your selection is "+input);
			
			try {
				int option = Integer.parseInt(input);
				String facilityName;
				HashMap<String,Object> reply;
				switch((byte)option) {
				case 1:
					System.out.println("Type the facility name that you want to query:");
					facilityName = scanner.nextLine();
					System.out.println("You have selected facility: "+facilityName);
					
					byte[] daysArray = ClientRunner.promptForDays(-1, (byte)(option-1));
					reply = client.sendQueryRequest(facilityName, daysArray);
					if(((byte)reply.get("success")) == 0) {
						System.out.println((String)reply.get("error_message"));
						break;
					}
					for(int day = 0; day < daysArray.length; day++) {
						short[] timeslots_avaliable = (short[]) reply.get("timeslots_avaliable_"+day);
						
						for(int i = 0; i < timeslots_avaliable.length; i+=2) {
							String formattedStart = Utils.parseMinuteOfDay(timeslots_avaliable[i]);
							String formattedEnd = Utils.parseMinuteOfDay(timeslots_avaliable[i+1]);
							
							System.out.println(formattedStart+ " to "+formattedEnd+" is avaliable");
						}
					}
					break;
				case 2:
					System.out.println("Type the name of the facility you would like to book:");
					facilityName = scanner.nextLine();
					System.out.println("You have selected facility: "+facilityName);
					System.out.println("What type would like to book the facility?");
					// TODO: loop until get start time?
					System.out.print("Start Time(enter in 24hours format eg. 23:59, 08:00):");
					String startTime = scanner.nextLine();
				
					System.out.print("End Time(enter in 24hours format eg. 23:59, 08:00):");
					String endTime = scanner.nextLine();
					System.out.println("Making booking for facility" + facilityName+ " Start time: "+startTime+" end time: "+endTime);
					
					byte day = ClientRunner.promptForDay((byte)(option-1));
					reply = client.sendBookRequest(facilityName, day, startTime, endTime);
					if(((byte)reply.get("success")) == 0) {
						System.out.println((String)reply.get("error_message"));
						break;
					}
					if(((byte)reply.get("success")) == 1) {
						String confirmId = (String) reply.get("confirm_id");
						
						System.out.println("Booking for "+facilityName+" from "+startTime+" to "+endTime+" added. :)");
						System.out.println("Confirmation ID for booking is: "+confirmId);
					}
					break;
				case 3:
					System.out.println("Enter booking confirmation ID:");
					String confirmationID = scanner.nextLine();
					String offsetString;
					do {
						System.out.println("Enter offset by specifying hours and minutes to change");
						System.out.println("Enter +01:00 to postpone by 1 Hour, Enter -01:00 to advance the booking by 1 Hour");
						offsetString = scanner.nextLine();
						if(offsetString.length() != 6 || !(offsetString.startsWith("+") || offsetString.startsWith("-"))) {
							System.out.println("Invalid input format, enter again");
						}
					}while(offsetString.length() != 6);
					char sign = offsetString.charAt(0);
					offsetString = offsetString.replace("+", "").replace("-", "");
					short offsetDuration = Utils.parseTextToMinuteOfDay(offsetString);
					if(sign == '+') {
						offsetDuration *= 1;
					}else {
						offsetDuration *= -1;
					}
					
					reply = client.sendChangeRequest(confirmationID, offsetDuration);
					if(((byte)reply.get("success")) == 0) {
						System.out.println((String)reply.get("error_message"));
						break;
					}
					if(((byte)reply.get("success")) == 1) {
						String confirmId = (String) reply.get("confirm_id");
						
						//TODO: send updated timing over too
						System.out.println("Booking updated and has been "+(offsetDuration > 0 ? "advanced" : "postponed" )+" by "+ Math.abs(offsetDuration)+" hours");
					}
					break;
				case 4:
					System.out.println("Enter facility name to monitor:");
					facilityName = scanner.nextLine();
					System.out.println("Enter monitor interval");
					System.out.println("Format: 01:00 for 1 hour, 00:30 for 30 minutes");
					String lengthMonitor = scanner.nextLine();
					short duration = Utils.parseTextToMinuteOfDay(lengthMonitor);
					reply = client.sendMonitorRequest(facilityName, duration);
					if((byte) reply.get("success") == 1) {
						System.out.println("Subscribed");
					}
					long monitorDeadline = System.nanoTime() + TimeUnit.NANOSECONDS.convert(duration, TimeUnit.MINUTES);
					while ( System.nanoTime() < monitorDeadline){
					  int remainingMs = (int)((monitorDeadline - System.nanoTime()) / 1000000);
					  try {
							System.out.println("remaining "+ remainingMs);
							HashMap<String, Object> callbackReply = client.listenForReply(remainingMs);
							System.out.println("Updated avaliability for "+facilityName);
							for(int i = 0 ; i < 7; i++) {
								// Print each timeslot updated avalibility
								short[] timeslots  = (short[])callbackReply.get("timeslots_avaliable_"+i);
								System.out.println("Updated avaliability for "+Constants.days[i]+" ");
								
								for(int j = 0; j < timeslots.length; j+=2) {
									System.out.println(Utils.parseMinuteOfDay(timeslots[j])+" to "+Utils.parseMinuteOfDay(timeslots[j+1])+" is now avaliable");
									
								}
							}
						} catch (IOException e) {
							System.out.println("timeout");
						}
					}
					break;
				case 5:
					System.out.println("Enter confirmation ID to cancel booking:");
					String confirmID = scanner.nextLine();
					reply = client.sendCancelRequest(confirmID);
					
					break;
				case 6:
					System.out.println("Listing facilities:");
					reply = client.sendListFacilityNames();
					String[] names = (String[]) reply.get("facility_names");
					for(int i =0 ; i < names.length; i++) {
						System.out.println((i+1)+". "+ names[i]);
					}
					break;
				}
			}catch(NumberFormatException e) {
				if(!input.equals("q")) {
					System.out.println("Input select is not a valid option in menu");
				}
			}
		}
	}

}
