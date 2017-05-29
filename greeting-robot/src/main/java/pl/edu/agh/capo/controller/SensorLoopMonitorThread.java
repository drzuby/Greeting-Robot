package pl.edu.agh.capo.controller;

public class SensorLoopMonitorThread
  implements Runnable
{
  public static final double maxSensorRefreshTimeSeconds = 0.3D;
  protected boolean stop = false;
  protected CapoController capoController;
  
  public void Stop()
  {
    this.stop = true;
  }
  
  public SensorLoopMonitorThread(CapoController capoController)
  {
    this.capoController = capoController;
  }
  
  public void run()
  {
    while (!this.stop)
    {
      Thread.interrupted();
      try
      {
        Thread.sleep((int)(maxSensorRefreshTimeSeconds*1000));
      }
      catch (InterruptedException e)
      {
        continue;
      }
      this.capoController.reduceSpeedDueToSensorReadingTimeout();
    }
  }
}
