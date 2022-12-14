package entity;

import java.time.LocalDateTime;

public record Flight(
        Long id,
        String flightNo,
        LocalDateTime departureDate,
        String arrivalDate,
        LocalDateTime arrival_date, String arrival_airport_code, Integer aircraftId,
        String status
) {
}
