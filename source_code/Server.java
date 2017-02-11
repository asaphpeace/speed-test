import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

	public static final int TEST_PACKET_SIZE = 2 * 1024 * 1024; // 2MB
	public static final int INIT_PACKET_SIZE = 10 * 1024; // 10KB
	public static final int HEADS_UP = 11;
	public static final int UPLOAD_ACK = 41;
	public static Logger logger;

	public static void main(String[] args) throws Exception {
		logger = Logger.getLogger(Server.class.getName());
		// get server socket and server port from user
		InetAddress server_addr = InetAddress.getByName(args[0]);
		int server_port = Integer.parseInt(args[1]);
		// start server socket
		ServerSocket server = new ServerSocket(server_port, 0, server_addr);
		logger.info("Server is up!");

		while (true) {
			Socket socket = server.accept();
			logger.info("Client " + socket.getInetAddress() + " is connected!");
			OutputStream output = socket.getOutputStream();
			InputStream input = socket.getInputStream();

			// create test packet
			byte[] packet = createPacket(TEST_PACKET_SIZE);
			// overcome TCP's slow start
			getPastSlowStart(input, output);
			// start servicing client download test
			logger.info("servicing client's download test");
			int test_status = serviceClientDownloadTest(input, output, packet);
			if (test_status == -1) {
				logger.log(Level.SEVERE, "client download test failed!");
			}
			// start servicing client upload test
			serviceClientUploadTest(input, output, packet);
			logger.info("SpeedTest done!");
			// close everything
			output.close();
			input.close();
			socket.close();
			logger.info("-------------------------------------------------------");
		}
	}

	// Get past TCP's slow-start by sending and receiving a small packet (10 KB)
	public static void getPastSlowStart(InputStream input, OutputStream output) throws IOException {
		// receive init_packet for ramping up TCP beyond slow start
		byte[] init_packet = new byte[INIT_PACKET_SIZE];
		int offset = 0;
		int size = input.read(init_packet, offset, init_packet.length);
		while (size > 0) {
			offset += size;
			size = input.read(init_packet, offset, init_packet.length - offset);
		}
		// send init_packet for ramping up TCP beyond slow start
		output.write(init_packet);
	}

	// Creates a packet of 'size' bytes filled with random data
	public static byte[] createPacket(int size) {
		byte[] packet = new byte[size];
		for (int i = 0; i < size; i++) {
			packet[i] = (byte) Math.random();
		}
		return packet;
	}

	public static int serviceClientDownloadTest(InputStream input, OutputStream output, byte[] packet)
			throws IOException {
		// wait for heads-up from client
		if (input.read() != HEADS_UP) {
			System.out.println("Didn't get heads up from client...");
			return -1;
		}
		logger.info("Received heads up from client. Starting to push test packet into network...");
		// push packet into network
		output.write(packet);
		logger.info("Test packet pushed into the network");
		return 0;
	}

	public static void serviceClientUploadTest(InputStream input, OutputStream output, byte[] packet)
			throws IOException {

		// Start Receiving from client for upload test
		int offset = 0;
		int size = input.read(packet, offset, packet.length);
		int total_received = size;
		while (size > 0) {
			// logger.info("packet download size: " + size);
			offset += size;
			size = input.read(packet, offset, packet.length - offset);
			total_received += size;
		}
		logger.info("packet received from client. downloaded size: " + total_received / (1024 * 1024) + " MB");

		logger.info("Sending client Upload-Ack to indicate upload finished");
		// Send client UPLOAD_ACK
		output.write((byte) UPLOAD_ACK);
		logger.info("Upload-Ack packet sent");
	}

}
