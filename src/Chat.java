import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
/*****************************//**
* \brief It implements a distributed chat. 
* It creates a ring and delivers messages
* using flooding 
**********************************/
public class Chat {

/*
   Json Messages:
 
  {
        "type" :  "JOIN",
        "parameters" :
               {   
                    "myAlias" : string,
                    "myPort"  : number
               }
   }
 
   {
        "type" :  "ACCEPT",
        "parameters" :
               {   
                   "ipPred"    : string,
                   "portPred"  : number
               }
    }
 
    {
         "type" :  "LEAVE",
         "parameters" :
         {
             "ipPred"    : string,
             "portPred"  : number
         }
    }

   {
         "type" :  "Put",
        "parameters" :
         {
             "aliasSender"    : string,
             "aliasReceiver"  : string,
             "message"        : string
        }
   }
 
 {
        "type" :  "NEWSUCCESSOR",
        "parameters" :
        {
            "ipSuccessor"    : string,
            "portSuccessor"  : number
        }
 }
 */

	// My info
	public static String myAlias;
	public static int myPort;
	public static String myIp;
	// Successor
	public static String ipSuccessor;
	public static int portSuccessor;
	// Predecessor
	public static String ipPredecessor;
	public static int portPredecessor;

	    
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
							JSONObject accept_message = createAcceptMsg(ipPredecessor, portPredecessor);
							oos.writeObject(accept_message.toString());
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

		// Creates a JSON Message for accepting a join
		private JSONObject createAcceptMsg(String ip_pred, int port_pred){
			JSONObject  obj = new JSONObject();

			try{
				obj.put("type","ACCEPT");
				Map<String, Object> t = new HashMap<>();
				t.put("ipPred", ip_pred);
				t.put("portPred", port_pred);
				obj.put("parameters",t);

			}catch(JSONException e){
				e.printStackTrace();
			}

			return obj;
		}
	}//end Server class

/*new class - client ================================================================================================================*/
    
/*****************************//*
* \brief It implements the client
**********************************/
  //private class Client implements Runnable {       
 	private static class Client implements Runnable {       
		
		private Scanner scan;
		
		private ObjectOutputStream oos = null;
		private ObjectInputStream ois = null; 

		public Client(){
			scan  = new Scanner(System.in);
		}

	/*****************************//**
	* \brief It allows the user to interact with the system. 
	**********************************/    
		public void run(){
			joinPort(myIp, myPort, myAlias);

			while (true) {

				try {
					// Basic User Interface to send messages to the server
					System.out.println("Send a message:\n 1) JOIN\n 2) CREATE\n 3) LEAVE");
					int user_option = scan.nextInt();
					scan.nextLine(); //consumes return char

					switch(user_option){
						case 1:
							System.out.println("Enter your Alias: ");
							String alias = scan.nextLine();
							System.out.println("Enter the port you would like to join: ");
							int port = scan.nextInt();
							
							joinPort(myIp, port, alias);
							break;
						case 2:
							// Connect to your own server socket
							joinPort(myIp, myPort, myAlias);

							break;	
						case 3:
							// Leave the room
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
		
		private void joinPort(String ip, int port, String alias){
			try{
				Socket socket = new Socket(ip, port);
				System.out.println("Client connected to server: "+socket.getRemoteSocketAddress());

				// Create streams
				oos = new ObjectOutputStream(socket.getOutputStream());
				ois = new ObjectInputStream(socket.getInputStream());

				JSONObject joinobj = createJoinMsg(alias, myPort);
				oos.writeObject(joinobj.toString());

				// Wait for Server Response
				String server_response = ois.readObject().toString();

				//Parse message from Server
				JSONObject server_message = new JSONObject(server_response);
				String type = server_message.getString("type");
				System.out.println("Server Sends " + type + " message");

				if(type.equals("ACCEPT")){
					// update successors
					ipSuccessor = ip;
					portSuccessor = port;

					// parse params from the message to get new predecessors
					JSONObject parameters = server_message.getJSONObject("parameters");
					ipPredecessor = parameters.getString("ipPred");
					portPredecessor = parameters.getInt("portPred");
				}
				else
					System.out.println("Could not join the server");

				socket.close();

			}catch(IOException e){
				e.printStackTrace();
			}catch (ClassNotFoundException e){
				e.printStackTrace();
			}catch (JSONException e){
				e.printStackTrace();
			}
		}

		// Creates a JSON Message to join a chat
		private JSONObject createJoinMsg(String alias, int port){
			JSONObject  obj = new JSONObject();

			try{
				obj.put("type","JOIN");
				Map<String, Object> t = new HashMap<String, Object>();
				t.put("myAlias", alias);
				t.put("myPort", port);
				obj.put("parameters",t);

			}catch(JSONException e){
				e.printStackTrace();
			}

			return obj;
		}
	}
  
  
/*****************************//**
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
		this.portPredecessor = myPort;
		this.portSuccessor = myPort;

		// Initialization of the peer
		Thread server = new Thread(new Server());
		Thread client = new Thread(new Client());
		server.start();
		client.start();
		try {
			client.join();
			server.join();
		} catch (InterruptedException e)
		{
			// Handle Exception
		}
	}

	public static void main(String[] args) throws JSONException {

		if (args.length < 2 ) {
			throw new IllegalArgumentException("Parameter: <myAlias> <myPort>");
		}

		Chat chat = new Chat(args[0], Integer.parseInt(args[1]));

//		Chat chat = new Chat("127.0.0.1",555);

		// https://www.codevoila.com/post/65/java-json-tutorial-and-example-json-java-orgjson
		/*String json = "";
		ArrayList<String> types = new ArrayList<String>();
		JSONArray jArray = (JSONArray) new JSONTokener(json).nextValue(); //creates JSON OBJECT array
		for(int x = 0; x < jArray.length(); x++) {
		  JSONObject object = jArray.getJSONObject(x);
		  types.add(object.getString("type"));
		}*/

	}
	

}





// mutex example

/*private final Lock _mutex = new ReentrantLock(true);

_mutex.lock();

// your protected code here

_mutex.unlock();*/

