/*
 * @(#)CControl2pachube.java	1.0 09/02/05 Fb
 *
 * based on 
 * SimpleRead.java	1.12 98/06/25 SMI
 * 
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license
 * to use, modify and redistribute this software in source and binary
 * code form, provided that i) this copyright notice and license appear
 * on all copies of the software; and ii) Licensee does not utilize the
 * software in a manner which is disparaging to Sun.
 * 
 * This software is provided "AS IS," without a warranty of any kind.
 * ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND
 * ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
 * LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THE
 * SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS
 * BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES,
 * HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING
 * OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * This software is not designed or intended for use in on-line control
 * of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and
 * warrants that it will not use or redistribute the Software for such
 * purposes.
 * 
 * Change History V1.0 Fb 09/02/05
 *   Compared to SimpleRead.java  
 * - Adapting Com-Port for Windows "COM1"
 * - Changing to gnu.io Libriary instead of sun javax.com 
 * - Removed Bug from reading buffer
 * - Added processing library
 * - Added eeml library
 * - Added pachube connection code
 * - Added C-Control connection code
 */

import java.io.*;
import java.util.*;
import gnu.io.*;//rxtx serial comm lib also part of processing package
import processing.core.PApplet; // processing package required by eeml
import eeml.*; // eeml for http/xml interface to pachube web service

/**
 * Class declaration
 * CControl2pachube provides an interface for ASCII data at 
 * the serial port to the web service of www.pachube.com.
 * This class assumes a string format for the date-time stamp 
 * and sensor datas input and send this sensor data to a 
 * specific pachube feed. Pachube is updated manually, by uploading 
 * data every time when a full data set is received at the serial port.
 * 
 * 
 * Input at Serial Port COM1 9600, databits 8,stoppbits 1, parity none
 *   dd.mm.yy hh:mm:ss temperature humidity pressure<CR><LF> 
 *   dd = day of month
 *   mm = month of year
 *   yy = year
 *   hh = hour
 *   mm = minute
 *   ss = second
 *   temperature = xx.x value in °C
 *   humidity = xx value relative %
 *   pressure = (x)xxx value hecto Pascal
 * 
 * Output
 *   http://www.pachube.com/api/1269.xml 
 *     tag0: temperature in Celsius
 *     tag1: humidity in Percent
 *     tag2: pressure in hecto Pascal
 *   
 * @author Fb
 * @version 1.0, 09/02/05
 */
public class CControl2pachube implements Runnable, SerialPortEventListener{
    static CommPortIdentifier portId;
    static Enumeration	      portList;
    InputStream		      inputStream;
    SerialPort		      serialPort;
    String readResult;
    String dateTimeStamp;
    Thread		      readThread;
	PApplet parent;
    DataOut pachubeDataOut;
    float temperature; 
    float humidity;
    float pressure;
    

    /**
     * Method declaration
     * Main program called from operating system.
     * Default port is COM1. You can change the port
     * with the command line parameter in args
     * with e.g. "COM2"
     * @param args
     *	serial port for data input 
     * @see
     */
public static void main(String[] args) {
    boolean		      portFound = false;
    String		      defaultPort = "COM1";
    
    System.out.println("CControl2pachube V1.0");
    
 	if (args.length > 0) {
	    defaultPort = args[0];
	} 
   
	portList = CommPortIdentifier.getPortIdentifiers();

	while (portList.hasMoreElements()) {
	    portId = (CommPortIdentifier) portList.nextElement();
	    if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
			if (portId.getName().equals(defaultPort)) {
			    System.out.println("Found port: "+defaultPort);
			    portFound = true;
			    CControl2pachube reader = new CControl2pachube();
			} 
	    } 
	} 
	if (!portFound) {
	    System.out.println("port " + defaultPort + " not found.");
	} 
} 

    /**
     * Constructor declaration
     * Initializes pachube service and serial port
     *
     * @see
     */
 public CControl2pachube(){

    pachubeDataOut = new DataOut(parent, "http://www.pachube.com/api/1269.xml", "e7d4c1504210d45e2f293014057f8b97e08148317c79a09a0b3eadf087a25e18");   
    
    pachubeDataOut.addData(0,"temperature", 15, +34);
    pachubeDataOut.setUnits(0, "Celsius","C","derivedSI");
    temperature = 20;  
    pachubeDataOut.addData(1,"humidity", 10, 79);
    pachubeDataOut.setUnits(1, "Percent","%","contextDependentUnits");
    humidity = 30; 
    pachubeDataOut.addData(2,"pressure", 950, 1049);
    pachubeDataOut.setUnits(2, "hecto Pascal","h Pa","derivedSI");
    pressure = 1000; 
   
    readResult = "";
    dateTimeStamp = "";
    
    try {
	    serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
	} catch (PortInUseException e) {}

	try {
	    inputStream = serialPort.getInputStream();
	} catch (IOException e) {}

	try {
	    serialPort.addEventListener(this);
	} catch (TooManyListenersException e) {}

	serialPort.notifyOnDataAvailable(true);

	try {
	    serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, 
					   SerialPort.STOPBITS_1, 
					   SerialPort.PARITY_NONE);
	} catch (UnsupportedCommOperationException e) {}

	readThread = new Thread(this);

	readThread.start();
    }

    /**
     * Method declaration
     * 
     *
     * @see
     */
