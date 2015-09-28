package at.acriba.logdog;

public class HourStat {
	
	int hour;
	int minutes[] = new int[60];
	
	public HourStat(int minute, int hour, int count) {
		this.hour = hour;
		this.minutes[minute] += count;
	}
	
	public void add(int minute, int hour, int count) {
		if(Math.abs(this.hour - hour) > 1) this.minutes = new int[60];
		this.hour = hour;
		this.minutes[minute] += count;
	}
	
	public int getCount(int hour, int minute, int interval) {
		if(Math.abs(this.hour - hour) > 1) return 0;
		int total = 0;
		for(int i = 0; i<interval; i++) {
			int index = (minute-i) % 60;			
			total += this.minutes[index];
		}
		return total;
	}
	
}
