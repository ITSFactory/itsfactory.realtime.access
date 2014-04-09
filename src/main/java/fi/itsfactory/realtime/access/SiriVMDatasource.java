package fi.itsfactory.realtime.access;

public interface SiriVMDatasource {
	public String getVehicleMonitoringData(String lineRef, String vehicleRef);
	public void initialize();
	public void shutdown();
}
