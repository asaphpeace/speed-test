# speed-test
A simple server client in Java which tests the network download and upload speed.

Source code is in the folder named: source_code
Jar executables are in the folder named: executables

Running the client and server:
(Assuming Java 7 or higher is present on the machine)

	Note: First Run the Server

	Server:

		java -jar server_executable.java <server_ip_address> <server_port>

		<server_ip_address> : the ip address on which the server listens
		<server_port> : port on which the server will bind to.

	Client:

		java -jar client_executable.java <server_ip_address> <server_port> > test_output

		<server_ip_address> : specify the server ip address
		<server_port> : specify server port

		test_output : contains the results of the test
