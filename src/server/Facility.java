package server;

import java.util.ArrayList;
import java.util.Random;
public class Facility {
	private String name;
	private ArrayList<Booking> bookings = new ArrayList<Booking>();
	public Facility(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public ArrayList<Booking> getBookings() {
		return bookings;
	}
	public void setBookings(ArrayList<Booking> bookings) {
		this.bookings = bookings;
	}
	
	public String addBooking(Booking booking) {
		booking.setConfirmID(this.generateID());
		this.bookings.add(booking);
		return booking.getConfirmID();
	}
	
	//return and find index of booking if there is any booking that conflicts with the current one.
		public int checkAndGetConflictBooking(Booking booking) {
			int index = -1;
			for(int i = 0; i < this.bookings.size(); i++) {
				if(this.bookings.get(i).getDay() == booking.getDay() ) {
					if(bookings.get(i).getStartMinuteOfDay() >= booking.getStartMinuteOfDay() && this.bookings.get(i).getEndMinuteOfDay() <= booking.getEndMinuteOfDay()) {
						index = i;
						break;
					}else if(bookings.get(i).getStartMinuteOfDay() <= booking.getStartMinuteOfDay() && bookings.get(i).getEndMinuteOfDay() >= booking.getStartMinuteOfDay()) {
						index = i;
						break;
					}else if(bookings.get(i).getStartMinuteOfDay() <= booking.getEndMinuteOfDay() && bookings.get(i).getEndMinuteOfDay() >= booking.getEndMinuteOfDay()) {
						index = i;
						break;
					}
				}
			}
			return index;
		}
	// get random id
	private String generateID() {
		char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder sb = new StringBuilder(10);
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		String output = sb.toString();
		return output;
	}
	
	public short[] getBookingIntervals(byte day) {
		short[] singleTime = new short[24*60];
		//init a 1440 size array to represent all minutes in a day
		for(int i = 0; i < singleTime.length; i++) {
			singleTime[i] = (short)i;
		}
		//for each time, see if it overlaps, for a booking that books 00:05 to 00:10 , result will be [0, 1, 2, 3, 4 , 11, 12...]
		ArrayList<Short> singleTimeValid = new ArrayList<Short>();
		for(int i = 0; i < singleTime.length; i++) {
			if(this.bookings.size() == 0) {
				singleTimeValid.add((short) i);
			}else {
				boolean timeNotOverlap = true;
				for(int j = 0; j < this.bookings.size(); j++) {
					if(this.bookings.get(j).getDay() == day && this.bookings.get(j).getStartMinuteOfDay() <= i && this.bookings.get(j).getEndMinuteOfDay() >= i) {
						timeNotOverlap = false;
						break;
					}
				}
				if(timeNotOverlap) {
					singleTimeValid.add((short)i);
				}
			}
		}
		ArrayList<Short[]> bookingInterval = new ArrayList<Short[]>();
		short intervalStart = -1;
		// group all values that are within same interval together
		for(short i = 1; i < singleTimeValid.size(); i++) {
			if((singleTimeValid.get(i-1).shortValue()+1) != singleTimeValid.get(i).shortValue()) {
				if(intervalStart == -1) {
					intervalStart = 0;
				}
				bookingInterval.add(new Short[] {intervalStart, singleTimeValid.get(i-1)});
				intervalStart = (short) singleTimeValid.get(i);
			}else if(intervalStart == -1) {
				intervalStart = (short) singleTimeValid.get(i-1);
				System.out.println("set interval start "+ intervalStart);
			}else if( i == singleTimeValid.size() - 1) {
				bookingInterval.add(new Short[] {intervalStart, singleTimeValid.get(i)});
			}
		}
		
		//maps the n intervals into n * 2 size array, where every 2 values indicate interval start and end
		short[] returnInterval = new short[bookingInterval.size()*2];
		for(int i = 0 ; i< returnInterval.length; i+=2) {
			returnInterval[i] = bookingInterval.get(i/2)[0];
			returnInterval[i+1] = bookingInterval.get(i/2)[1];
		}
		return returnInterval;
	}
	// find booking with the given confirmation ID
	public int getBookingWithConfirmationID(String confirmID) {
		for(int i = 0; i < this.bookings.size();i++) {
			if(this.bookings.get(i).getConfirmID().equals(confirmID)) {
				return i;
			}
		}
		return -1;
	}
	//replace booking by setting index
	public void replaceBooking(int index, Booking booking) {
		this.bookings.set(index, booking);
	}
}
