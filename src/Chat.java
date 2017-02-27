import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
/*****************************//**
* \brief It implements a distributed chat. 
* It creates a ring and delivers messages
* using flooding 
**********************************/
public class Chat {
	// My info
	private static String myAlias;
	private static int myPort;
	private static String myIp;
	// Successor
	private static String ipSuccessor;
	private static int portSuccessor;
	// Predecessor
	private static String ipPredecessor;
	private static int portPredecessor;

	    
  /*new class - Server===================================================================================================================*/

/*****************************//**
* \class Server class "chat.java" 
* \brief It implements the server
**********************************/ 
	private static class Server implements Runnable {
		private Socket clntSock = null;
		private ServerSocket servSock = null;
		
		public Server() {}
    
/*****************************//**
* \brief It allows the system to interact with the participants. 
**********************************/   
		public void run() {			
			try{
				//create the server socket
				servSock = new ServerSocket(myPort);
				System.out.println("Server running on port "+servSock.getLocalPort());
				
				while (true) {
					clntSock = servSock.accept(); // Get client connections

					//Create a new thread to handle the connection
					System.out.println("Server connected to client port: "+clntSock.getPort());

					//Create io streams
					ObjectInputStream  ois = new ObjectInputStream(clntSock.getInputStream());
					ObjectOutputStream oos = new ObjectOutputStream(clntSock.getOutputStream());

					//Parse message from client
					JSONObject client_message = new JSONObject(ois.readObject().toString());
					String type = client_message.getString("type");
					System.out.println("Server receives " + type + " message");

					//switch case based on the message type
					switch(type){
						case "JOIN":
							int client_port = client_message.getJSONObject("parameters").getInt("myPort");
							// Reply back with an ACCEPT message
							oos.writeObject(JSONMessage("ACCEPT", myAlias, portPredecessor).toString());
							portPredecessor = client_port;
							break;
						case "NEWSUCCESSOR":
							portSuccessor = client_message.getJSONObject("parameters").getInt("portSuccessor");
							oos.writeObject(JSONMessage("ACCEPT", myAlias, myPort).toString());
							break;
					}

					// close streams
					oos.close();
					ois.close();

					clntSock.close();
				}
			}catch(IOException e){
				e.printStackTrace();
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}catch(JSONException e){
				e.printStackTrace();
			}
		}//end server run
	}//end Server class

/*new class - client ================================================================================================================*/
    
	/******************************
	* \brief It implements the client
	**********************************/
 	private static class Client implements Runnable {
		
		private Scanner scan;
		
		private ObjectOutputStream oos = null;
		private ObjectInputStream ois = null; 

		public Client(){
			scan  = new Scanner(System.in);
		}

