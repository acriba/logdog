package at.acriba.logdog;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.net.InetAddresses;

import at.acriba.logdog.io.LogTailer;

public class LogDog {

	public static Cache<InetAddress, Instant> jail;
	
	private static Logger logger = LoggerFactory.getLogger(LogDog.class);
	
	public LogDog() {
		
		LogDog.jail = CacheBuilder.newBuilder().expireAfterAccess(4, TimeUnit.HOURS).removalListener( (RemovalNotification<InetAddress, Instant> removal) -> {
			String txtIp = InetAddresses.toAddrString(removal.getKey());
			try {
				Runtime rt = Runtime.getRuntime();
				rt.exec("/sbin/iptables -D INPUT -s " + txtIp + " -j DROP");
				logger.info("unbanned: " + txtIp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).build();
		
	}
	
	public void startConfiguration(String configurationFile) throws JDOMException, IOException {

		SAXBuilder jdomBuilder = new SAXBuilder();
		Document doc = jdomBuilder.build(new File(configurationFile));

		Element root = doc.getRootElement();
				
		root.getChildren().stream().forEach(e -> {
			
			try {
				
				String filename = e.getChild("file").getText();
				if(Strings.isNullOrEmpty(filename)) throw new Exception("Invalid Configuration: filename is mandatory.");
				
				int limitCount;
				try {
					limitCount = Integer.parseInt(e.getChild("limitCount").getText());
				} catch (NumberFormatException e1) {
					throw new Exception("Invalid Configuration: limitCount has to be an integer.", e1);
				}
				if(limitCount <= 0) throw new Exception("Invalid Configuration: limitCount has to be > 0.");
				
				int limitMinutes;
				try {
					limitMinutes = Integer.parseInt(e.getChild("limitMinutes").getText());
				} catch (NumberFormatException e1) {
					throw new Exception("Invalid Configuration: limitMinutes has to be an integer.", e1);
				}
				if(limitMinutes <= 0 || limitMinutes > 60) throw new Exception("Invalid Configuration: limitMinutes has to be between 1 and 60.");
				
				Attribute attrName = e.getAttribute("name");
				String name = attrName != null && !attrName.getValue().isEmpty() ? attrName.getValue() : filename;
	
				Attribute attrExecuteAction = e.getAttribute("executeAction");
				boolean executeAction = true;
				try {
					if (attrExecuteAction != null)
						executeAction = attrExecuteAction.getBooleanValue();
				} catch (Exception e2) {
					throw new Exception("Invalid Configuration: executeAction invalid.", e2);
				}
	
				Attribute analyzeFromStart = e.getChild("file").getAttribute("analyzeFromStart");
				boolean analyzeCompleteFile = false;
				try {
					analyzeCompleteFile = analyzeFromStart != null && analyzeFromStart.getBooleanValue();
				} catch (Exception e2) {
					throw new Exception("Invalid Configuration: analyzeFromStart invalid.", e2);
				}
	
				List<IPStatPattern> patterns = e.getChild("patterns").getChildren().stream().map(p -> {
					try {
						IPStatPattern pattern = new IPStatPattern();
						pattern.pattern = Pattern.compile(p.getText());
						pattern.hour = Integer.parseInt(p.getAttributeValue("hour"));
						pattern.minute = Integer.parseInt(p.getAttributeValue("minute"));
						pattern.ip = Integer.parseInt(p.getAttributeValue("ip"));
						return pattern;
					} catch (Exception e1) {
						logger.error("Could not add pattern " + p.getText() + ". Errormessage: " + e1.getMessage(), e1);
						return null;
					}
				})
						.filter( p -> p != null)
						.collect(Collectors.toList());
				
				if(patterns.size() == 0) throw new Exception("Invalid Configuration: at least 1 valid pattern is mandatory.");
				
				this.watchFile(filename, name, analyzeCompleteFile, executeAction, limitCount, limitMinutes, patterns);
				
			} catch (Exception e1) {
				logger.error("Could not start watching: " + e1.getMessage(), e1);
			}

		});

	}

	public void watchFile(String filename, String name, boolean analyzeCompleteFile, boolean executeAction,
			int limitCount, int limitMinutes, List<IPStatPattern> patterns) throws Exception {

		File watchedFile = new File(filename);
		if (!watchedFile.exists()) {
			throw new Exception("The File " + filename + " doesn't exist");
		}

		IPHourStat stat = new IPHourStat(name, limitCount, limitMinutes, executeAction);
		LogTailer tailer = new LogTailer(watchedFile, analyzeCompleteFile);
		tailer.addFileListener(new FileListener(stat, patterns));

		Thread logTailer = new Thread(tailer);
		logTailer.start();

		logger.info("Watching: " + filename);

	}

	public static void main(String[] args) {
		LogDog dog = new LogDog();

		try {

			if (args.length < 1) {
				throw new Exception("Wrong syntax: java -jar logdog.jar /path/to/config.xml");
			}

			dog.startConfiguration(args[0]);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

}