public void run() {
	try {
	    Thread.sleep(20000);
	} catch (InterruptedException e) {}
    } 

    /**
     * Method declaration
     * Call back function for serial port event.
     * Data is read from serial port until a full
     * line is completed. Then the ASCII input is parsed 
     * and prepared to send to pachube server.
     * @param event
     *	type of serial port event
     * @see
     */
public void serialEvent(SerialPortEvent event) {
    	int numBytes;
    	int year, month, date, hourOfDay, minute, second;
	    byte[] readBuffer;
	    String[] readLines;
	    String[] splitString;
	    String tmpDateTimeStamp;
   	
    	switch (event.getEventType()) {	
		case SerialPortEvent.BI:	
//      Break interrupt.
		case SerialPortEvent.OE:	
//      Overrun error.
		case SerialPortEvent.FE:	
//      Framing error.
		case SerialPortEvent.PE:	
//      Parity error.
		case SerialPortEvent.CD:	
//	    Carrier detect.
		case SerialPortEvent.CTS:	
//      Clear to send.
		case SerialPortEvent.DSR:	
//      Data set ready.
		case SerialPortEvent.RI:	
//      Ring indicator.    	
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
		    break;
	
		case SerialPortEvent.DATA_AVAILABLE:

	
		    try {
		    readBuffer = new byte[0];
		    numBytes = inputStream.available();
			if (numBytes > 0) {				
			    readBuffer = new byte[numBytes];
			    numBytes = inputStream.read(readBuffer);
			    readResult = readResult + new String(readBuffer);
			} 
			System.out.print("CControl: ");
			System.out.println(readResult);
		    if (readResult.contains("\r\n")){
		    	// end of line reached
		    	// parsing string date \s time \s temp \s hyd \s bar $
		    	readLines = readResult.split("\r\n");
		    	readResult = readLines[readLines.length-1];
		    	readLines = readResult.split("\\s");
		    	readResult="";
		    	if (readLines.length == 5){
		    		dateTimeStamp = readLines[0]+" "+readLines[1];
		    	    // string format: "date.month.year houfOfDay:minute:second"
		    	    tmpDateTimeStamp = dateTimeStamp.replace('.', ' ');
		    	    tmpDateTimeStamp = tmpDateTimeStamp.replace(':', ' ');
		    	    splitString = tmpDateTimeStamp.split("\\s");
		    		date = Integer.valueOf(splitString[0]);
		    		month = Integer.valueOf(splitString[1]);
		    		year = 2000+Integer.valueOf(splitString[2]);
		    		hourOfDay = Integer.valueOf(splitString[3]);
		    		minute = Integer.valueOf(splitString[4]);
		    		second = Integer.valueOf(splitString[5]);
		    		temperature = Float.valueOf(readLines[2]).floatValue();
		    		humidity = Float.valueOf(readLines[3]).floatValue();
		    		pressure = Float.valueOf(readLines[4]).floatValue();
			    	sendPachube();
		    	}
		    }
		    } catch (IOException e) {}
		    break;
		}
    }
   
/**
 * Method declaration
 *  Sends private data temperature, humidity, pressaure to the pachube
 *  server by calling the eeml library.
 *  If pachube service received data the return code "200" is given
 *  in the console. Otherwise the return code indicates stand http error 
 * @see
 */
public void sendPachube() {
	int response;

	pachubeDataOut.update(0, temperature);
    pachubeDataOut.update(1, humidity);
    pachubeDataOut.update(2, pressure);
    response = pachubeDataOut.updatePachube(); // updatePachube() updates by an authenticated PUT HTTP request
    System.out.println("pachube: " + response); // should be 200 if successful; 401 if unauthorized; 404 if feed doesn't exist
}

}




