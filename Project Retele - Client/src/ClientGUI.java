import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.GridLayout;
import javax.swing.JTextField;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.JList;
import java.awt.Dimension;
import javax.swing.JTextPane;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClientGUI extends JFrame {

	//tine mesajele pe care le ai cu fiecare user
	//este un map de la numele user-lui la mesajele pe care le ai cu el
	private HashMap<String, ClientMessage> clientsMessages = new HashMap<>();
	private ServerConnection serverConnection;
	
	//the currently selected user from the list
	private String selectedUser;
	//the username used to log in
	private String myUsername;
	
	//the list model for the users list (if this changes, so does the users list)
	private DefaultListModel defaultListModel;
	
	private JPanel contentPane;
	private JTextField loginTextField;
	private JPanel mainPanel;
	private JPanel loginPanel;
	private JScrollPane scrollPane;
	private JList list;
	private JPanel panel;
	private JTextPane textPane;
	private JPanel panel_1;
	private JTextField newMessageTextField;
	private JButton btnNewButton;
	private JButton logoutButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientGUI frame = new ClientGUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ClientGUI() {
		
		setMinimumSize(new Dimension(600, 600));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setMinimumSize(new Dimension(600, 600));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		loginPanel = new JPanel();
		loginPanel.setMinimumSize(new Dimension(300, 10));
		contentPane.setLayout(new BorderLayout(0, 0));
		
		mainPanel = new JPanel();
		mainPanel.setVisible(false);
		mainPanel.setMinimumSize(new Dimension(300, 10));
//		contentPane.add(mainPanel, BorderLayout.EAST);
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[]{220, 220};
		gbl_mainPanel.rowHeights = new int[]{268, 0};
		gbl_mainPanel.columnWeights = new double[]{0.0, 1.0};
		gbl_mainPanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		mainPanel.setLayout(gbl_mainPanel);
		
		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		mainPanel.add(scrollPane, gbc_scrollPane);
		
		defaultListModel = new DefaultListModel<>();
		list = new JList(defaultListModel);
		list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent arg0) {
            	//if a user was clicked
                if (!arg0.getValueIsAdjusting() && list.getSelectedValue() != null) {
                	//maybe the user has his name with a star (because he has unread messages)
                	//then we need to remove the star
                	String selectedUsername = removeStarFromUsernameIfNecessary(list.getSelectedValue().toString());
                	
                	//load the messages you had with that user into the window
                	openConversation(selectedUsername);
                }
            }
        });
		scrollPane.setViewportView(list);
		
		logoutButton = new JButton("Log Out");
		logoutButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//start a new thread where we send the log out command
				ServerManager serverManager = new ServerManager(serverConnection, new MessageObject(ServerConnection.ACTION_EXIT, ""));
				serverManager.start();
			}
		});
		scrollPane.setColumnHeaderView(logoutButton);
		
		panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 0;
		mainPanel.add(panel, gbc_panel);
		panel.setLayout(new BorderLayout(0, 0));
		
		panel_1 = new JPanel();
		panel.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		newMessageTextField = new JTextField();
		panel_1.add(newMessageTextField, BorderLayout.CENTER);
		newMessageTextField.setColumns(10);
		
		btnNewButton = new JButton("Send");
		btnNewButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				//if the message we want to send is not empty, then we send it
				if (newMessageTextField.getText().equals("") == false) {
					//if we selected the user we want to send the message to, then send it
					if (getSelectedUser().equals("") == false) {
						sendMessage(getSelectedUser(), newMessageTextField.getText());
						newMessageTextField.setText("");
					}
				}
			}
		});
		panel_1.add(btnNewButton, BorderLayout.EAST);
		
		textPane = new JTextPane();
		panel.add(textPane, BorderLayout.CENTER);
		contentPane.add(loginPanel, BorderLayout.CENTER);
		GridBagLayout gbl_loginPanel = new GridBagLayout();
		gbl_loginPanel.columnWidths = new int[]{220, 220};
		gbl_loginPanel.rowHeights = new int[]{268, 0};
		gbl_loginPanel.columnWeights = new double[]{0.0, 0.0};
		gbl_loginPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		loginPanel.setLayout(gbl_loginPanel);
		
		loginTextField = new JTextField();
		GridBagConstraints gbc_loginTextField = new GridBagConstraints();
		gbc_loginTextField.anchor = GridBagConstraints.EAST;
		gbc_loginTextField.insets = new Insets(0, 0, 0, 5);
		gbc_loginTextField.gridx = 0;
		gbc_loginTextField.gridy = 0;
		loginPanel.add(loginTextField, gbc_loginTextField);
		loginTextField.setColumns(10);
		
		JButton loginButton = new JButton("Login");
		GridBagConstraints gbc_loginButton = new GridBagConstraints();
		gbc_loginButton.anchor = GridBagConstraints.WEST;
		gbc_loginButton.gridx = 1;
		gbc_loginButton.gridy = 0;
		loginButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String username = loginTextField.getText();
				//if the username is not empty
				if (username.equals("") == false) {
					
					//if the username doesn't contain spaces, then log in
					if (username.indexOf(" ") == -1) {
						
						myUsername = username;

						startConnectingToServer(username);
						
					}
				}
			}
		});
		loginPanel.add(loginButton, gbc_loginButton);
	}
	
	private void openConversation(String newSelectedUser) {
		selectedUser = newSelectedUser;
		
		//remove the * from the user name
		updateUnreadMessages(newSelectedUser, true);
		
		//get all the messages from the conversation
		String messagesList = "";
		for(String message : clientsMessages.get(selectedUser).getMessages()) {
			messagesList += message + "\n";
		}
		
		//remove the extra '\n' from the end
		if(messagesList.length() > 0) {
			messagesList.substring(0, messagesList.length() - 1);
		}
		
		textPane.setText(messagesList);
	}

	private void startConnectingToServer(String clientName) {
		serverConnection = new ServerConnection(this, clientName);
		serverConnection.start();
	}
	
	public void receivedMessage(String transmitator, String mesaj) {
		//if we get a message from a user with whom we don't have any conversation,
		//then create a new link in the map
		if (clientsMessages.containsKey(transmitator) == false) {
			clientsMessages.put(transmitator, new ClientMessage(transmitator));
		}

		//save the message we received
		clientsMessages.get(transmitator).receivedMessage(mesaj);
		
		//if the conversation is opened with the one who sent the message, update the messages list
		if(transmitator.equals(selectedUser)) {
			openConversation(selectedUser);
		}
		//otherwise add * at the end of the user
		else {
			updateUnreadMessages(transmitator, false);
		}
	}
	
	public void sendMessage(String user, String mesaj) {
		//if the user we want to send the message is not already in the list, then add it
		if (clientsMessages.containsKey(user) == false) {
			clientsMessages.put(user, new ClientMessage(user));
		}
		//save the message we sent
		clientsMessages.get(user).sentMessage(mesaj);
		
		openConversation(user);
		
		//send the message to the server
		MessageObject messageObject = new MessageObject(ServerConnection.ACTION_SEND_TO, user + " " + mesaj);
		ServerManager serverManager = new ServerManager(serverConnection, messageObject);
		serverManager.start();
	}
	
	public void updateUnreadMessages(String unreadUser, boolean messageWasRead) {
		//go through all the users and when we find the wanted user, we set its status
		for(int i = 0; i < defaultListModel.size(); i++) {
			String currentUser = removeStarFromUsernameIfNecessary(defaultListModel.get(i).toString());
			if (currentUser.equals(unreadUser)) {
				//the messages are read
				if (messageWasRead) {
					defaultListModel.set(i, unreadUser);
				}
				//the messages are not read
				else {
					defaultListModel.set(i, unreadUser + " *");
				}
			}
		}
	}
	
	private String removeStarFromUsernameIfNecessary(String userName) {
    	//if the selected username contains " *" (it means he has unread messages), then remove the " *"
    	if (userName.indexOf(" ") != -1) {
    		userName = userName.substring(0, userName.indexOf(" "));
    	}
    	return userName;
	}
	
	public void updateClients(String[] clients) {

		//clear the GUI's list
		defaultListModel.clear();
		
		//add the new users to list
		for (String newClient : clients) {
			//if the user is not already in the list, then add him
			if (clientsMessages.get(newClient) == null) {
				clientsMessages.put(newClient, new ClientMessage(newClient));
			}
			
			//if the user is not myself, then add the user to GUI's list
			if (newClient.equals(myUsername) == false) {
				defaultListModel.addElement(newClient);
			}
		}
		
		//remove the disconnected clients
		Set<String> clientsToRemove = new HashSet<>();
		for (String clientName : clientsMessages.keySet()) {
			
			boolean isClientConnected = false;

			for (String newClient : clients) {
				//we find the client, so he is connected
				if (clientName.equals(newClient)) {
					isClientConnected = true;
					break;
				}
			}

			//the client is disconnected
			if (isClientConnected == false) {
				clientsToRemove.add(clientName);
			}
		}
		clientsMessages.keySet().removeAll(clientsToRemove);
		
	}

	public void loggedIn() {

		//hide the login panel
		loginPanel.setVisible(false);
		contentPane.remove(loginPanel);
		
		//show the GUI
		mainPanel.setVisible(true);
		contentPane.add(mainPanel, BorderLayout.CENTER);
		
		logoutButton.setText("Log out " + myUsername);
		
		contentPane.revalidate();
		
		System.out.println("logged in");
	}
	
	public void loggedOut() {

		//hide the main panel
		mainPanel.setVisible(false);
		contentPane.remove(mainPanel);
		
		//show the login GUI
		loginPanel.setVisible(true);
		contentPane.add(loginPanel, BorderLayout.CENTER);
		
		clientsMessages.clear();
		
		contentPane.revalidate();
		
		selectedUser = "";
		System.out.println("logged out");
	}
	
	private String getSelectedUser() {
		return selectedUser;
	}
	
	private void setSelectedUser(String selectedUser) {
		this.selectedUser = selectedUser;
	}
}

