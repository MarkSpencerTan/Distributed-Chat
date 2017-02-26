import java.net.*;
import java.io.*;
import java.util.*;

import org.json.*;

import java.util.concurrent.locks.*;
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
				servSock = new ServerSocket(6666);
				System.out.println("Server running on port "+servSock.getLocalPort());
				
				while (true) {
					clntSock = servSock.accept(); // Get client connections

					//Create a new thread to handle the connection
					System.out.println("Server connected to port "+clntSock.getLocalPort());

					//Create io streams
					ObjectInputStream  ois = new ObjectInputStream(clntSock.getInputStream());
					ObjectOutputStream oos = new ObjectOutputStream(clntSock.getOutputStream());

					System.out.println("Message from client: "+ois.readObject().toString());
					oos.writeObject("\nHi Client, Server got the message\n");   //test message

					// close streams
					oos.close();
					ois.close();

					clntSock.close();
				}
				
			}catch(IOException e){
				e.printStackTrace();
			}catch(ClassNotFoundException e){
				e.printStackTrace();
			}
		}//end server run
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

		private InetAddress ip = null;
		private Socket socket = null;

		public Client(){
			scan  = new Scanner(System.in);
		}

	/*****************************//**
	* \brief It allows the user to interact with the system. 
	**********************************/    
		public void run(){
			while (true) {
				try {
					// Connect to the server socket
					ip = InetAddress.getByName("127.0.0.1");
					socket = new Socket(ip,6666);
					System.out.println("Client Running on port " +socket.getLocalPort()+" and connected to server: "+socket.getRemoteSocketAddress());
					
					// Create streams
					oos = new ObjectOutputStream(socket.getOutputStream());
					ois = new ObjectInputStream(socket.getInputStream());

					// Basic User Interface to send messages to the server
					System.out.println("Send a message:\n 1) JOIN\n 2) CREATE\n 3) LEAVE");
					int useroption = scan.nextInt();
					scan.nextLine(); //consumes return char
					switch(useroption){
						case 1:
							System.out.println("Enter your Alias: ");
							String alias = scan.nextLine();
							System.out.println("Enter the port you would like to join: ");
							int port = scan.nextInt();
							
							oos = joinPort(ip, port);
							
							JSONObject joinobj = createJoinMsg(alias, port);
							oos.writeObject(joinobj.toString());
							
							break;
						case 2:
							// Create a standalone chat room
							break;	
						case 3:
							// Leave the room
							break;
					}

					System.out.println("server message: " + ois.readObject().toString());   // reads the response and parse it using JsonParser
					
					//socket.close();
					ois.close();
					oos.close();

				} catch (IOException e) {
					e.printStackTrace();
				} catch(ClassNotFoundException e){
					e.printStackTrace();
				}
			}
			/* TODO Use mutex to handle race condition when reading and writing the global variable (ipSuccessor, 
				portSuccessor, ipPredecessor, portPredecessor)*/
		}
		
		private static ObjectOutputStream joinPort(InetAddress ip, int port){
			Socket socket = new Socket(ip, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			System.out.println("Connecting to port "+port);
			
			return oos;
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

		/*      if (args.length < 2 ) {  
		  throw new IllegalArgumentException("Parameter: <myAlias> <myPort>");
		}

		Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
		*/
		Chat chat = new Chat("127.0.0.1",555);

		// https://www.codevoila.com/post/65/java-json-tutorial-and-example-json-java-orgjson
		/*String json = "";
		ArrayList<String> types = new ArrayList<String>();
		JSONArray jArray = (JSONArray) new JSONTokener(json).nextValue(); //creates JSON OBJECT array
		for(int x = 0; x < jArray.length(); x++) {
		  JSONObject object = jArray.getJSONObject(x);
		  types.add(object.getString("type"));
		}*/

	}
	
	// Creates a JSON Message to join a chat 
	public static JSONObject createJoinMsg(String alias, int port){
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





// mutex example

/*private final Lock _mutex = new ReentrantLock(true);

_mutex.lock();

// your protected code here

_mutex.unlock();*/

