package za.ac.sun.cs.green.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.util.Configuration;

/**
 * GreenServer - Standalone constraint solving server with Z3 model extraction.
 *
 * Protocol v2 (model extraction):
 * - Client sends JSON representation of Green Expression
 * - Server responds with JSON: {"sat":true/false,"model":{"var1":value1,...}}
 *
 * Legacy protocol fallback:
 * - Single char response: '1' (SAT), '0' (UNSAT), 'E' (Error)
 *
 * Special commands: "QUIT" (shutdown), "CLOSE" (disconnect)
 */
public class GreenServer {

	private static Green green = null;
	private static Green greenModel = null;
	private static Logger log = null;
	private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("DEBUG", "true"));

	public static void main(String[] args) {
		initializeSolvers();

		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		BufferedReader input = null;
		PrintStream output = null;

		int port = Integer.parseInt(System.getProperty("SATPort", "9408"));

		try {
			serverSocket = new ServerSocket(port);
			log.info("GreenServer started on port " + port + " with Z3 model extraction support");

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
						output.println("OK");
						try { input.close(); } catch (IOException x) { log.log(Level.SEVERE, "input.close() failed", x); }
						try { clientSocket.close(); } catch (IOException x) { log.log(Level.SEVERE, "clientSocket.close() failed", x); }
						break;
					}
					if (query.equals("CLOSE")) {
						log.info("Closing the client connection");
						output.println("OK");
						try { input.close(); } catch (IOException x) { log.log(Level.SEVERE, "input.close() failed", x); }
						try { clientSocket.close(); } catch (IOException x) { log.log(Level.SEVERE, "clientSocket.close() failed", x); }
						break;
					}
					// Process query and return JSON with model
					String response = processWithModel(query);
					output.println(response);
					output.flush();
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

	/**
	 * Initialize Green solvers for SAT checking and model extraction.
	 */
	private static void initializeSolvers() {
		// Initialize SAT solver (simple config without slicer to avoid null expression issues)
		green = new Green("greenserver-sat");
		log = green.getLog();

		try {
			Properties satProps = new Properties();
			satProps.setProperty("green.services", "sat");
			// Simplified: canonize -> z3 (no slicer to avoid null expression)
			satProps.setProperty("green.service.sat", "(canonize z3java)");
			satProps.setProperty("green.service.sat.canonize", "za.ac.sun.cs.green.service.canonizer.SATCanonizerService");
			satProps.setProperty("green.service.sat.z3java", "za.ac.sun.cs.green.service.z3.SATZ3JavaService");
			satProps.setProperty("green.z3java.timeout", "5000");

			Configuration satConfig = new Configuration(green, satProps);
			satConfig.configure();
			log.info("SAT solver configured with Z3 Java (no slicer)");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to configure SAT solver: " + e.getMessage(), e);
		}

		// Initialize Model solver for variable value extraction (direct to Z3)
		greenModel = new Green("greenserver-model");

		try {
			Properties modelProps = new Properties();
			modelProps.setProperty("green.services", "model");
			// Direct: z3javamodel only (simplest config for model extraction)
			modelProps.setProperty("green.service.model", "z3javamodel");
			modelProps.setProperty("green.service.model.z3javamodel", "za.ac.sun.cs.green.service.z3.ModelZ3JavaService");
			modelProps.setProperty("green.z3java.timeout", "5000");

			Configuration modelConfig = new Configuration(greenModel, modelProps);
			modelConfig.configure();
			log.info("Model solver configured with Z3 Java model extraction (direct)");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to configure model solver: " + e.getMessage(), e);
			greenModel = null;
		}
	}

	/**
	 * Process query with Z3 model extraction.
	 * Returns JSON response with model values.
	 */
	@SuppressWarnings("unchecked")
	private static String processWithModel(String query) {
		log.info("QUERY (JSON): " + query.substring(0, Math.min(100, query.length())) + "...");

		try {
			// Parse JSON to Expression
			Expression expression = parseJsonExpression(query);

			if (expression == null) {
				log.warning("Failed to parse JSON expression");
				return "{\"sat\":false,\"error\":\"parse_error\"}";
			}

			if (DEBUG) {
				log.info("Parsed expression: " + expression);
			}

			// Try model extraction first (gets actual variable values from Z3)
			if (greenModel != null) {
				try {
					Instance modelInstance = new Instance(greenModel, null, expression);
					Object result = modelInstance.request("model");

					if (result != null && result instanceof Map) {
						Map<Variable, Object> model = (Map<Variable, Object>) result;

						if (DEBUG) {
							log.info("Z3 returned model with " + model.size() + " variables");
						}

						// Build JSON response with model values
						StringBuilder json = new StringBuilder();
						json.append("{\"sat\":true,\"model\":{");

						boolean first = true;
						for (Map.Entry<Variable, Object> entry : model.entrySet()) {
							if (!first) json.append(",");
							first = false;

							String varName = entry.getKey().getName();
							Object value = entry.getValue();

							json.append("\"").append(escapeJson(varName)).append("\":");

							if (value == null) {
								json.append("null");
							} else if (value instanceof String) {
								json.append("\"").append(escapeJson((String) value)).append("\"");
							} else if (value instanceof Boolean) {
								json.append(value.toString());
							} else {
								// Numeric value
								json.append(value.toString());
							}
						}

						json.append("}}");

						if (DEBUG) {
							log.info("Returning JSON model: " + json);
						}

						return json.toString();
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Model extraction failed, falling back to SAT: " + e.getMessage());
					if (DEBUG) {
						e.printStackTrace();
					}
				}
			}

			// Fallback to SAT check only (no model values)
			Instance satInstance = new Instance(green, null, expression);
			Boolean isSat = (Boolean) satInstance.request("sat");

			log.info("SAT result: " + isSat);

			if (isSat != null && isSat) {
				// SAT but no model values available
				return "{\"sat\":true,\"model\":{}}";
			} else {
				return "{\"sat\":false}";
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to process query", e);
			return "{\"sat\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
		}
	}

	/**
	 * Escape JSON string value.
	 */
	private static String escapeJson(String s) {
		if (s == null) return "";
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			switch (c) {
				case '"': sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				default: sb.append(c);
			}
		}
		return sb.toString();
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
