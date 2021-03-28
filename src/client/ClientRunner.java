package client;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import shared.Constants;
import shared.Utils;

public class ClientRunner {
	public static byte promptForDay() {
		return ClientRunner.promptForDays(1)[0];
	}
	public static byte[] promptForDays(int count) {
		byte dayOfWeek;
		Scanner scanner = new Scanner(System.in);

		ArrayList<Byte> days = new ArrayList<Byte>();
		if(count == -1) {
			count = 7;
		}
		do {
			System.out.println("Which day would you like to query for? Choose from these days of the week:");
			
			for(int i = 1; i < Constants.days.length+1; i++) {
				if(days.indexOf(((byte)(i))) == -1)
					System.out.println(" "+i+" - "+ Constants.days[i-1]);	
			}
			System.out.println(" 0 - Exit this menu");
							
			dayOfWeek = scanner.nextByte();
			scanner.nextLine();
			if(dayOfWeek != 0) {
				days.add(dayOfWeek); 	
				String day = Constants.getDay((byte)(dayOfWeek-1));
				System.out.println("Added "+day+ " into your query");
				System.out.print("Current Query days: ");
				
				for(int i = 0; i < days.size();i++) {
					if(i == (days.size() -1 )) {
						System.out.println(Constants.days[days.get(i)-1]);
					}else {
						System.out.print(Constants.days[days.get(i)-1]+" ,");
					}
				}	
			}
			count --;	
		}while(dayOfWeek != 0 || count == 0);
		byte[] daysArray = new byte[days.size()];
		for(int i = 0; i < days.size(); i++) {
			daysArray[i] = days.get(i);
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
			System.out.println("Enter 'q' to quit");
			
			input = scanner.nextLine();
			System.out.println("Your selection is "+input);
			
			try {
				int option = Integer.parseInt(input);
				String facilityName;
				switch(option) {
				case 1:
					System.out.println("Type the facility name that you want to query:");
					facilityName = scanner.nextLine();
					System.out.println("You have selected facility: "+facilityName);
					
					byte[] daysArray = ClientRunner.promptForDays(-1);
					HashMap<String, Object> reply = client.sendQueryRequest(facilityName, daysArray);
					if(((byte)reply.get("success")) == 0) {
						System.out.println((String)reply.get("error_message"));
						break;
					}
					for(int day = 0; day < daysArray.length; day++) {
						short[] timeslots_avaliable = (short[]) reply.get("timeslots_avaliable_"+day);
						
						for(int i = 0; i < timeslots_avaliable.length; i+=2) {
							int hour = timeslots_avaliable[i] / 60;
							int minute = timeslots_avaliable[i] % 60;
							
							int hourEnd = timeslots_avaliable[i+1] / 60;
							int minuteEnd = timeslots_avaliable[i+1] % 60;
							
							String formattedStart = Utils.getFormat(hour, minute);
							String formattedEnd = Utils.getFormat(hourEnd, minuteEnd);
							
							System.out.println(formattedStart+ " to "+formattedEnd+" is avaliable");
						}
					}
					break;
				case 2:
					System.out.println("Type the name of the facility you would like to book:");
					facilityName = scanner.nextLine();
					System.out.println("You have selected facility: "+facilityName);
					System.out.println("What type would like to book the facility?");
					System.out.print("Start Time(enter in 24hours format eg. 23:59, 08:00):");
					String startTime = scanner.nextLine();
					System.out.print("End Time(enter in 24hours format eg. 23:59, 08:00):");
					String endTime = scanner.nextLine();
					System.out.println("Making booking for facility" + facilityName+ " Start time: "+startTime+" end time: "+endTime);
					
					byte day = ClientRunner.promptForDay();
					HashMap<String,Object> replyPayload = client.sendBookRequest(facilityName, day, startTime, endTime);
					
					break;
				case 3:
					break;
				case 4:
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
