package org.openhab.binding.serialbus.internal;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class SerialBusSocket {
	private static Socket socket;
	private final Socket m_socket;
	private boolean m_closed = false;

	private static Socket createSocket(final String hostName, final int port)
			throws IOException {
		return new Socket(InetAddress.getByName(hostName), port);
	}

	SerialBusSocket(final String hostName, final int port) throws IOException {
		if (null == socket) {
			socket = createSocket(hostName, port);
		}
		m_socket = socket;
	}

	static void shutDownInternal() throws IOException {
		if (socket != null) {
			socket.close();
		}
		socket = null;
	}

	final void close() {
		if (m_closed) {
			return;
		}
		m_closed = true;
	}

	final void assertOpen() {
		if (m_closed) {
			throw new IllegalStateException("already closed");
		}
	}

	final OutputStream getOutputStream() throws IOException {
		assertOpen();
		return new FilterOutputStream(m_socket.getOutputStream()) {
			@Override
			public void close() throws IOException {
				// does not close the stream. It is only closed when the
				// port is closed.
				synchronized (SerialBusSocket.this) {
					if (isClosed()) {
						return;
					}
					flush();
				}
			}

			@Override
			public void flush() throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					super.flush();
				}
			}

			@Override
			public void write(final byte[] b, final int off, final int len)
					throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					super.write(b, off, len);
				}
			}

			@Override
			public void write(final byte[] b) throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					super.write(b);
				}
			}

			@Override
			public void write(final int b) throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					super.write(b);
				}
			}
		};
	}

	final InputStream getInputStream() throws IOException {
		assertOpen();
		return new FilterInputStream(m_socket.getInputStream()) {
			@Override
			public int available() throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					return super.available();
				}
			}

			@Override
			public void close() throws IOException {
				// does not close the stream. It is only closed when the
				// port is closed.
			}

			@Override
			public synchronized void mark(final int readlimit) {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					super.mark(readlimit);
				}
			}

			@Override
			public boolean markSupported() {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					return super.markSupported();
				}
			}

			@Override
			public int read() throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					return super.read();
				}
			}

			@Override
			public int read(final byte[] b, final int off, final int len)
					throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					return super.read(b, off, len);
				}
			}

			@Override
			public int read(final byte[] b) throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					return super.read(b);
				}
			}

			@Override
			public synchronized void reset() throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					super.reset();
				}
			}

			@Override
			public long skip(final long n) throws IOException {
				synchronized (SerialBusSocket.this) {
					assertOpen();
					return super.skip(n);
				}
			}
		};
	}

	final boolean isClosed() {
		return m_closed;
	}
}
