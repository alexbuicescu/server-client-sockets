import java.net.*;
import java.util.HashMap;
import java.io.*;

public class MainServer extends Thread {
	private ServerSocket serverSocket;
	private static HashMap<String, ConnectedClient> connectedClients = new HashMap<>();

	public MainServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}

	public void run() {
		while (true) {
			try {
				
				System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
				//wait for a client to connect
				//MATERIE: SOCKET!!!
				Socket server = serverSocket.accept();
				
				//a client connected
				ConnectedClient connectedClient = new ConnectedClient(server);
				//start a new thread and continue waiting for a new client
				connectedClient.start();
				
			} catch (SocketTimeoutException s) {
				System.out.println("Socket timed out!");
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

	public static void main(String[] args) {
		int port = 8888;//Integer.parseInt(args[0]);
		try {
			//start the server
			Thread t = new MainServer(port);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ConnectedClient getConnectedClient(String clientName) {
		return connectedClients.get(clientName);
	}
	
	public static void addClient(ConnectedClient connectedClient) {
		//add the client to our list
		connectedClients.put(connectedClient.getClientName(), connectedClient);
		
		//send all the connected clients to everyone
		sendClients();
	}
	
	private static void sendClients() {

		String allClients = "";
		for(String clientName : connectedClients.keySet()) {
			allClients += clientName + " ";
		}
		//allClients will end with an extra space, so we remove it
		if (allClients.length() > 0) {
			allClients.substring(0, allClients.length() - 1);
		}
		
		//send the connected clients to all the clients so they can see who is online
		for(String clientName : connectedClients.keySet()) {
			ClientManager clientManager = new ClientManager(connectedClients.get(clientName), allClients);
			//start a new thread for each client so we don't block the main thread
			clientManager.start();
		}		
	}

	public static void removeClient(String clientName) {
		//remove the client from the list
		connectedClients.remove(clientName);
		System.out.println("disconnected client: " + clientName);
		//send all the connected clients to everyone
		sendClients();
	}
}

class ConnectedClient extends Thread {

	private boolean exit = false;
	private Socket clientSocket;
	private String clientName;
	
	public static final String ACTION_EXIT = "ACTION_EXIT";
	public static final String ACTION_SEND_TO = "ACTION_SEND_TO";
	public static final String ACTION_RECEIVE_FROM = "ACTION_RECEIVE_FROM";
	public static final String ACTION_CONNECTED = "ACTION_CONNECTED";
	public static final String ACTION_RECEIVE_CLIENTS = "ACTION_RECEIVE_CLIENTS";
	
	public ConnectedClient(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}
	
	public void run() {
		while (connectionOpened()) {
			//wait to receive a message from the client 
			MessageObject message = readClientMessage();
			//the client sent a message, it's time to interpret it
			manageMessage(message);
		}
		
		try {
			join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private MessageObject readClientMessage() {
		MessageObject message = new MessageObject("", "");
		try {
			//MATERIE: DESERIALIZARE!!!
			ObjectInputStream in = new ObjectInputStream(getClientSocket().getInputStream());
			//wait for the client to send a message
			message = (MessageObject)in.readObject();
		} catch (IOException e) {
			closeConnection();
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return message;
	}
	
	private void sendMessageToClient(MessageObject message) {
		try {
			//MATERIE: SERIALIZARE!!!
			ObjectOutputStream out = new ObjectOutputStream(getClientSocket().getOutputStream());
			//send a message to the client
			out.writeObject(message);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void manageMessage(MessageObject message) {
		System.out.println("manage message:\n" + clientName + "\n" + message.getMessageType() + "\n" + message.getMessage());
		if (message.getMessageType().equals(ACTION_EXIT)) {
			closeConnection();
		}
		else
			if(message.getMessageType().equals(ACTION_SEND_TO)) {
				/**
				 * received message from A to send to B:
				 * B hello => the_receiver_name your_message
				 * so if you split at " " (space), then the first item is the receiver name, and
				 * the rest is the message you want to send 
				 */
				//to who we want to send the message
				String destinatar = message.getMessage().split(" ")[0];
				
				/**
				 * substring(start, end) => returneaza stringul dintre start si end-1, ex: substr(1, 3) pentru "asdfgh" este "sd"
				 * (daca nu dai end-ul, atunci va lua stringul de la start pana la final)
				 * 
				 * indexOf(character) => returneaza pozitia pe care se afla un anumit caracter, sau returneaza -1 daca nu exista
				 * 
				 * so: we get the index of " " (because the message starts after the first space)
				 * after that, we do substring from that index to the end)
				 */
				//the message we want to send
				String mesaj = message.getMessage().substring(message.getMessage().indexOf(" ")+1);
				
				/**
				 * send the message to "destinatar"
				 * this means, "destinatar" will get an "ACTION_RECEIVE_FROM" type of message
				 * 
				 * the message will be: "the_current_client message_goes_here"
				 * 
				 * be aware that "the_current_client" is not the same as destinatar
				 * the_current_client = the one who sends the message (we get it using getClientName())
				 * destinatar = the one who receives the message
				 */
				MessageObject messageObject = new MessageObject(ACTION_RECEIVE_FROM, getClientName() + " " + mesaj);
				MainServer.getConnectedClient(destinatar).manageMessage(messageObject);
			}
			else
				if(message.getMessageType().equals(ACTION_RECEIVE_FROM)) {
					sendMessageToClient(message);
				}
				else
					if(message.getMessageType().equals(ACTION_RECEIVE_CLIENTS)) {
						sendMessageToClient(message);
					}
					else
						if(message.getMessageType().equals(ACTION_CONNECTED)) {
							String clientName = message.getMessage();
							
							//if the user is already connected, then don't allow login
							if(MainServer.getConnectedClient(clientName) != null) {
								closeConnection();
								return;
							}
							setClientName(clientName);
							
							System.out.println("connected: " + clientName);
							//send success to client
							sendMessageToClient(new MessageObject(ACTION_CONNECTED, "success"));
							
							//add the user to list
							MainServer.addClient(this);
						}
	}
	
	private void closeConnection() {
		exit = true;
		MainServer.removeClient(getClientName());
	}
	
	private boolean connectionOpened() {
		if (exit == false) {
			return true;
		}
		return false;
	}
	
	public Socket getClientSocket() {
		return clientSocket;
	}
	
	public String getClientName() {
		return clientName;
	}
	
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
}

//this class is used to send the connected clients to everyone on a secondary thread
class ClientManager extends Thread {
	
	private ConnectedClient connectedClient;
	private String clients;
	
	public ClientManager(ConnectedClient connectedClient, String clients) {
		this.connectedClient = connectedClient;
		this.clients = clients;
	}
	
	public void run() {
		connectedClient.manageMessage(new MessageObject(ConnectedClient.ACTION_RECEIVE_CLIENTS, this.clients));
	}
}

//MATERIE: SERIALIZARE!!!
class MessageObject implements Serializable {
	private String messageType;
	private String message;
	
	public MessageObject(String messageType, String message) {
		this.messageType = messageType;
		this.message = message;
	}
	
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
