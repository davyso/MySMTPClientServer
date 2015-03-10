import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	
	public static void main(String[] args) throws IOException{
		
		int state = MAIL_FROM;
		ArrayList<String> rcptList = new ArrayList<String>();
		String revPath = "";
		String fwdPath = "";
				
		ServerSocket welcomeSocket = new ServerSocket(Integer.parseInt(args[0]));
				
		//While loop to handle arbitrary sequence of clients of clients making requests
		while(true){
			
			//Waits for some client to connect and creates new socket for connection 
			Socket connectionSocket = welcomeSocket.accept();
			System.out.println("Client Made Conection");		
			
			// Create (buffered) input stream attached to connection socket
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
					connectionSocket.getInputStream()));
		
			// Create output stream attached to connectoin socket
			DataOutputStream outToClient = new DataOutputStream(
					connectionSocket.getOutputStream());

			
			String responseMsg = "";
				
			String msgHELO = "220 cs.unc.edu";
			outToClient.writeBytes(msgHELO + "\n");
			
			// Acknowledge HELO message from client
			String ack = inFromClient.readLine();
			if(!ack.matches("(HELO|HELO )" + ".*")){break;}
				
			// Echo and send to client
			responseMsg = "250 OK\n";		
			System.out.println("SEND TO CLIENT: " + responseMsg);
			outToClient.writeBytes(responseMsg);
				
			while(true){
			
				// Read input line from socket
				String cmdLine = inFromClient.readLine();
				
				// State machines doesn't terminate unless cmdLine is null or QUIT
				if(cmdLine == null || cmdLine.equals("QUIT")) break;
				System.out.println("Client sent: " + cmdLine);
				cmdLine = cmdLine.trim();

		
				// ----------------
				// State 0
				// ----------------
				if(state == MAIL_FROM){
					
					// We first check to see if the input follows the structure of the <mail-from-cmd>
					// token: "MAIL"<sp>"FROM:"<reverse-path><CRLF>
					if(cmdLine.matches("MAIL" +" "+ "FROM:" + ".+")){
						String path = extractPath(cmdLine);
						
						// Check the structure of the <path> token: "<" <mailbox> ">"
						if (path.matches("<" + MAILBOX + ">")){
							
							// Echo and send to client
							responseMsg = "250 OK\n";		
							System.out.println("SEND TO CLIENT: " + responseMsg);
							outToClient.writeBytes(responseMsg);
														
							revPath = path.substring(1,path.length()-1);
							state = RCPT_TO;
							continue;
						}else{
							responseMsg = "501 Syntax error in parameters or arguments\n";
							System.out.println(responseMsg);
							outToClient.writeBytes(responseMsg);
						}

					}
					else if(cmdLine.matches("RCPT TO:" + ".+") || (cmdLine.equals("DATA"))){
						responseMsg = "503 Bad Sequence of commands\n";
						System.out.println(responseMsg);
						outToClient.writeBytes(responseMsg);
					}
					else{
						responseMsg = "500 Syntax error: command unrecognized\n";
						System.out.println(responseMsg);
						outToClient.writeBytes(responseMsg);
					}
				}
				
				// ----------------
				// State 1
				// ----------------	
				if (state == RCPT_TO){
					
					if(cmdLine.equals("DATA") && rcptList.size()>0){
						state = DATA;
					}else if(cmdLine.matches("RCPT" +" "+ "TO:" + ".+")){
						
						String path = extractPath(cmdLine);
					
						// Check the structure of the <path> token: "<" <mailbox> ">"
						if (path.matches("<" + MAILBOX + ">")){
							
							// Echo and send to client
							responseMsg = "250 OK\n";		
							System.out.println("SEND TO CLIENT: " + responseMsg);
							outToClient.writeBytes(responseMsg);

							
							fwdPath = path.substring(1,path.length()-1);
							rcptList.add(fwdPath);
							continue;

						}else{
							responseMsg = "501 Syntax error in parameters or arguments\n";
							System.out.println(responseMsg);
							outToClient.writeBytes(responseMsg);

						}	
					}
					else if(cmdLine.matches("MAIL FROM:" + ".+") || (cmdLine.equals("DATA") && rcptList.size()==0)){
						responseMsg = "503 Bad Sequence of commands\n";
						System.out.println(responseMsg);
						outToClient.writeBytes(responseMsg);

					}else{
						responseMsg = "500 Syntax error: command unrecognized\n";
						System.out.println(responseMsg);
						outToClient.writeBytes(responseMsg);

					}
				}	

				
				// ----------------
				// State 2
				// ----------------
				// PRE-CONDITIONS: Data input is not null
				if (state == DATA){
					responseMsg = "354 Start mail input; end with <CRLF>.<CRLF>\n";
					outToClient.writeBytes(responseMsg);
					String message = "";
					String messageLine = "";
					
					// This loop takes in, echoes and stores every new line of input the user
					// tpes in.
					while(true){			
						messageLine = inFromClient.readLine();
						System.out.println(messageLine);
						if(messageLine.equals(".")) {break;}
						message = message + messageLine + "\r\n";
					}
					message = message.substring(0,message.length()-1);
					
					writeMails(revPath, rcptList, message);
											
					// Reset state machine
					responseMsg = "250 OK\n";
					outToClient.writeBytes(responseMsg);

					state = MAIL_FROM;
					rcptList.clear();
				}	
			}
			
			
			// End connection with client, allowing the server to be open for next client
			connectionSocket.close();
			
		}		
	}
	
	
	// -----------------
	// Constants
	// -----------------	
	
	// Definitions for states
	static final int MAIL_FROM = 0;
	static final int RCPT_TO = 1;
	static final int DATA = 2;
	
	// Grammar for SMTP
	static final String STRING = "[\\p{ASCII}&&[^( )<>\\(\\)\\[\\]\\\\.,;:\\@\"]]+";
	static final String ELEMENT = "\\p{Alpha}" + "\\p{Alnum}+";
	static final String LOCALPART = STRING;
	static final String DOMAIN = ELEMENT + "(." + ELEMENT + ")*";
	static final String MAILBOX = LOCALPART + "@" + DOMAIN;

	
	// -----------------
	// Helper Methods
	// -----------------
		
	// Returns the path from a valid command line
	public static String extractPath(String cmdLine){
			
		String[] cmdTokens = cmdLine.split(":", 2);
		// cmdTokens[0] should contain everything behind the first ":"
		// cmdTokens[1] should contain everything after the first ":"

		// Get rid of any unnecessary white space at the ends of the path token.
		return cmdTokens[1].trim();
	}
	
	
	// Returns the domain from a valid path
	public static String extractDomain(String path){
			
		String[] cmdTokens = path.split("@", 2);
		// cmdTokens[0] should contain everything behind the first "@"
		// cmdTokens[1] should contain everything after the first "@"

		// Get rid of any unnecessary white space at the ends of the path token.
		return cmdTokens[1].trim();
	}

	
	// Writes emails using the sender's address, the addresses of recipients, and message
	public static void writeMails(String revPath, ArrayList<String> rcptList, String message) 
			throws IOException{
		
		// This loop iterates and grabs each recipient in the list and
		// concatenates them into one long string, containing newlines
		// between each recipient.
		String recipients = "";
		for(int i=0; i<rcptList.size(); i++){
			recipients = recipients + "To: <" + rcptList.get(i) + ">\r\n";
		}		
		
		// This loop creates a file for each recipient, named after each
		// forward path. The file contains a header with the reverse path
		// and the various forward paths. This is followed by the body,
		// which contains the user's message.
		for(int i=0; i<rcptList.size(); i++){
			String rcpt = rcptList.get(i);
			String filename = extractDomain(rcpt);
			
			File file = new File("forward/" + filename);
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			
			// If file does not already exist, make a new file. Else, append to 
			// file with same name.				
			bw.write("From: <" + revPath + ">\r\n");
			bw.write("To: <" + rcpt + ">\r\n");
			bw.write(message + "\n");
			bw.close();
		}

	}

}
