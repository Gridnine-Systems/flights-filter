package com.gridnine.testing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factory class to get sample list of flights.
 */
class FlightBuilder {
    static List<Flight> createFlights() {
        LocalDateTime threeDaysFromNow = LocalDateTime.now().plusDays(3);
        return Arrays.asList(
                //A normal flight with two hour duration
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2)),
                //A normal multi segment flight
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2),
                        threeDaysFromNow.plusHours(3), threeDaysFromNow.plusHours(5)),
                //A flight departing in the past
                createFlight(threeDaysFromNow.minusDays(6), threeDaysFromNow),
                //A flight that departs before it arrives
                createFlight(threeDaysFromNow, threeDaysFromNow.minusHours(6)),
                //A flight with more than two hours ground time
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2),
                        threeDaysFromNow.plusHours(5), threeDaysFromNow.plusHours(6)),
                //Another flight with more than two hours ground time
                createFlight(threeDaysFromNow, threeDaysFromNow.plusHours(2),
                        threeDaysFromNow.plusHours(3), threeDaysFromNow.plusHours(4),
                        threeDaysFromNow.plusHours(6), threeDaysFromNow.plusHours(7)));
    }

    private static Flight createFlight(final LocalDateTime... dates) {
        if ((dates.length % 2) != 0) {
            throw new IllegalArgumentException(
                    "you must pass an even number of dates");
        }
        List<Segment> segments = new ArrayList<>(dates.length / 2);
        for (int i = 0; i < (dates.length - 1); i += 2) {
            segments.add(new Segment(dates[i], dates[i + 1]));
        }
        return new Flight(segments);
    }
}

/**
 * Bean that represents a flight.
 */
class Flight {
    private final List<Segment> segments;

    Flight(final List<Segment> segs) {
        segments = segs;
    }

    List<Segment> getSegments() {
        return segments;
    }

    @Override
    public String toString() {
        return segments.stream().map(Object::toString)
                .collect(Collectors.joining(" "));
    }
}

/**
 * Bean that represents a flight segment.
 */
class Segment {
    private final LocalDateTime departureDate;

    private final LocalDateTime arrivalDate;

    Segment(final LocalDateTime dep, final LocalDateTime arr) {
        departureDate = Objects.requireNonNull(dep);
        arrivalDate = Objects.requireNonNull(arr);
    }

    LocalDateTime getDepartureDate() {
        return departureDate;
    }

    LocalDateTime getArrivalDate() {
        return arrivalDate;
    }

    @Override
    public String toString() {
        DateTimeFormatter fmt =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        return '[' + departureDate.format(fmt) + '|' + arrivalDate.format(fmt)
                + ']';
    }
}

class Main {

    public static void main(String[] args) {

        List<Flight> flights = FlightBuilder.createFlights();

        List<Rule> rules = new ArrayList<>();
        rules.add(new DepartureBeforeNowRule());
        rules.add(new ArrivalBeforeDepartureRule());
        rules.add(new LongGroundTimeRule());

        for (Rule rule : rules) {
            System.out.println(rule.getDescription());
            filterFlights(flights, rule).forEach(System.out::println);
        }
    }

    private static List<Flight> filterFlights(List<Flight> flights, Rule rule) {
        List<Flight> filteredFlights = new ArrayList<>();
        for (Flight flight : flights) {
            if (rule.matches(flight)) {
                filteredFlights.add(flight);
            }
        }
        return filteredFlights;
    }
}

interface Rule {
    String getDescription();
    boolean matches(Flight flight);
}

class DepartureBeforeNowRule implements Rule {

    @Override
    public String getDescription(){
        return "\nВылет до текущего момента времени:";
    }

    @Override
    public boolean matches(Flight flight) {
        LocalDateTime now = LocalDateTime.now();
        for (Segment segment : flight.getSegments()) {
            if (segment.getDepartureDate().isBefore(now)) {
                return true;
            }
        }
        return false;
    }
}

class ArrivalBeforeDepartureRule implements Rule {

    @Override
    public String getDescription(){
        return "\nИмеются сегменты с датой прилёта раньше даты вылета:";
    }

    @Override
    public boolean matches(Flight flight) {
        for (Segment segment : flight.getSegments()) {
            if (segment.getArrivalDate().isBefore(segment.getDepartureDate())) {
                return true;
            }
        }
        return false;
    }
}

class LongGroundTimeRule implements Rule {

    @Override
    public String getDescription(){
        return "\nОбщее время, проведённое на земле, превышает два часа:";
    }

    @Override
    public boolean matches(Flight flight) {
        long groundTime = 0;
        for (int i = 0; i < flight.getSegments().size() - 1; i++) {
            Segment currentSegment = flight.getSegments().get(i);
            Segment nextSegment = flight.getSegments().get(i + 1);
            groundTime += java.time.Duration.between(currentSegment.getArrivalDate(), nextSegment.getDepartureDate()).toMinutes();
        }
        return groundTime > 120;
    }
}
