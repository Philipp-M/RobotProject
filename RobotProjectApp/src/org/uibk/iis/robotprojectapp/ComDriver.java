package org.uibk.iis.robotprojectapp;

import java.util.ArrayList;

import android.app.Activity;
import android.hardware.usb.UsbManager;
import jp.ksksue.driver.serial.FTDriver;

/// todo:	make this class threadsafe...
///			throw exceptions if it isn't initialized
public class ComDriver {
	// Singleton for easy access, only allowed to use in one Activity
	private static final class InstanceHolder {
		static final ComDriver INSTANCE = new ComDriver();
	}

	private ComDriver() {
	}

	public static ComDriver getInstance() {
		return InstanceHolder.INSTANCE;
	}

	// this method has to be called first otherwise the other methods won't work
	public void init(Activity activity, int baudrate) {
		com = new FTDriver((UsbManager) activity.getSystemService(android.content.Context.USB_SERVICE));
		connect(baudrate);
	}

	private FTDriver com;

	// public ComDriver(int baudrate, Activity owner) {
	// com = new FTDriver((UsbManager)
	// owner.getSystemService(android.content.Context.USB_SERVICE));
	// connect(baudrate);
	// }

	/**
	 * @todo should throw an exception
	 * @param baudrate
	 */
	public void connect(int baudrate) {
		// TODO implement permission request
		if (com != null) {
			if (com.begin(baudrate)) {
				// textLog.append("connected\n");
			} else {
				// textLog.append("could not connect\n");
			}
		}
	}

	public void disconnect() {
		if (com != null)
			com.end();
	}

	public boolean isConnected() {
		if (com != null)
			return com.isConnected();
		else
			return false;
	}

	/**
	 * transfers given bytes via the serial connection.
	 * 
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
		if (com != null) {
			int i = 0;
			int n = 0;
			while (i < 3 || n > 0) {
				byte[] buffer = new byte[256];
				n = com.read(buffer);
				s += new String(buffer, 0, n);
				i++;
			}
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
		if (com != null) {
			int i = 0;
			int n = 0;
			while (i < 3 || n > 0) {
				byte[] buffer = new byte[256];
				n = com.read(buffer);
				for (int j = 0; j < n; j++)
					b.add(buffer[j]);
				i++;
			}
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
		if (com != null) {
			com.write(data);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore
			}
			return comRead();
		} else
			return "";
	}

	public ArrayList<Byte> comReadBinWrite(byte[] data) {
		if (com != null) {
			com.write(data);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore
			}
			return comReadBin();
		} else
			return new ArrayList<Byte>();
	}
}
