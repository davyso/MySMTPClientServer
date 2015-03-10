import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
	
	public static void main(String[] args) throws IOException{
		
		// Terminate program if no arguments or given an argument that doesn't exist
		File file = null;
		if(args.length==0 || !(file = new File("outgoing")).exists()){
			System.out.println("QUIT");
			System.exit(0);
		}

		String outputData = "";
		
		// Initialize readers for file and standard output
		BufferedReader outgoing = new BufferedReader(new FileReader(file));		
				
		System.out.println("Client ready for input");

		// Initialize state machine
		int state = MAIL_FROM;

		// Create client socket with connection to server at port 6789
		Socket clientSocket = new Socket(args[0], Integer.parseInt(args[1]));

					
		// Create output stream attached to socket
		DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			
		// Create (buffered) input stream attached to socket
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));			
		
		
		// Receive 220 msg from server and send HELO msg
		String serverGreet = inFromServer.readLine();
		String domainName = InetAddress.getLocalHost().getCanonicalHostName();
		
		if(serverGreet.matches("(220|220 )" + ".*") && domainName.matches(DOMAIN)){
			outToServer.writeBytes("HELO " + domainName + "\n");
		}
		
		String userLine = outgoing.readLine();

		String serverResponse;
		if(!responseOK(serverResponse = inFromServer.readLine())){System.exit(0);}
		
		// While loop to read and handle multiple input lines
		while(userLine != null){

			String message = "";
			
			// -------------
			// State 0
			// -------------		
			if(state==MAIL_FROM && userLine.matches("From: <" + ".+" + ">")){
								
				// Create and send MAIL FROM cmd
				String revPath = extractPath(userLine);
				outputData = "MAIL FROM: <" + revPath + ">";
				outToServer.writeBytes(outputData + '\n');
				System.out.println("SENT TO SERVER: " + outputData);
			}
			
			
			// -------------
			// State 1
			// -------------					
			if(state==RCPT_TO/* && userLine.matches("To: <" + ".+" + ">")*/){
				
				if(userLine.matches("To: <" + ".+" + ">")){
				// new
				state--;
				
				// Create and send RCPT TO cmd
				String fwdPath = extractPath(userLine);				
				outputData = "RCPT TO: <" + fwdPath + ">";
				outToServer.writeBytes(outputData + '\n');
				System.out.println("SENT TO SERVER: " + outputData);
				}else{
					state++;
				}
			}			


			// -------------
			// State 2
			// -------------
			if(state==DATA){
				
				// Create and send DATA cmd
				outputData = "DATA";
				outToServer.writeBytes(outputData + '\n');
				System.out.println("SENT TO SERVER: " + outputData);
				
				// Check SMTP Response
				serverResponse = inFromServer.readLine();
				System.out.println("FROM SERVER: " + serverResponse);
				if(!responseOK(serverResponse)) break;

				// Like for DATA state in SMTP1, use a loop to take in and store
				// message
				while(userLine != null){
					if(userLine.matches("From: <" + ".+" + ">")){
						break;
					}else{
						message = message + userLine + "\n";
						userLine = outgoing.readLine();
					}
				}
				
				// Tidy up and close message
				message = message.substring(0,message.length()-1);
				outputData = message;
				outToServer.writeBytes(message);
				
				outputData = "\n.\n";
				outToServer.writeBytes(outputData);
			}
			
			// Check SMTP Response
			serverResponse = inFromServer.readLine();
			System.out.println("FROM SERVER: " + serverResponse);
			if(!responseOK(serverResponse)) break;
			
			// Prepare for next state and read next client input
			state = (state<2)? (state+1): 0;
			userLine = outgoing.readLine();	
		}
		
		// Tells server that it had quit and to wait for next connection
		outToServer.writeBytes("QUIT\n");
		clientSocket.close();
		System.out.println("QUIT");
	}
	
	
	// Constants
	static final int MAIL_FROM = 0;
	static final int RCPT_TO = 1;
	static final int DATA = 2;
	
	// Grammar for SMTP
	static final String STRING = "[\\p{ASCII}&&[^( )<>\\(\\)\\[\\]\\\\.,;:\\@\"]]+";
	static final String ELEMENT = "\\p{Alpha}" + "\\p{Alnum}+";
	static final String LOCALPART = STRING;
	static final String DOMAIN = ELEMENT + "(." + ELEMENT + ")*";
	static final String MAILBOX = LOCALPART + "@" + DOMAIN;
	
	
	// Pre-condition: Takes in the "From:" line
	// Extracts the reverse path from this line.
	private static String extractPath(String userLine){
		String[] tokens = userLine.split("<|>");
		return tokens[1];
	}
	
	// Checks to see if SMTP response is valid
	private static boolean responseOK(String stderr){
		if((stderr.matches("(250|250 )" + ".*")) || (stderr.matches("(354|354 )" + ".*"))){
			return true;
		}else{
			return false;
		}
	}

}
