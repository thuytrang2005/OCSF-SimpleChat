// This file contains material supporting section 3.7 of the textbook:
// "Object Oriented Software Engineering" and is issued under the open-source
// license found at www.lloseng.com 

import java.io.*;
import ocsf.server.*;
import common.*;

/**
 * This class overrides some of the methods in the abstract 
 * superclass in order to give more functionality to the server.
 *
 * @author Dr Timothy C. Lethbridge
 * @author Dr Robert Lagani&egrave;re
 * @author Fran&ccedil;ois B&eacute;langer
 * @author Paul Holden
 * @version July 2000 / Phase 2 (Exercises E49-E52)
 */
public class EchoServer extends AbstractServer 
{
  //Class variables *************************************************
  
  /**
   * The default port to listen on.
   */
  final public static int DEFAULT_PORT = 5555;

  //Instance variables **********************************************

  /**
   * The interface type variable. It allows the implementation of 
   * the display method in the server UI.
   */
  private ChatIF serverUI;

  /**
   * Indicates whether the server is completely closed.
   */
  private boolean isClosed = true;

  
  //Constructors ****************************************************
  
  /**
   * Constructs an instance of the echo server.
   *
   * @param port The port number to connect on.
   */
  public EchoServer(int port) 
  {
    super(port);
    this.isClosed = true;
  }

  /**
   * Constructs an instance of the echo server with a UI.
   *
   * @param port The port number to connect on.
   * @param serverUI The UI display handler.
   */
  public EchoServer(int port, ChatIF serverUI) 
  {
    super(port);
    this.serverUI = serverUI;
    this.isClosed = true;
  }

  
  //Instance methods ************************************************
  
  /**
   * This method handles any messages received from the client.
   * (Slot method required by AbstractServer).
   *
   * @param msg The message received from the client.
   * @param client The connection from which the message originated.
   */
  public void handleMessageFromClient
    (Object msg, ConnectionToClient client)
  {
    String msgStr = msg.toString();

    if (msgStr.startsWith("#login"))
    {
      if (client.getInfo("loginId") != null)
      {
        try
        {
          client.sendToClient("Error: You are already logged on as " + client.getInfo("loginId"));
        }
        catch (IOException e) {}
        return;
      }

      String[] parts = msgStr.split("\\s+", 2);
      String loginId = parts.length > 1 ? parts[1].trim() : "anonymous";

      System.out.println("Message received: " + msgStr + " from " + client.getInfo("loginId"));
      client.setInfo("loginId", loginId);
      System.out.println(loginId + " has logged on.");
      sendToAllClients(loginId + " has logged on.");
    }
    else
    {
      if (client.getInfo("loginId") == null)
      {
        try
        {
          client.sendToClient("Error: You must log in first before sending messages.");
          client.close();
        }
        catch (IOException e) {}
        return;
      }

      String loginId = (String) client.getInfo("loginId");
      System.out.println("Message received: " + msg + " from " + loginId);
      sendToAllClients(loginId + "> " + msg);
    }
  }

  /**
   * Handles messages typed into the server console.
   * Differentiates commands starting with '#' and normal broadcast messages.
   *
   * @param message The input string from the server UI.
   */
  public void handleMessageFromServerUI(String message)
  {
    if (message == null || message.trim().isEmpty())
    {
      return;
    }

    if (message.startsWith("#"))
    {
      handleServerCommand(message);
    }
    else
    {
      System.out.println("SERVER MESSAGE> " + message);
      sendToAllClients("SERVER MESSAGE> " + message);
    }
  }

  /**
   * Handles commands typed by the server administrator.
   *
   * @param command The command starting with '#'.
   */
  private void handleServerCommand(String command)
  {
    String[] parts = command.trim().split("\\s+", 2);
    String cmd = parts[0].toLowerCase();
    String argument = parts.length > 1 ? parts[1].trim() : null;

    if (cmd.equals("#quit"))
    {
      try
      {
        close();
      }
      catch (IOException e) {}
      System.exit(0);
    }
    else if (cmd.equals("#stop"))
    {
      stopListening();
    }
    else if (cmd.equals("#close"))
    {
      try
      {
        sendToAllClients("SERVER SHUTTING DOWN! DISCONNECTING!");
        close();
      }
      catch (IOException e)
      {
        System.out.println("Error while closing server.");
      }
    }
    else if (cmd.equals("#setport"))
    {
      if (isListening() || getNumberOfClients() > 0)
      {
        System.out.println("Error: Cannot change port. Server is not closed.");
      }
      else if (argument == null || argument.isEmpty())
      {
        System.out.println("Error: Port number argument required for #setport.");
      }
      else
      {
        try
        {
          int port = Integer.parseInt(argument);
          setPort(port);
          System.out.println("port set to: " + port);
        }
        catch (NumberFormatException e)
        {
          System.out.println("Error: Invalid port number: " + argument);
        }
      }
    }
    else if (cmd.equals("#start"))
    {
      if (isListening())
      {
        System.out.println("Error: Server is already listening for clients.");
      }
      else
      {
        try
        {
          listen();
        }
        catch (IOException e)
        {
          System.out.println("ERROR - Could not listen for clients!");
        }
      }
    }
    else if (cmd.equals("#getport"))
    {
      System.out.println("Current port: " + getPort());
    }
    else
    {
      System.out.println("Invalid command: " + command);
    }
  }

  // Hook methods overriding AbstractServer **************************
    
  /**
   * Hook method called when the server starts listening for connections.
   */
  @Override
  protected void serverStarted()
  {
    isClosed = false;
    System.out.println
      ("Server listening for clients on port " + getPort());
  }
  
  /**
   * Hook method called when the server stops listening for connections.
   */
  @Override
  protected void serverStopped()
  {
    System.out.println
      ("Server has stopped listening for connections.");
    sendToAllClients("WARNING - Server has stopped listening for connections.");
  }

  /**
   * Hook method called when the server is closed.
   */
  @Override
  protected void serverClosed()
  {
    isClosed = true;
    System.out.println("Server has closed.");
  }

  /**
   * Hook method called each time a new client connection is accepted.
   *
   * @param client The connection connected to the client.
   */
  @Override
  protected void clientConnected(ConnectionToClient client)
  {
    System.out.println("A new client is attempting to connect to the server.");
  }

  /**
   * Hook method called each time a client disconnects.
   *
   * @param client The connection with the client.
   */
  @Override
  synchronized protected void clientDisconnected(ConnectionToClient client)
  {
    Object loginId = client.getInfo("loginId");
    if (loginId != null)
    {
      client.setInfo("loginId", null);
      System.out.println(loginId + " has disconnected.");
      sendToAllClients(loginId + " has disconnected.");
    }
  }

  /**
   * Hook method called each time an exception is thrown in a ConnectionToClient thread.
   *
   * @param client The client that raised the exception.
   * @param exception The exception thrown.
   */
  @Override
  synchronized protected void clientException(ConnectionToClient client, Throwable exception)
  {
    Object loginId = client.getInfo("loginId");
    if (loginId != null)
    {
      client.setInfo("loginId", null);
      System.out.println(loginId + " has disconnected.");
      sendToAllClients(loginId + " has disconnected.");
    }
  }
  
  //Class methods ***************************************************
  
  /**
   * This method is responsible for the creation of 
   * the server instance via ServerConsole.
   *
   * @param args[0] The port number to listen on. Defaults to 5555 
   *          if no argument is entered.
   */
  public static void main(String[] args) 
  {
    ServerConsole.main(args);
  }
}
//End of EchoServer class
