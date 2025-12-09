package za.ac.sun.cs.green.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.util.Configuration;

public class GreenServer {

	private static Green green = null;

	private static Logger log = null;

	public static void main(String[] args) {
		green = new Green("greenserver");
		log = green.getLog();
		
		// Configure Z3 SAT solver
		try {
			Properties props = new Properties();
			props.setProperty("green.services", "sat");
			props.setProperty("green.service.sat", "z3");
			props.setProperty("green.service.sat.z3", "za.ac.sun.cs.green.service.z3.SATZ3JavaService");
			Configuration config = new Configuration(green, props);
			config.configure();
			log.info("Green server configured with Z3 solver");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to configure Z3 solver", e);
			return;
		}
		
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		BufferedReader input = null;
		PrintStream output = null;
		try {
			serverSocket = new ServerSocket(9408);
			boolean isRunning = true;
			while (isRunning) {
				log.info("Waiting for a client to connect...");
				clientSocket = serverSocket.accept();
				log.info("Connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getLocalPort());
				input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				output = new PrintStream(clientSocket.getOutputStream());
				while (clientSocket.isConnected()) {
					String query = input.readLine();
					if ((query == null) || query.equals("QUIT")) {
						isRunning = false;
						log.info("Closing the client connection and shutting down");
						output.print("OK");
						output.close();
						try {
							input.close();
						} catch (IOException x) {
							log.log(Level.SEVERE, "input.close() failed", x);
						}
						try {
							clientSocket.close();
						} catch (IOException x) {
							log.log(Level.SEVERE, "clientSocket.close() failed", x);
						}
						break;
					}
					if (query.equals("CLOSE")) {
						log.info("Closing the client connection");
						output.print("OK");
						output.close();
						try {
							input.close();
						} catch (IOException x) {
							log.log(Level.SEVERE, "input.close() failed", x);
						}
						try {
							clientSocket.close();
						} catch (IOException x) {
							log.log(Level.SEVERE, "clientSocket.close() failed", x);
						}
						break;
					}
					output.print(process(query));
				}
			}
		} catch (IOException x) {
			log.log(Level.SEVERE, x.getMessage(), x);
//		} catch (KleeParseException x) {
//			log.log(Level.SEVERE, x.getMessage(), x);
		} finally {
			output.close();
			try {
				input.close();
			} catch (IOException x) {
				log.log(Level.SEVERE, "input.close() failed", x);
			}
			try {
				clientSocket.close();
			} catch (IOException x) {
				log.log(Level.SEVERE, "clientSocket.close() failed", x);
			}
			try {
				serverSocket.close();
			} catch (IOException x) {
				log.log(Level.SEVERE, "serverSocket.close() failed", x);
			}
		}
	}

	private static char[] process(String query) {
		log.info("QUERY: " + query.substring(0, Math.min(100, query.length())) + "...");
		try {
			// Decode Base64-encoded serialized Expression
			byte[] data = Base64.getDecoder().decode(query);
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Expression expression = (Expression) ois.readObject();
			ois.close();
			
			log.info("Parsed expression: " + expression);
			
			Instance i = new Instance(green, null, expression);
			Boolean r = (Boolean) i.request("sat");
			
			log.info("SAT result: " + r);
			
			if ((r != null) && r) {
				return new char[] { '1' };
			} else {
				return new char[] { '0' };
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to process query", e);
			return new char[] { 'E' }; // Error indicator
		}
	}

}
