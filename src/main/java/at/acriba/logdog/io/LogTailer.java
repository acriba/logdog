package at.acriba.logdog.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.acriba.logdog.FileListener;

public class LogTailer implements Runnable {
	
	private static Logger logger = LoggerFactory.getLogger(LogTailer.class);
	
	protected boolean _running = true;
	protected int updateInterval = 500;
	protected File watchedFile;
	protected long filePointer = 0;
	protected List<FileListener> listeners = new ArrayList<>();
	
	public LogTailer(File watchFile, boolean analyzeCompleteFile) {
		this.watchedFile = watchFile;
		if(!analyzeCompleteFile) this.filePointer = watchedFile.length();
	}
	
	public void addFileListener(FileListener listener) {
		this.listeners.add(listener);
	}
	
	public void stop() {
		this._running = false;
	}
	
	public void run() {
	    try {
	        while (_running) {
	            Thread.sleep(updateInterval);
	            long len = watchedFile.length();
	            if (len < filePointer) {
	                // Log must have been jibbled or deleted.
	                logger.info("Log file was reset. Restarting logging from start of file.");
	                filePointer = len;
	            }
	            else if (len > filePointer) {
	                // File must have had something added to it!
	            	RandomAccessReader raf = new RandomAccessReader(watchedFile, RandomAccessReader.DEFAULT_BUFFER_SIZE, false);
	            	//BufferedRandomAccessFile raf = new BufferedRandomAccessFile(watchedFile, "r");
	                raf.seek(filePointer);
	                
	                while( true ) {
	                	final String line = raf.readLine();
	                	if (line == null) break;
	                	this.listeners.forEach( l -> l.processline(line));
	                }
	                filePointer = raf.getFilePointer();
	                raf.close();
	            }
	        }
	    }
	    catch (Exception e) {
	    	logger.error("Fatal error reading log file, log tailing has stopped.", e);
	    }
	    // dispose();
	}

}
