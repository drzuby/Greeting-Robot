package pl.edu.agh.capo.app;

import java.io.IOException;

import pl.edu.agh.capo.controller.CapoController;
import pl.edu.agh.capo.controller.SensorLoopMonitorThread;

public class CapoApp
{
  public static void main(String[] args)
    throws IOException
  {
	  
	  System.out.println("\n\n\n Two params can be used: robotId maxSpeed\n eg 203 0.6\n\n");
	  int robotId = -1;
	  
		if (args.length > 0)
		{	try {
			robotId = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
			}
		}	
			
		double defaultMaxSpeed = 1.5D;
		if (args.length > 1)
		{	try {
			defaultMaxSpeed = Double.parseDouble(args[1]);
			} catch (NumberFormatException e) {
			}
		}	
		
		CapoController capoController = null;
	if (robotId >= 200 && robotId < 254)	
		capoController = new CapoController("192.168.2."+robotId, defaultMaxSpeed);
	else
		capoController = new CapoController("127.0.0.1", defaultMaxSpeed);
    SensorLoopMonitorThread sensorLoopMonitorThread = new SensorLoopMonitorThread(capoController);
    
    Thread monitorThread = new Thread(sensorLoopMonitorThread);
    capoController.SetMonitoThread(monitorThread);
    Thread controllerThread = new Thread(capoController);
    
    controllerThread.start();
    monitorThread.start();
    
    System.in.read();
    System.out.println("STOP");
    sensorLoopMonitorThread.Stop();
    capoController.Stop();
    try
    {
      controllerThread.join();
    }
    catch (InterruptedException localInterruptedException) {}
    capoController.Stop();
  }
}
