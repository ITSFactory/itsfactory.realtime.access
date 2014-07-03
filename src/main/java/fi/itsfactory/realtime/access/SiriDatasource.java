package fi.itsfactory.realtime.access;

public interface SiriDatasource {
	public String getVehicleMonitoringData(String lineRef, String vehicleRef);
	public String getGeneralMessageData();
	public void initialize();
	public void shutdown();
}
