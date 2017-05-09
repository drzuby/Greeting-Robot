package pl.edu.agh.capo.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.hokuyo.HokuyoProxy;
import pl.edu.agh.amber.hokuyo.MapPoint;
import pl.edu.agh.amber.hokuyo.Scan;
import pl.edu.agh.amber.roboclaw.RoboclawProxy;

public class CapoController
implements Runnable
{

	protected double maxVelocity = 1.0D;
	protected double currentVelocityLeft;
	protected double currentVelocityRight;

	protected AmberClient client;
	protected RoboclawProxy roboclawProxy;
	protected HokuyoProxy hokuyoProxy;
	protected Thread monitorThread;
	
	protected boolean isRun = true;

	public CapoController(String robotIP, double maxVelocity)
			throws IOException
	{
		if (maxVelocity < 2 && maxVelocity > 0)
			this.maxVelocity = maxVelocity;
		
		this.client = new AmberClient(robotIP, 26233);
		this.roboclawProxy = new RoboclawProxy(this.client, 0);
		this.hokuyoProxy = new HokuyoProxy(this.client, 0);
	}

	public void SetMonitoThread(Thread monitorThread)
	{
		this.monitorThread = monitorThread;
	}
	
	public void Stop()
	{
		this.isRun = false;
		SetCapoVelocity(0.0D, 0.0D);
	}

	/**
	 * Controller thread - main control loop here
	 */
	public void run()
	{
		while (this.isRun)
		{
			Scan scan;
			try
			{
				scan = this.hokuyoProxy.getSingleScan();
			}
			catch (IOException e)
			{

				SetCapoVelocity(0.0D, 0.0D);
				System.out.println("FATAL Exception in hokuyoProxy.getSingleScan(): " + e.getMessage()); 
				return;
			}
			
			List<MapPoint> scanPoints;
			try
			{
				scanPoints = scan.getPoints();
			}
			catch (Exception e)
			{
				SetCapoVelocity(0.0D, 0.0D);
				System.out.println("Exception in scan.getPoints: " + e.getMessage());
				continue;
			}



			Optional<MapPoint> min = scanPoints.stream()
					.filter(mapPoint -> mapPoint.getAngle() < 60 && mapPoint.getAngle() > -60
							&& mapPoint.getDistance() > 500 && mapPoint.getDistance() < 5000)
					.min(Comparator.comparing(MapPoint::getDistance));

			final double targetDistance = 700;
			if (min.isPresent()) {
				this.monitorThread.interrupt();
				double angle = min.get().getAngle();
				double distance = min.get().getDistance();
				double deltaDistance = distance - targetDistance;

				double forwardVelocity = deltaDistance / 1000;
				double turn = angle / 100;
				SetCapoVelocity(forwardVelocity + turn, forwardVelocity - turn);

			}
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
//
//				Add your code here			
//			
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////
			
			

			
//			SetCapoVelocity(0.1, -0.1);
			
		}				
	
	}


	/**
	 * Sets the velocity of the robot
	 * 
	 * @param vLeft - left side velocity in m/s
	 * @param vRight - right side velocity in m/s
	 */
	protected synchronized void SetCapoVelocity(double vLeft, double vRight)
	{
		if (vLeft > maxVelocity) vLeft = maxVelocity;
		if (-vLeft > maxVelocity) vLeft = -maxVelocity;
		if (vRight > maxVelocity) vRight = maxVelocity;
		if (-vRight > maxVelocity) vRight = -maxVelocity;
		this.currentVelocityLeft = vLeft;
		this.currentVelocityRight = vRight;
		System.out.println("At: " + System.currentTimeMillis() + " set velocity from tread " + Thread.currentThread().getId() + ": left=" + vLeft + "; right=" + vRight);
		try
		{
			this.roboclawProxy.sendMotorsCommand((int)(vLeft * 1000.0D), (int)(vRight * 1000.0D), (int)(vLeft * 1000.0D), (int)(vRight * 1000.0D));
		}
		catch (Exception e)
		{
			System.out.println("Exception in roboclawProxy.sendMotorsCommand: " + e.getMessage());
		}
	}
	
	
	/**
	 * Reduces the velocity - divides it by 2. 
	 * Called by the sensor monitoring thread when reading is late
	 * 
	 */
	public void reduceSpeedDueToSensorRedingTimeout()
	{
		System.out.print("-> reduceSpeedDueToSensorRedingTimeou  ");
		SetCapoVelocity(this.currentVelocityLeft / 2.0D, this.currentVelocityRight / 2.0D);
	}
}
