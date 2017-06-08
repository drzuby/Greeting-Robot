package greeting.robot.capo;

public class SensorLoopMonitorThread
  implements Runnable
{
  private static final double maxSensorRefreshTimeSeconds = 0.3D;
  private boolean stop = false;
  private final CapoController capoController;
  
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
