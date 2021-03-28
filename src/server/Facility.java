package server;

import java.util.ArrayList;

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
	public void addBooking(Booking booking) {
		System.out.println("Added booking for "+this.name);
		this.bookings.add(booking);
	}
	public boolean hasSameBooking(Booking booking) {
		int index = -1;
		for(int i = 0; i < this.bookings.size(); i++) {
			if(this.bookings.get(i).getStartMinuteOfDay() >= booking.getStartMinuteOfDay() && this.bookings.get(i).getEndMinuteOfDay() <= booking.getEndMinuteOfDay()) {
				index = i;
				break;
			}
		}
		return index != -1;	
	}
	public ArrayList<Booking> getBookings() {
		return bookings;
	}
	public void setBookings(ArrayList<Booking> bookings) {
		this.bookings = bookings;
	}
	public short[] getBookingIntervals(byte day) {
		short[] singleTime = new short[24*60];
		for(int i = 0; i < singleTime.length; i++) {
			singleTime[i] = (short)i;
		}
		ArrayList<Short> singleTimeValid = new ArrayList<Short>();
		for(int i = 0; i < singleTime.length; i++) {
			if(this.bookings.size() == 0) {
				singleTimeValid.add((short) i);
			}else {
				boolean timeNotOverlap = true;
				for(int j = 0; j < this.bookings.size(); j++) {
					if(this.bookings.get(j).getStartMinuteOfDay() <= i && this.bookings.get(j).getEndMinuteOfDay() >= i) {
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
		short intervalEnd = -1;
		for(short i = 1; i < singleTimeValid.size(); i++) {
			if((singleTimeValid.get(i-1).shortValue()+1) != singleTimeValid.get(i).shortValue()) {
				bookingInterval.add(new Short[] {intervalStart, singleTimeValid.get(i-1)});
				intervalStart = (short) singleTimeValid.get(i);
			}else if(intervalStart == -1) {
				intervalStart = (short) singleTimeValid.get(i-1);
				
			}else if( i == singleTimeValid.size() - 1) {
				bookingInterval.add(new Short[] {intervalStart, singleTimeValid.get(i)});
			}
		}
		// might need to clean up last interval?
		short[] returnInterval = new short[bookingInterval.size()*2];
		System.out.println("This facility has "+this.bookings.size()+" bookings and "+bookingInterval.size());
		
		for(int i = 0 ; i< returnInterval.length; i+=2) {
			returnInterval[i] = bookingInterval.get(i/2)[0];
			returnInterval[i+1] = bookingInterval.get(i/2)[1];
		}
		return returnInterval;
	}
}
