package org.openhab.binding.serialbus.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public enum RawCommand {
	GET_TEMPERATURE(0x21, 4), //
	GET_TARGET_TEMPERATURE(0x22, 4), //
	SET_TARGET_TEMPERATURE(0x23, 2), //
	// depreciated: SET_DEFAULT_TARGET_TEMPERATURE(0x24, 2),
	SWITCH_CONTROL(0x25, 2), //
	GET_VALVE(0x26, 4), //
	SET_VALVE(0x27, 2), //
	GET_DELAY(0x28, 4), //
	SET_DELAY(0x29, 2), //
	SET_DEFAULT_DELAY(0x2A, 2), //
	RESET(0x2B, 0), //
	GET_CHANGE_RATE(0x2C, 2), //
	STORE_TARGET_EEPROM(0x2D, 2), //
	GET_VALVE_MIN(0x2E, 4), //
	SET_VALVE_MIN(0x2F, 2), //
	GET_VALVE_MAX(0x30, 4), //
	SET_VALVE_MAX(0x31, 2), //
	STORE_VALVE_EEPROM(0x32, 2), //
	SET_TEMP_CALIB_CYCLES(0x33, 2), //
	GET_TEMP_CALIB_CYCLES(0x34, 6), //
	SET_TEMP_CALIB_OFFSET(0x35, 2), //
	GET_TEMP_CALIB_OFFSET(0x36, 6), //
	STORE_TEMP_CALIB(0x37, 2), //
	;
	private static final byte[] EMPTY = new byte[0];
	private static final byte COMMAND_FROM_HOST = (byte) 0xFF;
	private final byte m_commandByte;
	private final int m_returnBytes;

	private RawCommand(final int commandByte, final int returnBytes) {
		m_commandByte = (byte) commandByte;
		m_returnBytes = returnBytes;
	}

	public byte[] execute(final SerialBusSocket port, final int number,
			final byte... bytes) throws PortShutDownException, IOException,
			PortTimeoutException {
		if (bytes.length > 250) {
			throw new IllegalArgumentException("Too long command "
					+ Arrays.toString(bytes));
		}
		final InputStream inputStream = port.getInputStream();
		try {
			int available;
			while ((available = inputStream.available()) > 0) {
				inputStream.skip(available);
				System.out.println("skipped " + available);
			}
			final OutputStream outputStream = port.getOutputStream();
			try {
				outputStream.write(COMMAND_FROM_HOST);
				outputStream.write(number);
				outputStream.write(m_commandByte);
				outputStream.write((byte) bytes.length);
				for (final byte b : bytes) {
					outputStream.write(b);
				}
			} finally {
				outputStream.close();
			}
			if (this == RESET) {
				// special case, do not expect RESET to return anything
				return EMPTY;
			}
			final int expectO = getExpectedByte(port, inputStream, "O");
			final int expectK = getExpectedByte(port, inputStream, "K");
			if (expectO != 'O' || expectK != 'K') {
				if (expectO == 'X' && expectK == 'X') {
					final int expectCR = getExpectedByte(port, inputStream,
							"CR");
					final int expectLF = getExpectedByte(port, inputStream,
							"LF");
					if (expectCR == 0x0d && expectLF == 0x0a) {
						// XX<CR><LF>: timeout on remote side
						throw new PortTimeoutException();
					}
					throw new RuntimeException("Garbage read after XX: 0x"
							+ Integer.toHexString(expectCR) + ", 0x"
							+ Integer.toHexString(expectLF) + ".");
				}
				throw new RuntimeException("Garbage read (instead of OK): 0x"
						+ Integer.toHexString(expectO) + ", 0x"
						+ Integer.toHexString(expectK) + ".");
			}
			final byte[] result = new byte[m_returnBytes];
			for (int complete = 0; complete < result.length;) {
				if (port.isClosed()) {
					throw new PortShutDownException("port closed while reading");
				}
				final int read = inputStream.read(result, complete,
						result.length - complete);
				if (read < 0) {
					throw new RuntimeException(
							"Unexpected short read (expected: " + result.length
							+ ", got: " + complete + " from port >>"
							+ Arrays.toString(result) + "<<.");
				}
				if (read == 0) {
					throw new PortTimeoutException();
				}
				complete += read;
			}
			final int expectCR = getExpectedByte(port, inputStream, "CR");
			final int expectLF = getExpectedByte(port, inputStream, "LF");
			if (expectCR != 0x0d || expectLF != 0x0a) {
				throw new RuntimeException(
						"Invalid termination characters read from port result is >>"
								+ Arrays.toString(result)
								+ "<<, followed by 0x"
								+ Integer.toHexString(expectCR) + ", 0x"
								+ Integer.toHexString(expectLF) + ".");
			}
			if ((available = inputStream.available()) > 0) {
				final StringBuilder details = new StringBuilder();
				details.append(">>");
				for (int i = 0; i < available; i++) {
					details.append("0x");
					details.append(Integer.toHexString(inputStream.read()));
					details.append(" ");
				}
				details.append("<<, (own result was " + Arrays.toString(result)
						+ ").");
				throw new RuntimeException(
						"Trailing characters after successfull read: "
								+ details);
			}
			if (m_returnBytes > 0) {
				return result;
			}
			return EMPTY;
		} catch (final SocketTimeoutException e) {
			throw new PortTimeoutException();
		} finally {
			inputStream.close();
		}
	}

	private int getExpectedByte(final SerialBusSocket port,
			final InputStream inputStream, final String text)
					throws PortShutDownException, IOException {
		if (port.isClosed()) {
			throw new PortShutDownException("port closed while reading");
		}
		final int expectCR = inputStream.read();
		if (expectCR < 0) {
			throw new RuntimeException("Unexpected short read (missing " + text
					+ ").");
		}
		return expectCR;
	}

	public static double byteToTemperature(final byte[] buff) {
		final int rawTemp = Integer.parseInt(new String(buff));
		return 0.2 * rawTemp;
	}

}
