import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.logging.*;

public class ThhBtpplClass
{
		private static final Logger LOGGER = Logger.getLogger(ThhBtpplTelegramClass.class.getName());
		private static final boolean DebugMode = false;
		private static final boolean VerbosMode = false;
		class LogFilter implements Filter
		{
			public boolean isLoggable(LogRecord record)
			{
				if (DebugMode)
					return true;
				
				if ((VerbosMode) && (record.getLevel() != Level.INFO))
					return true;
				
				return false;
			}
		}
		//---------------------------------------------
		
		private static ThhBtpplTelegramClass TelegramRespond;
	
		private static 	String 			ControllerIP;
		private static  int			    hPort;
		private static  int				lPort;
		private static  String			Password;
		private	static	short			JobTime;
		private	static	short			JobTimeCount;
		
		private static  Socket			hSocket;
		private static  Socket			lSocket;
		
		private static  OutputStream	hOutput;
		private static  InputStream		hInput;
		
		private static  OutputStream	lOutput;
		private static  InputStream		lInput;
		
		public static 	boolean 		NewRespond	= false;
		
		public static  	boolean 		Connected;
		
		private static PortRxThread		lPortThread;
		private static PortRxThread		hPortThread;
		
		
		//------------------------------------------
		
		public ThhBtpplClass(String inIP, String i_Password, int i_hPort, int i_lPort)
		{
			hPort = i_hPort;
			lPort = i_lPort;
			ControllerIP = inIP;
			Password = i_Password;
			LOGGER.setFilter(new LogFilter());
		}
		//-------------------------------------------
		
		public ThhBtpplClass(String inIP, String i_Password)
		{
			hPort = 2504;
			lPort = 3110;
			ControllerIP = inIP;
			Password = i_Password;
			JobTime = 0;
			JobTimeCount = 0;
			LOGGER.setFilter(new LogFilter());
		}
		//-------------------------------------------
		
		public ThhBtpplClass(String inIP)
		{
			hPort = 2504;
			lPort = 3110;
			Password = "";
			ControllerIP = inIP;
			JobTime = 0;
			JobTimeCount = 0;
			LOGGER.setFilter(new LogFilter());
		}
		//-------------------------------------------
		
		/**
	     * Test the connection with the controller
	     * @return 0 if success, negative if failed
	     */
		private static int TestConnections()
		{
			if ((hPort <= 0) || (lPort <= 0) || (ControllerIP == ""))
			{
				LOGGER.log(Level.SEVERE, "Connection parameters are missing. aborting!");
				return -1;
			}

			LOGGER.info("Trying to connect to Controller at " + ControllerIP + ":" + hPort + ":" + lPort);
			
			InetAddress ip;
			
			try
			{
				ip = InetAddress.getByName(ControllerIP);
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to resolve ip at " + ControllerIP +" aborting!");
				return -2;
			}

			try
			{
				hSocket = new Socket(ip, hPort);
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to open high priority socket on " + ControllerIP + ":" + hPort +" aborting!");
				return -3;
			}
			
			try
			{
				lSocket = new Socket(ip, lPort);
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to open low priority socket on " + ControllerIP + ":" + lPort +" aborting!");
				return -4;
			}
			
			return 0;
		}
		//------------------------------------------
		
		/**
	     * Open stream with the controller
	     * @return 0 if success, negative if failed
	     */
		private static int OpenStreams()
		{
			try
			{
				hInput = hSocket.getInputStream();
				lInput = lSocket.getInputStream();
				
				hOutput = hSocket.getOutputStream();
				lOutput = lSocket.getOutputStream();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to open IO strems on " + ControllerIP +" aborting!");
				return -1;
			}
			
			return 0;
		}
		//--------------------------------------------
		
		/**
	     * Opening TCP channel to the controller and test the connection
	     * @return 0 if success, negative if failed
	     */
		public int Connect()
		{
			int ret = TestConnections();
			if (ret < 0) {return ret;}
			
			ret = OpenStreams();
			if (ret < 0) {return ret;}
			
			hPortThread = new PortRxThread(hInput, hPort);
			try 
			{
				hPortThread.start();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to start Rx Thread on port " + hPort +" aborting!");
				return -1;
			}
			
			lPortThread = new PortRxThread(lInput, lPort);
			try 
			{
				lPortThread.start();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to start Rx Thread on port " + lPort +" aborting!");
				hPortThread.Stop();
				try
				{
					hPortThread.join();
				}
				catch (Exception ee)
				{
					LOGGER.log(Level.SEVERE, "ERROR [" + ee.getMessage() + "]: Unable to stop high priority Rx Thread on " + ControllerIP + ":" + hPort +" skipping!");
				}
				return -2;
			}
			
			LOGGER.info("Connection asstablished to controller at " + ControllerIP + ":" + hPort + ":" + lPort);
			
			Connected = true;
			
			return 0;
		}
		//--------------------------------------------
		
		/**
	     * Disconnect the TCP channel with the controller 
	     * @return 0 if success, negative if failed
	     */
		public int Disconnect()
		{
			try
			{
				hPortThread.Stop();
				hPortThread.join();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to stop high priority Rx Thread on " + ControllerIP + ":" + hPort +" skipping!");
			}
			
			try
			{
				lPortThread.Stop();
				lPortThread.join();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to stop low priority Rx Thread on " + ControllerIP + ":" + lPort +" skipping!");
			}
			
			
			try
			{
				hSocket.close();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to close high priority socket on " + ControllerIP + ":" + hPort +" aborting!");
				return -1;
			}
			
			try
			{
				lSocket.close();
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Unable to close low priority socket on " + ControllerIP + ":" + lPort +" aborting!");
				return -2;
			}
			
			LOGGER.info("Connection to controller at " + ControllerIP + " is now closed.");
			
			Connected = false;
			
			return 0;
		}
		//----------------------------------------------
		
