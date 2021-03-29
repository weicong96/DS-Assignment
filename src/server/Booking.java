package server;

import shared.Utils;

public class Booking {
	private String startTime;
	private String endTime;

	private byte day;
	private String confirmID;
	public Booking(String startTime, String endTime, byte day) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.day = day;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public byte getDay() {
		return day;
	}
	public void setDay(byte day) {
		this.day = day;
	}
	public int getStartHour() {
		String hour =  startTime.split(":")[0];
		return Integer.parseInt(hour);
	}
	public String getConfirmID() {
		return confirmID;
	}
	public void setConfirmID(String confirmID) {
		this.confirmID = confirmID;
	}
	public int getStartMinute() {
		String min =  startTime.split(":")[1];
		return Integer.parseInt(min);
	}
	
	public int getEndHour() {
		String min = endTime.split(":")[0];
		return Integer.parseInt(min);
	}
	public int getEndMinute() {
		String min = endTime.split(":")[1];
		return Integer.parseInt(min);
	}
	public int getStartMinuteOfDay() {
		return (this.getStartHour() * 60) + this.getStartMinute();
	}

	public int getEndMinuteOfDay() {
		return (this.getEndHour() * 60) + this.getEndMinute();
	}
	public void offsetBooking(short offset) {
		short newStart = (short)(this.getStartMinuteOfDay() + offset);
		short newEnd = (short)(this.getEndMinuteOfDay() + offset);
		
		int newStartHour = (newStart/60);
		int newStartMin = (newStart % 60);
		int newEndHour = (newEnd/60);
		int newEndMin = (newEnd % 60);
		
		String startString = Utils.getFormat(newStartHour, newStartMin);
		String endString = Utils.getFormat(newEndHour, newEndMin);
		this.setEndTime(endString);
		this.setStartTime(startString);
	}
	
}
