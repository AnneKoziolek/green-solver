package za.ac.sun.cs.green.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.util.Configuration;

/**
 * GreenServer - Standalone constraint solving server using JSON protocol.
 *
 * Protocol:
 * - Client sends JSON representation of Green Expression
 * - Server responds with single char: '1' (SAT), '0' (UNSAT), 'E' (Error)
 * - Special commands: "QUIT" (shutdown), "CLOSE" (disconnect)
 */
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
			log.info("Green server configured with Z3 solver (JSON protocol)");
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
					if (query == null) {
						log.info("Client disconnected, waiting for next connection");
						try { input.close(); } catch (IOException x) { }
						try { clientSocket.close(); } catch (IOException x) { }
						break;
					}
					if (query.equals("QUIT")) {
						isRunning = false;
						log.info("Received QUIT - shutting down server");
						output.print("OK");
						output.close();
						try { input.close(); } catch (IOException x) { log.log(Level.SEVERE, "input.close() failed", x); }
						try { clientSocket.close(); } catch (IOException x) { log.log(Level.SEVERE, "clientSocket.close() failed", x); }
						break;
					}
					if (query.equals("CLOSE")) {
						log.info("Closing the client connection");
						output.print("OK");
						output.close();
						try { input.close(); } catch (IOException x) { log.log(Level.SEVERE, "input.close() failed", x); }
						try { clientSocket.close(); } catch (IOException x) { log.log(Level.SEVERE, "clientSocket.close() failed", x); }
						break;
					}
					output.print(process(query));
				}
			}
		} catch (IOException x) {
			log.log(Level.SEVERE, x.getMessage(), x);
		} finally {
			if (output != null) output.close();
			try { if (input != null) input.close(); } catch (IOException x) { log.log(Level.SEVERE, "input.close() failed", x); }
			try { if (clientSocket != null) clientSocket.close(); } catch (IOException x) { log.log(Level.SEVERE, "clientSocket.close() failed", x); }
			try { if (serverSocket != null) serverSocket.close(); } catch (IOException x) { log.log(Level.SEVERE, "serverSocket.close() failed", x); }
		}
	}

	private static char[] process(String query) {
		log.info("QUERY (JSON): " + query.substring(0, Math.min(100, query.length())) + "...");
		try {
			// Parse JSON to Expression
			Expression expression = parseJsonExpression(query);

			if (expression == null) {
				log.warning("Failed to parse JSON expression");
				return new char[] { 'E' };
			}

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
			return new char[] { 'E' };
		}
	}

	// ==================== JSON Parser ====================

	private static Expression parseJsonExpression(String json) {
		if (json == null || json.equals("null")) {
			return null;
		}
		return parseExpression(json.trim(), new int[]{0});
	}

	private static Expression parseExpression(String json, int[] pos) {
		skipWhitespace(json, pos);
		if (pos[0] >= json.length()) return null;

		if (json.charAt(pos[0]) != '{') {
			throw new RuntimeException("Expected '{' at position " + pos[0]);
		}
		pos[0]++;

		String type = null;
		String op = null;
		String name = null;
		Object value = null;
		Expression left = null;
		Expression right = null;
		Expression operand = null;

		while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
			skipWhitespace(json, pos);
			String key = parseString(json, pos);
			skipWhitespace(json, pos);
			expect(json, pos, ':');
			skipWhitespace(json, pos);

			switch (key) {
				case "type":
					type = parseString(json, pos);
					break;
				case "op":
					op = parseString(json, pos);
					break;
				case "name":
					name = parseString(json, pos);
					break;
				case "value":
					value = parseValue(json, pos);
					break;
				case "left":
					left = parseExpression(json, pos);
					break;
				case "right":
					right = parseExpression(json, pos);
					break;
				case "operand":
					operand = parseExpression(json, pos);
					break;
				case "repr":
					parseString(json, pos);
					break;
				default:
					skipValue(json, pos);
			}

			skipWhitespace(json, pos);
			if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
				pos[0]++;
			}
		}

		if (pos[0] < json.length() && json.charAt(pos[0]) == '}') {
			pos[0]++;
		}

		if (type == null) return null;

		switch (type) {
			case "binary":
				Operation.Operator binOp = Operation.Operator.valueOf(op);
				return new BinaryOperation(binOp, left, right);
			case "unary":
				Operation.Operator unOp = Operation.Operator.valueOf(op);
				return new UnaryOperation(unOp, operand);
			case "intvar":
				return new IntVariable(name, Integer.MIN_VALUE, Integer.MAX_VALUE);
			case "realvar":
				return new RealVariable(name, Double.MIN_VALUE, Double.MAX_VALUE);
			case "strvar":
				return new StringVariable(name);
			case "intconst":
				return new IntConstant(((Number) value).intValue());
			case "realconst":
				return new RealConstant(((Number) value).doubleValue());
			case "strconst":
				return new StringConstant((String) value);
			default:
				log.warning("Unknown expression type: " + type);
				return null;
		}
	}

	private static void skipWhitespace(String json, int[] pos) {
		while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
			pos[0]++;
		}
	}

	private static void expect(String json, int[] pos, char c) {
		if (pos[0] >= json.length() || json.charAt(pos[0]) != c) {
			throw new RuntimeException("Expected '" + c + "' at position " + pos[0]);
		}
		pos[0]++;
	}

	private static String parseString(String json, int[] pos) {
		skipWhitespace(json, pos);
		if (json.charAt(pos[0]) != '"') {
			throw new RuntimeException("Expected '\"' at position " + pos[0]);
		}
		pos[0]++;

		StringBuilder sb = new StringBuilder();
		while (pos[0] < json.length() && json.charAt(pos[0]) != '"') {
			char c = json.charAt(pos[0]);
			if (c == '\\' && pos[0] + 1 < json.length()) {
				pos[0]++;
				char escaped = json.charAt(pos[0]);
				switch (escaped) {
					case 'n': sb.append('\n'); break;
					case 't': sb.append('\t'); break;
					case 'r': sb.append('\r'); break;
					case '"': sb.append('"'); break;
					case '\\': sb.append('\\'); break;
					default: sb.append(escaped);
				}
			} else {
				sb.append(c);
			}
			pos[0]++;
		}
		pos[0]++;
		return sb.toString();
	}

	private static Object parseValue(String json, int[] pos) {
		skipWhitespace(json, pos);
		char c = json.charAt(pos[0]);

		if (c == '"') {
			return parseString(json, pos);
		} else if (c == '-' || Character.isDigit(c)) {
			return parseNumber(json, pos);
		} else if (c == 't' || c == 'f') {
			return parseBoolean(json, pos);
		} else if (c == 'n') {
			pos[0] += 4;
			return null;
		}
		return null;
	}

	private static Number parseNumber(String json, int[] pos) {
		int start = pos[0];
		boolean isDouble = false;

		if (json.charAt(pos[0]) == '-') pos[0]++;
		while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
			pos[0]++;
		}
		if (pos[0] < json.length() && json.charAt(pos[0]) == '.') {
			isDouble = true;
			pos[0]++;
			while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
				pos[0]++;
			}
		}
		if (pos[0] < json.length() && (json.charAt(pos[0]) == 'e' || json.charAt(pos[0]) == 'E')) {
			isDouble = true;
			pos[0]++;
			if (pos[0] < json.length() && (json.charAt(pos[0]) == '+' || json.charAt(pos[0]) == '-')) {
				pos[0]++;
			}
			while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) {
				pos[0]++;
			}
		}

		String numStr = json.substring(start, pos[0]);
		if (isDouble) {
			return Double.parseDouble(numStr);
		} else {
			return Long.parseLong(numStr);
		}
	}

	private static Boolean parseBoolean(String json, int[] pos) {
		if (json.substring(pos[0]).startsWith("true")) {
			pos[0] += 4;
			return true;
		} else if (json.substring(pos[0]).startsWith("false")) {
			pos[0] += 5;
			return false;
		}
		return null;
	}

	private static void skipValue(String json, int[] pos) {
		skipWhitespace(json, pos);
		char c = json.charAt(pos[0]);

		if (c == '"') {
			parseString(json, pos);
		} else if (c == '{') {
			int depth = 1;
			pos[0]++;
			while (pos[0] < json.length() && depth > 0) {
				c = json.charAt(pos[0]);
				if (c == '{') depth++;
				else if (c == '}') depth--;
				else if (c == '"') {
					parseString(json, pos);
					continue;
				}
				pos[0]++;
			}
		} else if (c == '[') {
			int depth = 1;
			pos[0]++;
			while (pos[0] < json.length() && depth > 0) {
				c = json.charAt(pos[0]);
				if (c == '[') depth++;
				else if (c == ']') depth--;
				else if (c == '"') {
					parseString(json, pos);
					continue;
				}
				pos[0]++;
			}
		} else {
			parseValue(json, pos);
		}
	}
}