		/**
	     * Send Telegram on TCP channel to controller 
	     * @Param Port the port - 3110/2504
	     * @param Telegram, The Telegram to send
	     * @return 0 if success, negative if failed
	     */
		public int SendTelegram(int Port, ThhBtpplTelegramClass Telegram)
		{
			if (!Connected)
			{
				LOGGER.log(Level.SEVERE, "Connection to controller at " + ControllerIP + " at port " + Port + " is now closed.");
				return -1;
			}
			
			OutputStream Output = lOutput;
			
			if (Telegram.Type == ThhBtpplTelegramClass.TelegramType_e.TELEGRAM_TYPE_REQUEST)
			{
				JobTime = (short)(JobTime + 1);
				if (JobTime == (short)0x7FFF)
				{
					JobTime = 0;
					JobTimeCount = (short)(JobTimeCount + 1);
				}
				Telegram.JobTime = JobTime;
				Telegram.JobTimeCount = JobTimeCount;
			}
			
			Telegram.Password = Password;
			byte[] bufToSend = Telegram.toByteStream();
			if (bufToSend.length < 4)
			{
				LOGGER.log(Level.SEVERE, "Attempting to send bad telegram to controller at " + ControllerIP + " at port " + Port + " is now closed.");
				return -1;
			}
			
			if (Port == lPort)
			{
				Output = lOutput;
			}
			else if (Port == hPort)
			{
				Output = hOutput;
			}
			else
			{
				LOGGER.log(Level.SEVERE, "Attempting to send telegram to controller at " + ControllerIP + " at illegal port " + Port + " aborting operation");
				return -2;
			}	
			
			try
			{
				Output.write(bufToSend);
			}
			catch(Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: Failed to send telegram:");
				return -3;
			}	
			
			NewRespond = false;
			LOGGER.info("Telegram sent:");
			return 0;
		}
		//---------------------------------------------
		
		public static class PortRxThread extends Thread
		{
			public InputStream input;
			public int Port;
			public boolean NeedToExit;
			//-------------
			
			public PortRxThread(InputStream inStream, int inPort)
			{
				input = inStream;
				Port = inPort;
				NeedToExit = false;
				LOGGER.info("Rx thread created on port " + Port);
			}
			//---------------
			
			public void Stop()
			{
				NeedToExit = true;
			}
			//--------------
			
			/**
		     * Run the thread that wait for receiving Tekegram from the controller
		     */
			public void run()
			{
				byte[] RxPacket = new byte[4];
				int RxBL = -1;
				int RxExpectedLength = -1;
				int RxPacketIndex = 0;
				boolean BlockStarted = false;
				
				LOGGER.info("Rx thread running on port " + Port);
				
				while (!NeedToExit)
				{
					try
					{
						if (input.available() == 0)
						{
							Thread.sleep(1);
							continue;
						}
						else if (!BlockStarted)
						{
							if (input.available() >= 4)
							{
								byte[] tmpArr = new byte[4];
								input.read(tmpArr, 0 , 4);
								
								RxBL = ByteBuffer.wrap(tmpArr).getInt();
								if (RxBL <= 0)
								{
									LOGGER.info("Zero length telegram Rxed on port " + Port);
									BlockStarted = false;
									RxBL = -1;
									RxPacketIndex = 0;
									RxExpectedLength = -1;
									continue;
								}
								
								BlockStarted = true;
								RxExpectedLength = RxBL + 4;
								RxPacket = new byte[RxExpectedLength];
								System.arraycopy(tmpArr, 0, RxPacket, 0, 4);
								RxPacketIndex = 4;
								RxExpectedLength = RxExpectedLength - 4;
							}
						}
						else // BlockStarted
						{
							int bytesRead = input.read(RxPacket, RxPacketIndex, RxExpectedLength);
							if (bytesRead < 0)
								continue;
							
							RxPacketIndex = RxPacketIndex + bytesRead;
							RxExpectedLength = RxExpectedLength - bytesRead;
							if (RxExpectedLength == 0)
							{
								LOGGER.info("New packet Rxed on port " + Port + " with length " + RxPacket.length + " sending to parser");
								ParsePacket(RxPacket);
								BlockStarted = false;
								RxBL = -1;
								RxExpectedLength = -1;
								RxPacketIndex = 0;
							}
						}
					}
					catch (Exception e)
					{
						LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: on port " + Port + " Rx thread.");
					}
				}
				
				LOGGER.info("Rx thread stopped on port " + Port);
			}
			//---------------
			
			/**
		     * Parsing the received Telegram packet 
		     * @param Packet byte array of the packet
		     */
			public void ParsePacket(byte[] Packet)
			{
				TelegramRespond = new ThhBtpplTelegramClass();
				
				
				if (TelegramRespond.fromByteStream(Packet) < 0)
				{
					LOGGER.log(Level.SEVERE, "Error while parsing packet on port " + Port);
				}
				else
				{
					NewRespond = true;
					LOGGER.info("New telegram Rxed on port " + Port + " :");
					TelegramRespond.print();
				}
			}
		}
		//---------------------------------------------

		public static ThhBtpplTelegramClass GetLastRespond()
		{
			int TimeOut = 10000;
			
			while(!NewRespond && TimeOut>0)
			{
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				TimeOut--;
			}
			
			if(!NewRespond)
			{
				return null;
			}
			else
			{
				return TelegramRespond;
			}
			
		}
		
}