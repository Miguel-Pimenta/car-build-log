package com.miguelpimenta.buildlog.dyno;

import com.miguelpimenta.buildlog.dyno.dto.DynoRequest;
import com.miguelpimenta.buildlog.dyno.dto.DynoResponse;
import com.miguelpimenta.buildlog.vehicle.Vehicle;
import org.springframework.stereotype.Component;

/** Maps between {@link DynoResult} entities and their DTOs. */
@Component
public class DynoMapper {

    public DynoResult toEntity(DynoRequest request, Vehicle vehicle) {
        DynoResult result = new DynoResult();
        result.setVehicle(vehicle);
        result.setPowerHp(request.powerHp());
        result.setTorqueNm(request.torqueNm());
        result.setMeasuredAt(request.measuredAt());
        result.setNotes(request.notes());
        return result;
    }

    public DynoResponse toResponse(DynoResult result) {
        return new DynoResponse(
                result.getId(),
                result.getVehicle().getId(),
                result.getPowerHp(),
                result.getTorqueNm(),
                result.getMeasuredAt(),
                result.getNotes(),
                result.getCreatedAt()
        );
    }
}
