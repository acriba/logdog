package at.acriba.logdog;

import java.util.List;
import java.util.regex.Matcher;

public class FileListener {
	
	IPHourStat stat;
	List<IPStatPattern> patterns;
	
	public FileListener(IPHourStat stat, List<IPStatPattern> patterns) {
		this.stat = stat;
		this.patterns = patterns;
	}
		
	public void processline(String line) {
		
		for(IPStatPattern p : patterns) {
		
			Matcher regexMatcher = p.pattern.matcher(line);
			if (regexMatcher.find()) {
				String Hour = regexMatcher.group(p.hour);
				String Minute = regexMatcher.group(p.minute);
				String IP = regexMatcher.group(p.ip);		
				stat.put(IP, Integer.parseInt(Minute), Integer.parseInt(Hour), 1);
				return;
			}
		
		}

	}
	
}
