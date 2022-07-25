public class TestTlc
{
	public static void main(String Args[])
		{
			TlcInterface Tlc = new TlcInterface();
			
			boolean connected = false;
			while (!connected)
			{
				int ret = Tlc.Connect("127.0.0.1", 11, 1096, "SIEMENSKEY");
				if (ret < 0)
				{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
					break;
			}
			
			TlcInterface.TlcGeneralData TlcData;
			
			for (int i = 0; i < 100; i++)
			{
				TlcData = Tlc.readGeneralData();
				Tlc.PrintTlcData(TlcData);
			}		
			
			Tlc.Disconnect();
		}
}
//------------------------------------------------------