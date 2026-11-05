// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

package client;

import ocsf.client.*;
import common.*;
import java.io.*;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @version July 2000 / Phase 2 (Exercises E49-E52)
 */
public class ChatClient extends AbstractClient
{
  //Instance variables **********************************************
  
  /**
   * The interface type variable. It allows the implementation of 
   * the display method in the client.
   */
  private ChatIF clientUI; 

  /**
   * The login ID of this client.
   */
  private String loginId;

  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the chat client.
   *
   * @param loginId The user's login ID.
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   * @throws IOException If opening the connection fails.
   */
  public ChatClient(String loginId, String host, int port, ChatIF clientUI) 
    throws IOException 
  {
    super(host, port); //Call the superclass constructor
    this.loginId = loginId;
    this.clientUI = clientUI;
    openConnection();
  }

  /**
   * Constructs an instance of the chat client with optional auto-connect.
   *
   * @param loginId The user's login ID.
   * @param host The server to connect to.
   * @param port The port number to connect on.
   * @param clientUI The interface type variable.
   * @param autoConnect If true, attempts to open connection immediately.
   */
  public ChatClient(String loginId, String host, int port, ChatIF clientUI, boolean autoConnect) 
  {
    super(host, port);
    this.loginId = loginId;
    this.clientUI = clientUI;
    if (autoConnect)
    {
      try
      {
        openConnection();
      }
      catch (IOException e)
      {
        clientUI.display("Cannot open connection.  Awaiting command.");
      }
    }
  }

  
  //Instance methods ************************************************

  /**
   * Returns the client's login ID.
   *
   * @return The login ID.
   */
  public String getLoginId()
  {
    return loginId;
  }

  /**
   * Sets the client's login ID.
   *
   * @param loginId The new login ID.
   */
  public void setLoginId(String loginId)
  {
    this.loginId = loginId;
  }
    
  /**
   * This method handles all data that comes in from the server.
   * (Slot method required by AbstractClient).
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) 
  {
    clientUI.display(msg.toString());
  }

  /**
   * This method handles all data coming from the UI.
   * It differentiates between normal messages and commands starting with '#'.
   *
   * @param message The message from the UI.    
   */
  public void handleMessageFromClientUI(String message)
  {
    if (message == null || message.trim().isEmpty())
    {
      return;
    }

    if (message.startsWith("#"))
    {
      handleCommand(message);
    }
    else
    {
      try
      {
        if (!isConnected())
        {
          clientUI.display("Cannot send message: Not connected to a server.");
          return;
        }
        sendToServer(message);
      }
      catch(IOException e)
      {
        clientUI.display("Could not send message to server. Terminating client.");
        quit();
      }
    }
  }

  /**
   * Handles user commands typed into the client console.
   *
   * @param command The command string starting with '#'.
   */
  private void handleCommand(String command)
  {
    String[] parts = command.trim().split("\\s+", 2);
    String cmd = parts[0].toLowerCase();
    String argument = parts.length > 1 ? parts[1].trim() : null;

    if (cmd.equals("#quit"))
    {
      quit();
    }
    else if (cmd.equals("#logoff"))
    {
      if (!isConnected())
      {
        clientUI.display("Cannot logoff: already disconnected.");
      }
      else
      {
        try
        {
          closeConnection();
        }
        catch(IOException e)
        {
          clientUI.display("Error while disconnecting from server.");
        }
      }
    }
    else if (cmd.equals("#sethost"))
    {
      if (isConnected())
      {
        clientUI.display("Error: Cannot set host while connected. Please logoff first.");
      }
      else if (argument == null || argument.isEmpty())
      {
        clientUI.display("Error: Host name argument required for #sethost.");
      }
      else
      {
        setHost(argument);
        clientUI.display("Host set to: " + argument);
      }
    }
    else if (cmd.equals("#setport"))
    {
      if (isConnected())
      {
        clientUI.display("Error: Cannot set port while connected. Please logoff first.");
      }
      else if (argument == null || argument.isEmpty())
      {
        clientUI.display("Error: Port number argument required for #setport.");
      }
      else
      {
        try
        {
          int port = Integer.parseInt(argument);
          setPort(port);
          clientUI.display("port set to: " + port);
        }
        catch (NumberFormatException e)
        {
          clientUI.display("Error: Invalid port number: " + argument);
        }
      }
    }
    else if (cmd.equals("#login"))
    {
      if (isConnected())
      {
        clientUI.display("Error: Cannot login. Already connected to server.");
      }
      else
      {
        if (argument != null && !argument.isEmpty())
        {
          this.loginId = argument;
        }
        try
        {
          openConnection();
        }
        catch (IOException e)
        {
          clientUI.display("Cannot open connection.  Awaiting command.");
        }
      }
    }
    else if (cmd.equals("#gethost"))
    {
      clientUI.display("Current host: " + getHost());
    }
    else if (cmd.equals("#getport"))
    {
      clientUI.display("Current port: " + getPort());
    }
    else
    {
      clientUI.display("Invalid command: " + command);
    }
  }

  // Hook methods overriding AbstractClient *************************

  /**
   * Hook method called after a connection has been established.
   * Automatically sends the #login <loginId> command to the server.
   */
  @Override
  protected void connectionEstablished()
  {
    try
    {
      sendToServer("#login " + loginId);
    }
    catch (IOException e)
    {
      clientUI.display("Error: Could not send login ID to server.");
    }
  }

  /**
   * Hook method called after the connection has been closed gracefully.
   */
  @Override
  protected void connectionClosed()
  {
    clientUI.display("Connection closed.");
  }

  /**
   * Hook method called each time an exception is thrown by the
   * client's thread waiting for messages from the server (e.g. server crash or shutdown).
   *
   * @param exception The exception raised.
   */
  @Override
  protected void connectionException(Exception exception)
  {
    clientUI.display("Abnormal termination of connection.");
  }
  
  /**
   * This method terminates the client gracefully.
   */
  public void quit()
  {
    try
    {
      closeConnection();
    }
    catch(IOException e) {}
    System.exit(0);
  }
}
//End of ChatClient class
