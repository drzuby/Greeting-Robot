package pl.edu.agh.capo.controller;

import pl.edu.agh.amber.common.AmberClient;
import pl.edu.agh.amber.hokuyo.HokuyoProxy;
import pl.edu.agh.amber.hokuyo.MapPoint;
import pl.edu.agh.amber.hokuyo.Scan;
import pl.edu.agh.amber.roboclaw.RoboclawProxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.joining;

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

	public static final double TARGET_DISTANCE = 700;

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
		int counter = 0;
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

			this.monitorThread.interrupt();
			SetCapoVelocity(0,0);

			List<ScannedEntity> scannedEntities = detectEntities(scanPoints);

            /* Debug: print all ranges */
			System.out.println(scannedEntities.size());
			System.out.println(scannedEntities.stream()
					.map(ScannedEntity::toString)
					.collect(joining(",\n"))
			);

			// TODO: scannedEntities:
			// 	filter by width [human leg],
			// 	find 2 points close enough to each other,
			// 	Assume human, go towards it (old capo code)

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				continue;
			}
			
		}				
	
	}

	/* Split into ranges of similar distance */
	private List<ScannedEntity> detectEntities(List<MapPoint> scanPoints) {
		if (scanPoints.isEmpty()) return Collections.emptyList();
		List<ScannedEntity> scannedEntities = new ArrayList<>();
		MapPoint prev = scanPoints.get(0);
		ScannedEntity current = new ScannedEntity(prev);
		int n = 1;

		double dR;
		for (MapPoint p : scanPoints.subList(1,scanPoints.size())) {
            dR = (p.getDistance() - prev.getDistance()) /
                    (p.getAngle() - prev.getAngle());
            if (Math.abs(dR) > 300) {
                current.endAngle = p.getAngle();
                current.avgDistance /= n;

                if (current.getWidth() > 1) scannedEntities.add(current);

                current = new ScannedEntity(p);
                n = 1;
            } else {
                current.avgDistance += p.getDistance();
                n++;
            }
            prev = p;
        }
		current.endAngle = scanPoints.get(scanPoints.size()-1).getAngle();
		current.avgDistance /= n;
		if (current.getWidth() > 1) scannedEntities.add(current);
		return scannedEntities;
	}

	private static class ScannedEntity {
		public double startAngle;
		public double endAngle;
		public double avgDistance;

		public ScannedEntity(MapPoint point) {
			this.startAngle = point.getAngle();
			this.avgDistance = point.getDistance();
		}

		public double getWidth() {
			return avgDistance * Math.sqrt(2 * (1 - Math.cos(Math.toRadians(startAngle - endAngle))));
		}

		@Override
		public String toString() {
			return "[" +
					"" + startAngle +
					", " + endAngle +
					", " + avgDistance +
					", " + getWidth() +
					"]";
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
