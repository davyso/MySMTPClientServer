
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class JServer {
	
	public static void main(String[] args) throws IOException{
		
		String clientSentence;
		String capitalizedSentence;
		
		ServerSocket welcomeSocket = new ServerSocket(6789);
		
		System.out.println("Server Ready for Connection");
		
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
		
			// Read input line from socket
			clientSentence = inFromClient.readLine();
			
			System.out.println("Client sent: " + clientSentence);
			capitalizedSentence = clientSentence.toUpperCase() + '\n';
			
			//Write output line to socket
			outToClient.writeBytes(capitalizedSentence);
			connectionSocket.close();
			
		}
		
	}


}
