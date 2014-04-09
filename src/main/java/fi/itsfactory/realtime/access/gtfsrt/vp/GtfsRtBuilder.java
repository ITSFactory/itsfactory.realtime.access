package fi.itsfactory.realtime.access.gtfsrt.vp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeLibrary;

import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;

import com.google.protobuf.Message;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.googlecode.protobuf.format.JsonFormat;

public class GtfsRtBuilder {
	private Siri siri;

	public GtfsRtBuilder(Siri siri) {
		this.siri = siri;
	}

	public String buildJson() {
		return JsonFormat.printToString(buildMessage());
	}

	public void writeToOutputStream(OutputStream stream) throws IOException {
		buildMessage().writeTo(stream);
	}

	private Message buildMessage() {
		FeedMessage.Builder feedMessageBuilder = GtfsRealtimeLibrary.createFeedMessageBuilder();

		int entityCounter = 0;
		for (VehicleMonitoringDeliveryStructure vmd : siri.getServiceDelivery()
				.getVehicleMonitoringDelivery()) {
			for (VehicleActivityStructure va : vmd.getVehicleActivity()) {
				MonitoredVehicleJourneyStructure mvj = va.getMonitoredVehicleJourney();
				FramedVehicleJourneyRefStructure fvj = mvj.getFramedVehicleJourneyRef();

				VehiclePosition.Builder vp = VehiclePosition.newBuilder();

				TripDescriptor.Builder tdBuilder = TripDescriptor.newBuilder();
				tdBuilder.setRouteId(mvj.getLineRef().getValue());
				tdBuilder.setStartDate(fvj.getDataFrameRef().getValue().replace("-", ""));

				StringBuffer timeBuffer = new StringBuffer(8);
				timeBuffer.append(fvj.getDatedVehicleJourneyRef().substring(0, 2));
				timeBuffer.append(":");
				timeBuffer.append(fvj.getDatedVehicleJourneyRef().substring(2));
				timeBuffer.append(":00");
				tdBuilder.setStartTime(timeBuffer.toString());

				vp.setTrip(tdBuilder.build());

				VehicleDescriptor.Builder vdBuilder = VehicleDescriptor.newBuilder();
				vdBuilder.setId(mvj.getVehicleRef().getValue());
				VehicleDescriptor vd = vdBuilder.build();

				vp.setVehicle(vd);

				Date date = va.getRecordedAtTime().toGregorianCalendar().getTime();
				vp.setTimestamp(date.getTime() / 1000);

				Position.Builder position = Position.newBuilder();
				position.setLatitude(mvj.getVehicleLocation().getLatitude().floatValue());
				position.setLongitude(mvj.getVehicleLocation().getLongitude().floatValue());
				position.setBearing(mvj.getBearing());
				vp.setPosition(position);

				FeedEntity.Builder entity = FeedEntity.newBuilder();
				entity.setId(Integer.toString(entityCounter++));

				entity.setVehicle(vp);

				feedMessageBuilder.addEntity(entity);
			}
		}
		return feedMessageBuilder.build();
	}
}