	/*******************************
	* \brief It allows the user to interact with the system. 
	**********************************/    
		public void run(){
			//joinPort(myIp, myPort, myAlias);

			while (true) {
				try {
					// Basic User Interface to send messages to the server
					System.out.println("\nSend a message:\n 1) PUT\n 2) JOIN\n 3) LEAVE\n 4) GET STATUS");
					int user_option = scan.nextInt();
					scan.nextLine(); //consumes return char

					switch(user_option){
						case 1:

							break;
						case 2:
							System.out.println("Enter your Alias: ");
							String alias = scan.nextLine();
							System.out.println("Enter the port you would like to join: ");
							int port = scan.nextInt();

							joinChat(myIp, port, alias);

							break;	
						case 3:
							// Leave the room
							break;
						case 4:
							getStatus();
							break;
					}

					if(oos != null && ois !=null){
						//socket.close();
						ois.close();
						oos.close();
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			/* TODO Use mutex to handle race condition when reading and writing the global variable (ipSuccessor, 
				portSuccessor, ipPredecessor, portPredecessor)*/
		}
		
		private void joinChat(String ip, int port, String alias){
			try{
				JSONObject join_msg = JSONMessage("JOIN", myAlias, myPort);
				JSONObject server_message = serverConnect(ip, port, join_msg);

				String type = server_message.getString("type");
				System.out.println("Server Sends " + type + " message");

				if(type.equals("ACCEPT")){
					// update successors
					ipSuccessor = ip;
					portSuccessor = port;

					// parse params from the message to get new predecessors
					JSONObject parameters = server_message.getJSONObject("parameters");
					int server_port_pred = parameters.getInt("portPred");
					String server_ip_pred = parameters.getString("ipPred");

					// Check that the predecessor we get is not 0/null
					if(server_port_pred != 0) {
						portPredecessor = server_port_pred;
						ipPredecessor = server_ip_pred;

						// send a NEWSUCCESSOR message to the predecessor
						JSONObject successor_msg = JSONMessage("NEWSUCCESSOR", myAlias, myPort);
						serverConnect(ipPredecessor, portPredecessor, successor_msg);
					}
				}
				else
					System.out.println("Could not join the server");

			}catch (JSONException e){
				e.printStackTrace();
			}
		}

		private JSONObject serverConnect(String ip, int port, JSONObject msg){
			JSONObject response = null;
			try{
				Socket socket = new Socket(ip, port);
				System.out.println("Client connected to server: "+socket.getRemoteSocketAddress());

				// Create streams
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());

				// Send message to server
				oos.writeObject(msg.toString());

				// Wait for Server Response
				String server_response = ois.readObject().toString();

				//Parse message from Server
				response = new JSONObject(server_response);

				socket.close();
			}catch(IOException e){
				e.printStackTrace();
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}catch(JSONException e){
				e.printStackTrace();
			}
			return response;
		}
	}
  
  
	/*******************************
	* Starts the threads with the client and server:
	* \param Id unique identifier of the process
	* \param port where the server will listen
	**********************************/
	public Chat(String myAlias, int myPort) {

		this.myAlias = myAlias;
		this.myPort = myPort;
		this.myIp = "127.0.0.1"; //set default to localhost for now
		this.ipPredecessor = myIp;
		this.ipSuccessor = myIp;
		this.portPredecessor = 0;
		this.portSuccessor = 0;

		// Initialization of the peer
		Thread server = new Thread(new Server());
		Thread client = new Thread(new Client());
		server.start();
		client.start();
		try {
			client.join();
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public static void getStatus(){
		System.out.println("\n====================");
		System.out.println("Alias: " + myAlias);
		System.out.println("Port: " + myPort);
		System.out.println("ipPredecessor: " + ipPredecessor);
		System.out.println("portPredecessor: " + portPredecessor);
		System.out.println("ipSuccessor: " + ipSuccessor);
		System.out.println("portSuccessor: " + portSuccessor);
		System.out.println("====================\n");
	}


	// Creates a JSON Message
	public static JSONObject JSONMessage(String type, String alias, int port, String... msgArgs){
		JSONObject  obj = new JSONObject();
		JSONObject params = new JSONObject();
		try {
			obj.put("type",type);
			obj.put("parameters", params);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		if (type.equals("JOIN")){
			try{
				params.put("myAlias", alias);
				params.put("myPort", port);
			}catch(JSONException e){
				e.printStackTrace();
			}
		}else if (type.equals("ACCEPT") || type.equals("LEAVE")){
			try{
				params.put("ipPred", myIp);
				params.put("portPred", port);
			}catch(JSONException e){
				e.printStackTrace();
			}
		}else if(type.equals("NEWSUCCESSOR")){
			try{
				params.put("ipSuccessor", myIp);
				params.put("portSuccessor", port);
			}catch(JSONException e){
				e.printStackTrace();
			}
		}else if(type.equals("PUT")){
			try{
				params.put("aliasSender", alias);
				params.put("aliasReceiver", msgArgs[0]);
				params.put("message", msgArgs[1]);
			}catch(JSONException e){
				e.printStackTrace();
			}
		}
		return obj;
	}


	public static void main(String[] args) throws JSONException {

		if (args.length < 2 ) {
			throw new IllegalArgumentException("Parameter: <myAlias> <myPort>");
		}

		Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
	}
	

}

// mutex example

/*private final Lock _mutex = new ReentrantLock(true);

_mutex.lock();

// your protected code here

_mutex.unlock();*/