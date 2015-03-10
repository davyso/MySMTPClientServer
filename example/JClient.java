
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class JClient {
	
	public static void main(String[] args) throws IOException{

		String sentence;
		String modifiedSentence;
		
		// Create (buffered) input stream using standard input
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("Client ready for input");
		
		// While loop to read and handle multiple input lines
		while((sentence = inFromUser.readLine()) != null){
			
			// Create client socket with connection to server at port 6789
			Socket clientSocket = new Socket("snapper.cs.unc.edu", 6789);
			
			// Create output stream attached to socket
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			
			// Create (buffered) input stream attached to socket
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
		
			// Write line to server
			outToServer.writeBytes(sentence + '\n');
			
			// Read line from server
			modifiedSentence = inFromServer.readLine();
			
			System.out.println("FROM SERVER: " + modifiedSentence);
			
			clientSocket.close();
		}
		
		
	}
}
