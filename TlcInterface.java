import java.nio.*;
import java.util.*;
import java.util.TimeZone;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
//-------------------------------------------------------
import java.util.logging.Logger;

public class TlcInterface
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
	
	
	public static final long version = 0x000001;
	
	private ThhBtpplClass Controller;
	
	public class TlcGeneralData
	{
		private int 	ErrorCode; //0:OK, other see TBD doc
		private byte 	hour;                    
		private byte 	minute;
		private byte 	second;
		private byte 	day;
		private byte 	month;
		private int  	year;
		private int 	ProgramNumber;
		private int 	CycleSecond;
		private HashMap<String, SignalGroupStatus> 	SignalGroups;		
		private HashMap<String, DetectorStatus> 	Detectors;
		private int 	StageNumber;
	}	
	
	private boolean 	skeepEnumEntities = false;
	
	private String[] 	LampNames;
	private byte[] 		LampValue;
	
	private String[] 	DigEngineNames;
	private byte[] 		DigEngineValue;
	
	private int			ControllerZNr;
	private int			ControllerFNr;
	//--------------------------------------------------------------------
	
	public static class SignalGroupStatus 
	{
		public SignalGroupType 		type;                             			//Move type
		public boolean 				off;                                       	//true - lights off
		public boolean 				red;                                       	//true - red is on
		public boolean 				yellow;                                    	//true - yellow is on
		public boolean 				green;                                     	//true - green is on
		public boolean 				flashing;                                  	//true - lights flashing			
	}
	//--------------------------------------------------------------------
	
	public class DetectorStatus
	{
		public boolean occupied;
	}	
	//--------------------------------------------------------------------
	
	public enum SignalGroupType // option - locally defined 
	{
		VEHICLE,
		PEDESTRIAN,
		BLINKER,
		BICYCLE,
		PUBLIC_TRANSPORT,		
		PREEMPTION_TRIANGLE,		
		VEHICLE_FLASHING_GREEN
	}	
	//--------------------------------------------------------------------
	
	public class TlcStatusCmd 
	{
	    public int     programNumber;		
	}
	//---------------------------------------------------------------------
	
	/**
     * creates new TlcStatusCmd structure for public use
     * @return new TlcStatusCmd struct
     */
	public TlcStatusCmd createStatusCmd()
	{
		return new TlcStatusCmd();
	}
	//--------------------------------------------------------------------
	
	/**
     * Read all data from the controller 
     * @return TlcGeneralData with current information, if ret field is other than 0 - the returned structure is invalid
     */
	public TlcGeneralData readGeneralData()
	{
		int ret;
		
	    TlcGeneralData retData = new TlcGeneralData();
	    
		ret = this.GetTESiplOnline(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "GetTESiplOnline return with error: " + ret);
			retData.ErrorCode = ret;
			return retData;
		}
		
		ret = this.DigEngine(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "DigEngine return with error: " + ret);
			retData.ErrorCode = ret;
			return retData;
		}
		
		ret = this.ISignalProgram(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "ISignalProgram return with error: " + ret);
			retData.ErrorCode = ret;
			return retData;
		}
		
		ret = this.GetTime(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "GetTime return with error: " + ret);
			retData.ErrorCode = ret;
			return retData;
		}
		
		ret = this.APWertRkUshort(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "APWertRkUshort return with error: " + ret);
			retData.ErrorCode = ret;
			return retData;
		}
		
		return retData;
	}	
	//--------------------------------------------------------------------
	
	/**
     * set status of controller
     * @param cmd new status to set. see TlcStatusCmd class for details
     * @return 0 if success, negative if failed
     */
	public int sendStatusCommand(TlcStatusCmd cmd)
	{
		int ret;
		
		ret = this.setProgramNumber(cmd.programNumber);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "setProgramNumber return with error: " + ret);
			return ret;
		}
		
		return 0;
	}
	//----------------------------------------------------------------------
	
	/**
     * Print all the data of TlcGeneralData to console
     * @param TlcData, the TlcGeneralData data set source
     */	
	public void PrintTlcData(TlcGeneralData TlcData)
	{
		if (TlcData.ErrorCode != 0)
			System.out.println("FATAL ERROR: invalid TlcData");
			
		System.out.println("Date: " + TlcData.day + "/"  
									+ TlcData.month + "/"
									+ TlcData.year + " "
									+ TlcData.hour + ":"
									+ TlcData.minute + ":"
									+ TlcData.second);
		
		String[] tmpSG = TlcData.SignalGroups.keySet().toArray(new String[TlcData.SignalGroups.size()]);
		for(int i=0;i<TlcData.SignalGroups.size();i++)
		{		    
			System.out.println(tmpSG[i] + ":");
			System.out.println("Flashin: " + TlcData.SignalGroups.get(tmpSG[i]).flashing);
			System.out.println("Red: " + TlcData.SignalGroups.get(tmpSG[i]).red);
			System.out.println("Green: " + TlcData.SignalGroups.get(tmpSG[i]).green);
			System.out.println("On/Off: " + TlcData.SignalGroups.get(tmpSG[i]).off);
			System.out.println("Yellow: " + TlcData.SignalGroups.get(tmpSG[i]).yellow + "\n");
		}
		
		String[] tmpDS = TlcData.Detectors.keySet().toArray(new String[TlcData.Detectors.size()]);
		for(int i=0;i<TlcData.Detectors.size();i++)
		{
		    System.out.print(tmpDS[i] + ": " + TlcData.Detectors.get(tmpDS[i]).occupied+"\n");
		}
		
		System.out.println("\nProgram number: "+ TlcData.ProgramNumber);
		System.out.println("Stage number: "+ TlcData.StageNumber);
		System.out.println("Cycle seconds: "+ TlcData.CycleSecond);
	}
	//-------------------------------------------------------------
	
	/**
     * Opening TCP channel to the controller 
     * @param inIP the IP of the controller
     * @param inZNr the ZNr of the controller
     * @param inFNr the FNr of the controller
     * @param i_Password the password for authentication to the controller
     * @param i_hPort set the high priority port other than the default of OCIT-O
     * @param i_lPort set the low priority port other than the default of OCIT-O
     * @return 0 if success, negative if failed
     */
	public int Connect(String inIP, int inZNr, int inFNr, String i_Password, int i_hPort, int i_lPort)
	{
		this.ControllerZNr = inZNr;
		this.ControllerFNr = inFNr;
		this.Controller = new ThhBtpplClass(inIP, i_Password, i_hPort, i_lPort);
		
		int ret = this.Controller.Connect();
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "Unable to connect to TLC. return with error: " + ret);
			return ret;
		}
		
		this.Liste32_Configure();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		TlcGeneralData retData = new TlcGeneralData();
		
		ret = this.GetTESiplOnline(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "GetTESiplOnline return with error: " + ret);
			return ret;
		}
		
		ret = this.DigEngine(retData);
		if (ret != 0)
		{
			LOGGER.log(Level.SEVERE, "DigEngine return with error: " + ret);
			return ret;
		}
		
		skeepEnumEntities = true;
		
		return 0;
	}
	//-------------------------------------------
	
	/**
     * same as connect with OCIT port defaults:
     * i_hPort = 2504
     * i_lPort = 3110  
     * @param inIP the IP of the controller
     * @param inZNr the ZNr of the controller
     * @param inFNr the FNr of the controller
     * @param i_Password the password for authentication to the controller
     * @return 0 if success, negative if failed
     */
	public int Connect(String inIP, int inZNr, int inFNr, String i_Password)
	{
		return this.Connect(inIP, inZNr, inFNr, i_Password, 2504, 3110);
	}
	//-------------------------------------------
	
	/**
     * same as connect with OCIT port defaults and no password:
     * i_hPort = 2504
     * i_lPort = 3110  
     * @param inIP the IP of the controller
     * @param inZNr the ZNr of the controller
     * @param inFNr the FNr of the controller
     * @return 0 if success, negative if failed
     */
	public int Connect(String inIP, int inZNr, int inFNr)
	{
		return this.Connect(inIP, inZNr, inFNr, "", 2504, 3110);
	}
	//-------------------------------------------
	
	/**
     * Disconnect TCP channel of the controller:
     */
	public void Disconnect()
	{
		this.Controller.Disconnect();
	}	
	//--------------------------------------------------------------------
	
	/**
     * Send Telegram on TCP channel to controller @ i_lPort
     * @param Telegram, The Telegram to send
     */
	private void SendTelegram(ThhBtpplTelegramClass Telegram)
	{
		this.Controller.SendTelegram(3110, Telegram);
	}	
	//--------------------------------------------------------------------
	
	public TlcInterface()
	{
	}	
	//--------------------------------------------------------------------
	
	
	/**
     * Configuration with the controller with Liste32
     */
	private void Liste32_Configure()
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		
		Telegram.Path = new byte[1];
		Telegram.Path[0] = 0x20;
		Telegram.Params = new byte[0];
		
		Telegram = this.BuildPack(Telegram, 0, 400, 0);
		this.SendTelegram(Telegram);
		
		Telegram.Path = new byte[2];
		Telegram.Path[0] = 0x20;
		Telegram.Path[1] = 0x00;
		Telegram.Params = new byte[0];
		
		Telegram = this.BuildPack(Telegram, 0, 405, 0);
		this.SendTelegram(Telegram);
		
		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_SHA1;
		Telegram.Path = new byte[1];
		Telegram.Path[0] = 0x20;
		
		Telegram.Params = new byte[4];
		Telegram.Params[0] = 0x00;
		Telegram.Params[1] = 0x01;
		Telegram.Params[2] = 0x01;
		Telegram.Params[3] = (byte) 0x97;
		
		Telegram = this.BuildPack(Telegram, 0, 400, 108);
		this.SendTelegram(Telegram);
		
		Telegram.Path = new byte[2];
		Telegram.Path[0] = 0x20;
		Telegram.Path[1] = 0x01;
		Telegram.Params = new byte[4];
		Telegram.Params[0] = 0x00;
		Telegram.Params[1] = 0x01;
		Telegram.Params[2] = 0x01;
		Telegram.Params[3] = (byte) 0xb6;
		
		Telegram = this.BuildPack(Telegram, 1, 407, 120);
		this.SendTelegram(Telegram);
		
		Telegram.Path = new byte[3];
		Telegram.Path[0] = 0x20;
		Telegram.Path[1] = 0x01;
		Telegram.Path[2] = 0x00;
		Telegram.Params = new byte[1];
		Telegram.Params[0] = 0x00;
		
		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;
		
		Telegram = this.BuildPack(Telegram, 1, 438, 157);
		this.SendTelegram(Telegram);
		
		Telegram.Path = new byte[2];
		Telegram.Path[0] = 0x20;
		Telegram.Path[1] = 0x01;
		Telegram.Params = new byte[8];
		Telegram.Params[0] = 0x00;
		Telegram.Params[1] = 0x00;
		Telegram.Params[2] = 0x00;
		Telegram.Params[3] = (byte) 0x64;
		Telegram.Params[4] = 0x00;
		Telegram.Params[5] = 0x00;
		Telegram.Params[6] = 0x00;
		Telegram.Params[7] = (byte) 0x32;
		
		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_SHA1;
		Telegram = this.BuildPack(Telegram, 1, 407, 130);
		this.SendTelegram(Telegram);
		
		Telegram.Path = new byte[1];
		Telegram.Path[0] = 0x20;
		Telegram.Params = new byte[0];
		
		Telegram = this.BuildPack(Telegram, 0, 400, 105);
		this.SendTelegram(Telegram);
	}
	
	//--------------------------------------------------------------------
	
	/**
     * Get the names and the values of the signal groups
     * @param inTlc, the TlcGeneralData set source
     * @return 0 if success, negative if failed
     */
	private int GetTESiplOnline(TlcGeneralData inTlc)
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		boolean flag=true;
		int iPath = 1;		
		
		Telegram.Path = new byte[3];
		Telegram.Path[0] = 0x20;
		Telegram.Path[1] = 0x01;
		Telegram.Path[2] = 0x00;
		
		Telegram.Params = new byte[0];
		
		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;		
		Telegram = this.BuildPack(Telegram, 1, 438, 151);
		this.SendTelegram(Telegram);
		
		Telegram = ThhBtpplClass.GetLastRespond();
		if (Telegram == null)
		{
			LOGGER.log(Level.SEVERE, "No response from TLC");
			return -1;
		}
		
		if (Telegram.Params.length < 6)
		{
			LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
			return -2;
		}
		
		LampValue = new byte[Telegram.Params[4]];
		System.arraycopy(Telegram.Params, 5, LampValue, 0, Telegram.Params[4]); 
		
		if (!skeepEnumEntities)
		{
		
			LampNames = new String[Telegram.Params[4]];		
			
			while(flag)
			{
				Telegram.Path = new byte[2];
				Telegram.Params = new byte[0];
				System.arraycopy(Telegram.IntToByte(iPath), 2, Telegram.Path, 0, 2);
				
				Telegram = this.BuildPack(Telegram, 1, 501, 0);
				this.SendTelegram(Telegram);
				Telegram = ThhBtpplClass.GetLastRespond();
				if (Telegram == null)
				{
					LOGGER.log(Level.SEVERE, "No response from TLC");
					return -1;
				}
				
				if (Telegram.Params.length < 5)
				{
					LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
					return -2;
				}
				
				if((Telegram.Params[1]) == 0x11)
				{
					flag=false;
				}
				else
				{
					int sLength = Telegram.Params[3];
					byte[] tmpStrBytes = new byte[sLength];
					System.arraycopy(Telegram.Params, 4, tmpStrBytes, 0, sLength);
					LampNames[iPath-1] = new String(tmpStrBytes);
					
					iPath++;
				}
			}
		}
		
		SignalGroupInitialize(inTlc, LampNames, LampValue);
		
		return 0;
	}	
	//--------------------------------------------------------------------

	/**
     * Get the names and the state of the Detectors
     * @param inTlc, the TlcGeneralData set source
     * @return 0 if success, negative if failed
     */
	private int DigEngine (TlcGeneralData inTlc)
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		
		boolean flag=true;
		
		int iPath = 1;
		
		if (!skeepEnumEntities)
		{
			int Size = 0;
			
			Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;
			
			iPath = 1;
			String[] tmpNames = new String[1];
			
			while(flag)
			{
				Telegram.Path = new byte[2];
				Telegram.Params = new byte[0];
				System.arraycopy(Telegram.IntToByte(iPath), 2, Telegram.Path, 0, 2);
				Telegram = this.BuildPack(Telegram, 1, 500, 0);
				this.SendTelegram(Telegram);
				Telegram = ThhBtpplClass.GetLastRespond();
				
				if (Telegram == null)
				{
					LOGGER.log(Level.SEVERE, "No response from TLC");
					return -1;
				}
				
				if (Telegram.Params.length < 5)
				{
					LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
					return -2;
				}
				
				if((Telegram.Params[1]) == 0x11)
				{
					flag = false;
				}
				else
				{
					Size++;
					
					DigEngineNames = new String[Size];
					if (Size > 1)
						System.arraycopy(tmpNames, 0, DigEngineNames, 0, Size - 1);
					
					int sLength = Telegram.Params[3];
					byte[] tmpStrBytes = new byte[sLength];
					System.arraycopy(Telegram.Params, 4, tmpStrBytes, 0, sLength);
					
					DigEngineNames[iPath-1] = new String(tmpStrBytes);
					tmpNames = new String[Size];
					System.arraycopy(DigEngineNames, 0, tmpNames, 0, Size);
									
					iPath++;
					
				}
			}
			
			DigEngineValue = new byte[Size];			
		}
		
		flag = true;
		iPath = 1;
		
		while(flag)
		{
			Telegram.Path = new byte[2];
			Telegram.Params = new byte[0];
			System.arraycopy(Telegram.IntToByte(iPath), 2, Telegram.Path, 0, 2);
			Telegram = this.BuildPack(Telegram, 1, 500, 16);
			this.SendTelegram(Telegram);
			Telegram = ThhBtpplClass.GetLastRespond();
			
			if (Telegram == null)
			{
				LOGGER.log(Level.SEVERE, "No response from TLC");
				return -1;
			}
			
			if (Telegram.Params.length < 3)
			{
				LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
				return -2;
			}
			
			if((Telegram.Params[1]) == 0x11)
			{
				flag = false;
			}
			else
			{
				System.arraycopy(Telegram.Params, 2, DigEngineValue, iPath - 1, 1);				
				iPath++;
			}
		}
		
		DigEngineInitialize(inTlc, DigEngineNames, DigEngineValue);
		
		return 0;
	}	
	//--------------------------------------------------------------------
	
	/**
     * Build the packet controller and OCIT parameters before send
     * @param TelPack, the ThhBtpplTelegramClass set to build
     * @param Member see OCIT-O
     * @param OType see OCIT-O
     * @param Method see OCIT-O
     * @return new ThhBtpplTelegramClass to send
     */
	private ThhBtpplTelegramClass BuildPack(ThhBtpplTelegramClass TelPack, int Member, int OType, int Method)
	{		
		TelPack.Version = (byte)(Integer.parseInt("0"));
		TelPack.Member = (short)(Member & 0xFFFF);
		TelPack.OType = (short)(OType & 0xFFFF);	
		TelPack.Method = (short)(Method & 0xFFFF);
		TelPack.ZNr = (short)this.ControllerZNr;
		TelPack.FNr = (short)this.ControllerFNr;	
		
		TelPack.Type = ThhBtpplTelegramClass.TelegramType_e.TELEGRAM_TYPE_REQUEST;
			
		
		return TelPack;
	}	
	//--------------------------------------------------------------------
	
	/**
     * Get the program number:
     * @param inTlc, the TlcGeneralData set source
     * @return 0 if success, negative if failed
     */
	private int ISignalProgram (TlcGeneralData inTlc)
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		Telegram.Path = new byte[1];
		Telegram.Path[0] = 0x00;
		Telegram.Params = new byte[0];
		
		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;		
		Telegram = this.BuildPack(Telegram, 1, 223, 0);

		this.SendTelegram(Telegram);
		Telegram = ThhBtpplClass.GetLastRespond();
		
		if (Telegram == null)
		{
			LOGGER.log(Level.SEVERE, "No response from TLC");
			return -1;
		}
		
		if (Telegram.Params.length < 7)
		{
			LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
			return -2;
		}
		
		inTlc.ProgramNumber = Telegram.Params[6];
		
		return 0;
	}	
	//--------------------------------------------------------------------

	/**
     * Initialize the hashMap of signal groups
     * @param inTlc, the TlcGeneralData set source
     * @param Names, string array with all signal group names
     * @param Value, byte array string with all signal group state Values
     * @return 0 if success, negative if failed
     */
	private int SignalGroupInitialize(TlcGeneralData inTlc, String[] Names,byte[] Value)
	{
		inTlc.SignalGroups = new HashMap<String, SignalGroupStatus>();
		
		
		for(int i=0; i<Names.length;i++)
		{
			SignalGroupStatus Status = new SignalGroupStatus();
			
			switch (Value[i])
			{
				case 0: {
					
					Status.off = true;
					break;
				}
				case 0x03: {
								
					Status.red = true;
					break;
				}
				
				case 0x04: {
					
					Status.flashing = true;
					break;
				}
				
				case 0x0c: {
					
					Status.yellow = true;
					break;
				}
				
				case 0x30: {
					
					Status.green = true;
					break;
				}
			}
			inTlc.SignalGroups.put(Names[i], Status);
		}	
		
		return 0;
	}
	//--------------------------------------------------------------------
	
	/**
     * Initialize the hashMap of Detectors
     * @param inTlc, the TlcGeneralData set source
     * @param Names, string array with all detector names
     * @param Value, byte array string with all detector Values
     * @return 0 if success, negative if failed
     */
	private int DigEngineInitialize(TlcGeneralData inTlc, String[] Names,byte[] Value)
	{
		inTlc.Detectors = new HashMap<String, DetectorStatus>();
		
		for(int i=0; i<Names.length;i++)
		{
			DetectorStatus Status = new DetectorStatus();
			
			switch (Value[i])
			{
				case 0: {
					
					Status.occupied = false;
					break;
				}
				case 0x01: {
								
					Status.occupied = true;
					break;
				}
			}
			inTlc.Detectors.put(Names[i], Status);
		}	
		return 0;
	}	
	//---------------------------------------------------------------
	
	/**
     * Get the time from the controller by local UTC value
     * @param inTlc the TlcGeneralData set source
     * @return 0 if success, negative if failed
     */
	private int GetTime(TlcGeneralData inTlc)
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		
		Telegram.Path = new byte[0];
		Telegram.Params = new byte[0];

		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;
		
		Telegram = this.BuildPack(Telegram, 0, 815, 103);
		this.SendTelegram(Telegram);
		
		Telegram = ThhBtpplClass.GetLastRespond();
		
		if (Telegram == null)
		{
			LOGGER.log(Level.SEVERE, "No response from TLC");
			return -1;
		}
		
		if (Telegram.Params.length < 11)
		{
			LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
			return -2;
		}
		
		byte[] bUTC = new byte[8];
		System.arraycopy(Telegram.Params, 2, bUTC, 4, 4);		
		long lUTC = ByteBuffer.wrap(bUTC).getLong() * 1000L;		
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getDefault());
		calendar.setTimeInMillis(lUTC);
		
		inTlc.day = (byte)calendar.get(Calendar.DAY_OF_MONTH);
		inTlc.month = (byte)(calendar.get(Calendar.MONTH) + 1);
		inTlc.year = (int)calendar.get(Calendar.YEAR);
		
		inTlc.hour = (byte)calendar.get(Calendar.HOUR);
		inTlc.minute = (byte)calendar.get(Calendar.MINUTE);
		inTlc.second = (byte)calendar.get(Calendar.SECOND);
		
		return 0;
	}
	//------------------------------------------------------------------
	
	/**
     * sets the program number at the controller
     * @param programToSet the program number to set
     * @return 0 if success, negative if failed
     */
	private int setProgramNumber(int programToSet)
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		
		Telegram.Path = new byte[1];
		Telegram.Params = new byte[13];

		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;
		
		long opID = 0xCCC003L + ((long)this.ControllerFNr << 1) + 0x01;
		long startUTC = (long)((System.currentTimeMillis() - 0x0100) / 1000L);
		long endUTC = (long)((System.currentTimeMillis()  + 0xFF00) / 1000L);
		
		System.arraycopy(Telegram.LongToByte(opID), 4, Telegram.Params, 0, 4);
		System.arraycopy(Telegram.LongToByte(startUTC), 4, Telegram.Params, 4, 4);
		System.arraycopy(Telegram.LongToByte(endUTC), 4, Telegram.Params, 8, 4);
		Telegram.Params[12] = (byte)programToSet;
		
		Telegram = this.BuildPack(Telegram, 1, 222, 16);
		this.SendTelegram(Telegram);
		
		Telegram = ThhBtpplClass.GetLastRespond();
		
		if (Telegram == null)
		{
			LOGGER.log(Level.SEVERE, "No response from TLC");
			return -1;
		}
		
		if (Telegram.Params.length < 2)
		{
			LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
			return -2;
		}
		
		if (Telegram.Params[1] != 0x00)
		{
			LOGGER.log(Level.SEVERE, "Telegram return with an error");
			return -3;
		}
		
		return 0;
	}
	//-----------------------------------------------------------
	
	/**
     * Get the "PH"(Stage number) and "TX"(Cycle second) from the controller
     * @param inTlc, the TlcGeneralData set source
     * @return 0 if success, negative if failed
     */
	private int APWertRkUshort (TlcGeneralData inTlc)
	{
		ThhBtpplTelegramClass Telegram = new ThhBtpplTelegramClass();
		Telegram.Path = new byte[6]; 
		Telegram.Path[0] = 0x00;
		Telegram.Path[1] = 0x03;
		Telegram.Path[2] = 0x54;
		Telegram.Path[3] = 0x58;
		Telegram.Path[4] = 0x00;
		Telegram.Path[5] = 0x00;
		Telegram.Params = new byte[0];
		
		Telegram.CsType = ThhBtpplTelegramClass.ChecksumType_e.CS_FLETCHER_ONLY;		
		Telegram = this.BuildPack(Telegram, 1, 511, 16);

		this.SendTelegram(Telegram);
		Telegram = ThhBtpplClass.GetLastRespond();
		
		if (Telegram == null)
		{
			LOGGER.log(Level.SEVERE, "No response from TLC");
			return -1;
		}
		
		if (Telegram.Params.length < 4)
		{
			LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
			return -2;
		}
		
		inTlc.CycleSecond = (short)ByteBuffer.wrap(new byte[] {0, 0, Telegram.Params[2], Telegram.Params[3]}).getInt();
		
		Telegram.Path = new byte[6];
		Telegram.Path[0] = 0x00;
		Telegram.Path[1] = 0x03;
		Telegram.Path[2] = 0x50;
		Telegram.Path[3] = 0x48;
		Telegram.Path[4] = 0x00;
		Telegram.Path[5] = 0x00;
		Telegram.Params = new byte[0];
		
		Telegram = this.BuildPack(Telegram, 1, 511, 16);

		this.SendTelegram(Telegram);
		Telegram = ThhBtpplClass.GetLastRespond();
		
		if (Telegram == null)
		{
			LOGGER.log(Level.SEVERE, "No response from TLC");
			return -1;
		}
		
		if (Telegram.Params.length < 4)
		{
			LOGGER.log(Level.SEVERE, "Telegram recieved with invalid parameter block");
			return -2;
		}
		
		inTlc.StageNumber = (short)ByteBuffer.wrap(new byte[] {0, 0, Telegram.Params[2], Telegram.Params[3]}).getInt();
		
		return 0;
	}	
	//--------------------------------------------------------------------
}
//-------------------------------------------------------------------

