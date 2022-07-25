import java.nio.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.*;

public class ThhBtpplTelegramClass 
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
	
	public int				BL;
	public byte				HdrLen;
	public TelegramType_e	Type;
	public byte				Version;
	public ChecksumType_e	CsType;
	public short			JobTime;
	public short			JobTimeCount;
	public short			Member;
	public short			OType;
	public short			Method;
	public short			ZNr;
	public short			FNr;
	public byte				Path[];
	public byte				Params[];
	public int				UTC;
	public byte				CsSha1[];
	public byte				CsFletcher[];
	public String			Password;
	//------------------------- 
	
	public enum TelegramType_e
	{
		TELEGRAM_TYPE_REQUEST,
		TELEGRAM_TYPE_RESPOND,
		TELEGRAM_TYPE_MESSAGE,
		TELEGRAM_TYPE_UNKNOWN
	}
	//----------------------------------------
	
	public enum ChecksumType_e
	{
		CS_FLETCHER_ONLY,
		CS_FLETCHER_SHA1, 
		CS_FLETCHER_UNKNOWN
	}
	//----------------------------------------
	
	/**
     * Fletcher16 is used to represent the running value of a 16-bit Fletcher's Checksum of series of bytes
     * @param inBuf, the buffer
     * @param inBufLength, the buffer length
     * @return The fletcher
     */
	private static int fletcher16OCIT(byte[] inBuf, int inBufLength)
	{
		byte[] data = new byte[inBufLength - 4];
		int length = inBufLength - 4;
		
		System.arraycopy(inBuf, 4, data, 0, length);
		
		int modulus = (int)0xFF;
		int c0 = 0;
		int c1 = 0;
		byte[] retC = new byte[2];
		retC[0] = 0;
		retC[1] = 0;
		
		for (int i = 0; i < length; i++)
		{
			c0 = ((c0 & 0xFF) + ((int)data[i] & 0xFF)) % modulus;
			c1 = ((c1 & 0xFF) + (c0 & 0xFF)) % modulus;
		}
		
		int f0 = modulus - ((c0 + c1) % modulus);
		int f1 = c1;
		int f1f0 = (f0 << 8) + f1;
		
		return f1f0; 
	}
	//----------------------------------------------
	
	/**
     * SHA is for secure hashing algorithm
     * @param convertme, the block to be sign according to OCIT-O
     * @return byte array of the SHA
     */
	public static byte[] SHAsum(byte[] convertme) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("SHA-1"); 
		return (md.digest(convertme));
	}
	//---------------------------------------------
	
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	
	/**
     * Converting byte to hex string
     * @param bytes, byte array to be converted
     * @return string of the hex
     */
	public static String bytesToHex(byte[] bytes) 
	{
		if (bytes == null)
		{return new String("");}
		
		if (bytes.length == 0)
		{return new String("");}
	
		char[] hexChars = new char[bytes.length * 3];
		for (int j = 0; j < bytes.length; j++) 
		{
			int v = bytes[j] & 0xFF;
			hexChars[j * 3] = HEX_ARRAY[v >>> 4];
			hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
			hexChars[j * 3 + 2] = ' ';
		}
		
		return new String(hexChars);
	}
	//------------------------------------------------
	
	public ThhBtpplTelegramClass()
	{
		BL = -1;
		HdrLen = -1;
		Type = TelegramType_e.TELEGRAM_TYPE_REQUEST;
		Version = 1;
		CsType = ChecksumType_e.CS_FLETCHER_ONLY;
		JobTime = 1;
		JobTimeCount = 0;
		Member = -1;
		OType = -1;
		Method = -1;
		ZNr = -1;
		FNr = -1;
		UTC = -1;
		CsSha1 = new byte[20];
		CsFletcher = new byte[2];
		Password = "";
		
		LOGGER.setFilter(new LogFilter());
	}
	//-------------------------
	
	/**
     * Converting integer to byte array
     * @param inInt, integer to be converted
     * @return byte array of the integer
     */
	public byte[] IntToByte(int inInt)
	{
		return ByteBuffer.allocate(4).putInt(inInt).array();
	}
	//------------------------------------------
	
	/**
     * Converting long to byte array
     * @param inLong, long to be converted
     * @return byte array of the long
     */
	public byte[] LongToByte(long inLong)
	{
		return ByteBuffer.allocate(8).putLong(inLong).array();
	}
	//------------------------------------------
	
	/**
     * Converting Short to byte array
     * @param inShort, Short to be converted
     * @return byte array of the Short
     */
	public byte[] ShortToByte(short inShort)
	{
		return ByteBuffer.allocate(2).putShort(inShort).array();
	}
	//------------------------------------------
	
	/**
     * Take Telegram and create him to byte stream
     * @return The Telegram in byte array
     */
	public byte[] toByteStream()
	{
		byte[] retVal;
		
		if (BL == 0)
		{
			retVal = new byte[4];
			// BL
			System.arraycopy(IntToByte(BL), 0, retVal, 0, 4);
			return retVal;
		}
		
		// calc HdrLen and BL
		HdrLen = (byte)(Path.length + 16);
		if (CsType == ChecksumType_e.CS_FLETCHER_ONLY)
		{
			BL = HdrLen + Params.length + 2;
		}
		else
		{
			BL = HdrLen + Params.length + 4 + CsSha1.length + 2;
		}
		
		// alloc buffer
		retVal = new byte[BL + 4];
		int retIndex = 0;
		
		// BL
		System.arraycopy(IntToByte(BL), 0, retVal, retIndex, 4);
		retIndex = retIndex + 4;
		
		// HdrLen
		retVal[retIndex] = HdrLen;
		retIndex = retIndex + 1;
		
		// Flags
		byte Flags = 0;
		switch (Type)
		{
			case TELEGRAM_TYPE_REQUEST:
				Flags = 0x00;
				break;
				
			case TELEGRAM_TYPE_RESPOND:
				Flags = 0x20;
				break;
			
			case TELEGRAM_TYPE_MESSAGE:
				Flags = 0x40;
				break;
				
			default:
				break;
		}
		
		Flags = (byte)(Flags + ((Version & 0x03) << 3));
		
		if (CsType == ChecksumType_e.CS_FLETCHER_SHA1)
		{
			Flags = (byte)(Flags + 0x01);
		}
		
		retVal[retIndex] = Flags;
		retIndex = retIndex + 1;
		
		// JobTime
		System.arraycopy(ShortToByte(JobTime), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// JobTimeCount
		System.arraycopy(ShortToByte(JobTimeCount), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// Member
		System.arraycopy(ShortToByte(Member), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// OType
		System.arraycopy(ShortToByte(OType), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// Method
		System.arraycopy(ShortToByte(Method), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// ZNr
		System.arraycopy(ShortToByte(ZNr), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// FNr
		System.arraycopy(ShortToByte(FNr), 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		// Path
		System.arraycopy(Path, 0, retVal, retIndex, Path.length);
		retIndex = retIndex + Path.length;
		
		// Param block
		System.arraycopy(Params, 0, retVal, retIndex, Params.length);
		retIndex = retIndex + Params.length;
		
		// SHA-1 Checksum
		if (CsType == ChecksumType_e.CS_FLETCHER_SHA1)
		{
			// UTC
			UTC = (int)(System.currentTimeMillis() / 1000L);
			
			System.arraycopy(IntToByte(UTC), 0, retVal, retIndex, 4);
			retIndex = retIndex + 4;
			
			//SHA-1 Block			
			byte[] ShaBlock = new byte[64 + retIndex - 4 + Password.length()];
			byte[] bPassword = new byte[64];
			byte[] tmpbPassword = Password.getBytes();
			
			for (int i = 0; i < 64; i++)
				bPassword[i] = 0x00;
			
			System.arraycopy(tmpbPassword, 0, bPassword, 0, Password.length());
			System.arraycopy(bPassword, 0, ShaBlock, 0, 64);
			System.arraycopy(retVal, 4, ShaBlock, 64, retIndex - 4);
			System.arraycopy(tmpbPassword, 0, ShaBlock, 64 + retIndex - 4, tmpbPassword.length);
			
			try
			{
				CsSha1 = SHAsum(ShaBlock);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "ERROR SHA1 [" + e.getMessage() + "]");
			}			
			
			// SHA-1
			System.arraycopy(CsSha1, 0, retVal, retIndex, CsSha1.length);
			retIndex = retIndex + CsSha1.length;
		}
		
		int tmpFletcher = fletcher16OCIT(retVal, retIndex);
		System.arraycopy(IntToByte(tmpFletcher), 2, CsFletcher, 0, 2);
		
		// Fletcher
		System.arraycopy(CsFletcher, 0, retVal, retIndex, 2);
		retIndex = retIndex + 2;
		
		LOGGER.info("sending telegram bytes. size: " + retIndex + "; data : " + bytesToHex(retVal));
		
		return retVal;
	}
	//------------------------
	
	/**
     * Btppl Logger, logger of The Telegram packeg
     */
	public void print()
	{
		LOGGER.info("\t BL = " + BL);
		LOGGER.info("\t HdrLen = " + HdrLen);
		switch (Type)
		{
			case TELEGRAM_TYPE_REQUEST:
				LOGGER.info("\t Type = Request");
				break;
				
			case TELEGRAM_TYPE_RESPOND:
				LOGGER.info("\t Type = Respond");
				break;
			
			case TELEGRAM_TYPE_MESSAGE:
				LOGGER.info("\t Type = Message");
				break;
				
			case TELEGRAM_TYPE_UNKNOWN:
				LOGGER.info("\t Type = Unknown");
				break;
		}
		LOGGER.info("\t Version = " + Version);
		LOGGER.info("\t HdrLen = " + HdrLen);
		switch (CsType)
		{
			case CS_FLETCHER_ONLY:
				LOGGER.info("\t Checksum type = Fletcher only");
				break;
				
			case CS_FLETCHER_SHA1:
				LOGGER.info("\t Checksum Type = Fletcher + SHA-1");
				break;
				
			case CS_FLETCHER_UNKNOWN:
				LOGGER.info("\t Checksum Type = Unknown");
				break;
		}
		LOGGER.info("\t JobTime = " + JobTime);
		LOGGER.info("\t JobTimeCount = " + JobTimeCount);
		LOGGER.info("\t Member = " + Member);
		LOGGER.info("\t OType = " + OType);
		LOGGER.info("\t Method = " + Method);
		LOGGER.info("\t ZNr = " + ZNr);
		LOGGER.info("\t FNr = " + FNr);
		LOGGER.info("\t Path = " + bytesToHex(Path));
		LOGGER.info("\t ParamBlock = " + bytesToHex(Params));
		if (CsType == ChecksumType_e.CS_FLETCHER_SHA1)
		{
			LOGGER.info("\t UTC = " + (long)UTC);
			LOGGER.info("\t SHA-1 = " + bytesToHex(CsSha1));
		}
		LOGGER.info("\t Fletcher = " + bytesToHex(CsFletcher));
	}
	//-------------------------
	
	/**
     * Take byte stream and create Telegram
     * @param inBfr byte array of the buffer
     * @return 0 - OK
     */
	public int fromByteStream(byte[] inBfr)
	{
		if (inBfr == null)
		{
			LOGGER.log(Level.SEVERE, "Telegram parser error, input buffer is NULL");
			return - 1;
		}
	
		if (inBfr.length < 6)
		{
			LOGGER.log(Level.SEVERE, "Telegram parser error, input buffer is incomplete");
			return - 2;
		}
		
		LOGGER.info("Parsing telegram bytes. size: " + inBfr.length + "; data : " + bytesToHex(inBfr));
		
		int bfrIndex = 0;
		
		// BL
		BL = ByteBuffer.wrap(inBfr, bfrIndex, 4).getInt();
		bfrIndex = bfrIndex + 4;
		
		// HdrLen
		HdrLen = inBfr[bfrIndex];
		bfrIndex = bfrIndex + 1;
				
		try
		{
			// Flags
			byte Flags = inBfr[bfrIndex];
			switch (Flags >> 5)
			{
				case 0 :
					Type = TelegramType_e.TELEGRAM_TYPE_REQUEST;
					break;
					
				case 1 :
					Type = TelegramType_e.TELEGRAM_TYPE_RESPOND;
					break;
					
				case 2 :
					Type = TelegramType_e.TELEGRAM_TYPE_MESSAGE;
					break;
					
				default:
					Type = TelegramType_e.TELEGRAM_TYPE_UNKNOWN;
					break;
			}
			Version = (byte)((Flags >> 3) & 3);
			switch (Flags & 0x01)
			{
				case 0:
					CsType = ChecksumType_e.CS_FLETCHER_ONLY;
					break; 
					
				case 1:
					CsType = ChecksumType_e.CS_FLETCHER_SHA1;
					break; 
				
				default:
					CsType = ChecksumType_e.CS_FLETCHER_UNKNOWN;
					break; 
				
			}
			bfrIndex = bfrIndex + 1;
			
			// JobTime
			JobTime = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// JobTimeCount
			JobTimeCount = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// Member
			Member = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// OType
			OType = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// Method
			Method = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// ZNr
			ZNr = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// FNr
			FNr = (short)ByteBuffer.wrap(new byte[] {0, 0, inBfr[bfrIndex], inBfr[bfrIndex +1]}).getInt();
			bfrIndex = bfrIndex + 2;
			
			// Path
			int ptBlockSize = HdrLen - 16;
			if (ptBlockSize > 0)
			{
				Path = new byte[ptBlockSize];
				System.arraycopy(inBfr, bfrIndex, Path, 0, ptBlockSize);
				bfrIndex = bfrIndex + ptBlockSize;
			}
			
			// ParameterBlock
			int pBlockSize = BL - HdrLen - 2;
			if (pBlockSize > 0)
			{
				if (CsType == ChecksumType_e.CS_FLETCHER_SHA1)
				{
					pBlockSize = BL - HdrLen - 26;
				}
				Params = new byte[pBlockSize];
				System.arraycopy(inBfr, bfrIndex, Params, 0, pBlockSize);
				bfrIndex = bfrIndex + pBlockSize;
			}
			// SHA-1 checksun
			if (CsType == ChecksumType_e.CS_FLETCHER_SHA1)
			{
				// UTC
				UTC = ByteBuffer.wrap(inBfr, bfrIndex, 4).getInt();
				bfrIndex = bfrIndex + 4;
				
				// SHA-1
				System.arraycopy(inBfr, bfrIndex, CsSha1, 0, CsSha1.length);
				bfrIndex = bfrIndex + CsSha1.length;
			}
			
			// Fletcher checksun
			System.arraycopy(inBfr, bfrIndex, CsFletcher, 0, CsFletcher.length);
			
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "ERROR [" + e.getMessage() + "]: while parsing packet,");
			return -3;
		}
		
		return 0;
	}
	//-----------------------------
	
}