package at.acriba.logdog;

import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class IPHourStat extends LinkedHashMap<InetAddress, HourStat> {
	
	private static final long serialVersionUID = 1L;
	private static final int MAX_ENTRIES = 500;
	
	private static Logger logger = LoggerFactory.getLogger(IPHourStat.class);
	
	public int limitMinutes;
	public int limitCount;
	public boolean executeAction = true;
	public String name;
	
	public IPHourStat(String name, int limitCount, int limitMinutes, boolean executeAction) {
		super(100, .75f, true);
		this.name = name;
		this.limitCount = limitCount;
		this.limitMinutes = limitMinutes;
		this.executeAction = executeAction;
	}
		
	@Override
	protected boolean removeEldestEntry(Map.Entry<InetAddress, HourStat> eldest) {
		return size() > MAX_ENTRIES;
	}

	public void put(String ipAddress, int minute, int hour, int count) {
				
		InetAddress ip = InetAddresses.forString(ipAddress);
		
		if(LogDog.jail.getIfPresent(ip) != null) return;
		
		HourStat c = this.get(ip);
		if(c == null) {
			c = new HourStat(minute, hour, count);
			this.put(ip, c);
		} else {
			c.add(minute, hour, count);
		}
		
		//Prüfung auf Limit
		try {
			int counted = c.getCount(hour, minute, this.limitMinutes);
			if(counted >= this.limitCount) {
				
				if(LogDog.jail.getIfPresent(ip) == null) {
					
					String txtIp = InetAddresses.toAddrString(ip);
					Instant now = Instant.now();
					logger.info(name + " - limit exceeded: " + txtIp);
					LogDog.jail.put(ip, now);
					
					if(executeAction) {
						Runtime rt = Runtime.getRuntime();
						rt.exec("/sbin/iptables -I INPUT 1 -s " + txtIp + " -j DROP");
					}
					
				}
			}
		} catch (Exception e) {
			logger.error("error checking limit for " + name, e);
		}
		
	}
	
}
