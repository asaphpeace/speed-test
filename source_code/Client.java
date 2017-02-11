import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

public class Client {

	public static final int TEST_PACKET_SIZE = 2 * 1024 * 1024; // 2MB
	public static final int INIT_PACKET_SIZE = 10 * 1024; // 10KB
	public static final int HEADS_UP = 11;
	public static final int UPLOAD_ACK = 41;
	public static Logger logger;

	public static void main(String[] args) throws Exception {
		logger = Logger.getLogger(Client.class.getName());

		// get server socket and server port from user
		InetAddress server_addr = InetAddress.getByName(args[0]);
		int server_port = Integer.parseInt(args[1]);

		// create and connect a socket to the server
		Socket socket = new Socket(server_addr, server_port);
		logger.info("Client connected to the server!");
		InputStream input = socket.getInputStream();
		OutputStream output = socket.getOutputStream();

		getPastSlowStart(input, output);
		long download_speed = downloadTest(input, output);
		long upload_speed = uploadTest(input, output);

		input.close();
		output.close();
		socket.close();
		// Result
		System.out.println("Speed Test summary\nDownload Speed: " + download_speed + " MBits/sec\nUpload Speed: "
				+ upload_speed + " MBits/sec\n");
	}

	// Get past TCP's slow-start by sending and receiving a small packet (10 KB)
	public static void getPastSlowStart(InputStream input, OutputStream output) throws IOException {
		// create a 10 KB packet
		byte[] init_packet = new byte[INIT_PACKET_SIZE];
		init_packet = createPacket(INIT_PACKET_SIZE);

		// send init_packet to make sure you are out of tcp's slow start
		// (upload)
		output.write(init_packet);

		// receive a 10 KB packet from the server to make sure you are out of
		// slow start (download)
		int offset = 0;
		int size = input.read(init_packet, offset, init_packet.length);
		while (size > 0) {
			offset += size;
			size = input.read(init_packet, offset, init_packet.length - offset);
		}
	}

	// Creates a packet of 'size' bytes filled with random data
	public static byte[] createPacket(int size) {
		byte[] packet = new byte[size];
		for (int i = 0; i < size; i++) {
			packet[i] = (byte) Math.random();
		}
		return packet;
	}

	// Download Test
	public static long downloadTest(InputStream input, OutputStream output) throws IOException {
		// send server a heads-up
		output.write(HEADS_UP);
		logger.info("Begin download test...");

		// read packets sent by server and record time
		byte[] test_packet = new byte[TEST_PACKET_SIZE];
		int offset = 0;
		long start_time = System.currentTimeMillis();
		int size = input.read(test_packet, offset, TEST_PACKET_SIZE);
		long total_received = size;
		while (size > 0) {
			// logger.info("packet download size: " + size);
			offset = offset + size;
			size = input.read(test_packet, offset, TEST_PACKET_SIZE - offset);
			total_received += size;
		}
		long end_time = System.currentTimeMillis();
		// for connections too fast (local sever)
		if ((end_time - start_time) == 0) {
			end_time = start_time + 1;
		}
		logger.info("time: " + (end_time - start_time));
		long download_speed = (total_received * 1000 * 8) / ((end_time - start_time) * 1024 * 1024); // MBits/sec
		logger.info("test-packet download size: " + total_received / (1024 * 1024) + " MB");
		// logger.info("download speed = " + download_speed);
		return download_speed;

	}

	public static long uploadTest(InputStream input, OutputStream output) throws IOException {
		// Upload Test
		byte[] test_packet = new byte[TEST_PACKET_SIZE];
		// create a 10MB packet
		test_packet = createPacket(test_packet.length);
		// send packet to client and record time
		logger.info("Begin Upload Test...");
		long start_time = System.currentTimeMillis();
		output.write(test_packet);
		// wait for Upload ACK from server
		int flag = input.read();
		if (flag != UPLOAD_ACK) {
			logger.info("Didn't get Upload ACK from server.");
			System.exit(0);
		}
		long end_time = System.currentTimeMillis();

		// for connections too fast (local sever)
		if ((end_time - start_time) == 0) {
			end_time = start_time + 1;
		}
		logger.info("time: " + (end_time - start_time));
		long upload_speed = ((long) (test_packet.length) * 1000 * 8) / ((end_time - start_time) * 1024 * 1024); // MBits/sec
		logger.info("test-packet upload size: " + test_packet.length / (1024 * 1024) + " MB");
		return upload_speed;
	}

}
