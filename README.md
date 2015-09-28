logdog: ACRIBA Logfile scanner
=====================================

Comparable with fail2ban but based on Java and optimized for high performance. 
Log scans log files (e.g. /var/log/apache/access_log) and bans IPs that show 
the malicious signs -- too many password failures, seeking for exploits, etc.

Requires JDK 1.8 or higher.

How to use
--------------

1. Make sure that JAVA 8 is installed on your server
2. Copy build/logdog.jar into /opt/logdog/
3. Copy build/config.xml into /opt/logdog/ and update the configuration to your needs.
4. Copy logdog into /etc/init.d/. You might have to update the paths in the file.
5. Start the service with service logdog start