class ServerConnection extends Thread {

	private boolean exit = false;
	private String clientName;
	private Socket serverSocket;
	private ClientGUI GUI;

	public static final String ACTION_EXIT = "ACTION_EXIT";
	public static final String ACTION_SEND_TO = "ACTION_SEND_TO";
	public static final String ACTION_RECEIVE_FROM = "ACTION_RECEIVE_FROM";
	public static final String ACTION_CONNECTED = "ACTION_CONNECTED";
	public static final String ACTION_RECEIVE_CLIENTS = "ACTION_RECEIVE_CLIENTS";
	
	public ServerConnection(ClientGUI mainClient, String clientName) {
		this.GUI = mainClient;
		this.clientName = clientName;
	}
	
	public void run() {
		connectToServer();
		if (connectionOpened()) {
			GUI.loggedIn();
		}
		while (connectionOpened()) {
			MessageObject message = readMessage();
			manageMessage(message);
		}
		GUI.loggedOut();
	}
	
	private void connectToServer() {
		String serverName = "localhost";//args[0];
		int port = 8888;//Integer.parseInt(args[1]);
		try {
			System.out.println("Connecting to " + serverName + " on port " + port);
			//MATERIE: SOCKET!!!
			serverSocket = new Socket(serverName, port);
			sendMessageToServer(new MessageObject(ACTION_CONNECTED, getClientName()));
			readMessage();
		} catch (IOException e) {
			
			closeConnection();
			
			e.printStackTrace();
		}

	}
	
