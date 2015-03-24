package org.uibk.iis.robotprojectapp;

import java.util.ArrayList;

import android.app.Activity;
import android.hardware.usb.UsbManager;
import jp.ksksue.driver.serial.FTDriver;
/// todo: make this class threadsafe...
public class ComDriver {
	private FTDriver com;
	
	public ComDriver(int baudrate, Activity owner) {
		com = new FTDriver((UsbManager) owner.getSystemService(android.content.Context.USB_SERVICE));
		connect(baudrate);
	}

	/**
	 * @todo should throw an exception
	 * @param baudrate
	 */
	public void connect(int baudrate) {
		// TODO implement permission request

		if (com.begin(baudrate)) {
			//textLog.append("connected\n");
		} else {
			//textLog.append("could not connect\n");
		}
	}

	public void disconnect() {
		com.end();
	}
	public boolean isConnected() {
		return com.isConnected();
	}
	/**
	 * transfers given bytes via the serial connection.
	 * @todo should throw an exception
	 * @param data
	 */
	public void comWrite(byte[] data) {
		if (com.isConnected()) {
			com.write(data);
		}
	}

	/**
	 * reads from the serial buffer. due to buffering, the read command is
	 * issued 3 times at minimum and continuously as long as there are bytes to
	 * read from the buffer. Note that this function does not block, it might
	 * return an empty string if no bytes have been read at all.
	 * 
	 * @return buffer content as string
	 */
	public String comRead() {
		String s = "";
		int i = 0;
		int n = 0;
		while (i < 3 || n > 0) {
			byte[] buffer = new byte[256];
			n = com.read(buffer);
			s += new String(buffer, 0, n);
			i++;
		}
		return s;
	}
	/**
	 * reads from the serial buffer. due to buffering, the read command is
	 * issued 3 times at minimum and continuously as long as there are bytes to
	 * read from the buffer. Note that this function does not block, it might
	 * return an empty string if no bytes have been read at all.
	 * 
	 * @return buffer content as binary char array
	 */
	public ArrayList<Byte> comReadBin() {
		ArrayList<Byte> b = new ArrayList<Byte>();
		int i = 0;
		int n = 0;
		while (i < 3 || n > 0) {
			byte[] buffer = new byte[256];
			n = com.read(buffer);
			for(int j = 0; j < n; j++) 
				b.add(buffer[j]);
			i++;
		}
		return b;
	}
	/**
	 * write data to serial interface, wait 50 ms and read answer.
	 * 
	 * @param data
	 *            to write
	 * @return answer from serial interface
	 */
	public String comReadWrite(byte[] data) {
		com.write(data);
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// ignore
		}
		return comRead();
	}
	public ArrayList<Byte> comReadBinWrite(byte[] data) {
		com.write(data);
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// ignore
		}
		return comReadBin();
	}
}