	private MessageObject readMessage() {
		MessageObject message = new MessageObject("", "");
		try {
			//MATERIE: DESERIALIZARE!!!
			ObjectInputStream in = new ObjectInputStream(getServerSocket().getInputStream());
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

	private void sendMessageToServer(MessageObject message) {
		try {
			//MATERIE: SERIALIZARE!!!
			ObjectOutputStream out = new ObjectOutputStream(getServerSocket().getOutputStream());
			//send a message to the client
			out.writeObject(message);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void manageMessage(MessageObject message) {
		//if the connection is closed, don't do anything
		if (connectionOpened() == false) {
			return;
		}
		System.out.println("manage message:\n" + clientName + "\n" + message.getMessageType() + "\n" + message.getMessage());
		if (message.getMessageType().equals(ACTION_EXIT)) {
			sendMessageToServer(message);
			closeConnection();
			GUI.loggedOut();
		}
		else
			if(message.getMessageType().equals(ACTION_SEND_TO)) {
				sendMessageToServer(message);
			}
			else
				if(message.getMessageType().equals(ACTION_RECEIVE_FROM)) {
					String transmitator = message.getMessage().split(" ")[0];
					String mesaj = message.getMessage().substring(message.getMessage().indexOf(" ")+1);
					
					//show the message on screen
					GUI.receivedMessage(transmitator, mesaj);
				}
				else
					if(message.getMessageType().equals(ACTION_RECEIVE_CLIENTS)) {
						String[] clients = message.getMessage().split(" ");
						
						//show the clients on screen
						GUI.updateClients(clients);
					}
					else
						if(message.getMessageType().equals(ACTION_CONNECTED)) {
//							mainClient.loggedIn();
						}
	}
	
	private void closeConnection() {
		exit = true;
	}
	
	private boolean connectionOpened() {
		if (exit == false) {
			return true;
		}
		return false;
	}
	
	public String getClientName() {
		return clientName;
	}
	
	public Socket getServerSocket() {
		return serverSocket;
	}
}

//class to send messages on secondary thread
class ServerManager extends Thread {
	
	private ServerConnection serverConnection;
	private MessageObject messageToSend;
	
	public ServerManager(ServerConnection serverConnection, MessageObject messageToSend) {
		this.serverConnection = serverConnection;
		this.messageToSend = messageToSend;
	}
	
	public void run() {
		serverConnection.manageMessage(messageToSend);
	}
}

class ClientMessage {
	
	private String clientName;
	private ArrayList<String> messages = new ArrayList<>();
	
	public ClientMessage(String clientName) {
		this.clientName = clientName;
	}
	
	public void receivedMessage(String message) {
		messages.add(clientName + ": " + message);
	}
	
	public void sentMessage(String message) {
		messages.add("ME: " + message);
	}
	
	public ArrayList<String> getMessages() {
		return messages;
	}
}

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
