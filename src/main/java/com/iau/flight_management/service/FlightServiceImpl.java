package com.iau.flight_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iau.flight_management.model.dto.Airport;
import com.iau.flight_management.model.dto.FlightDTO;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@RequiredArgsConstructor
@Service
public class FlightServiceImpl implements FlightService{

    private final static String THY_IATA = "TK";
    private final static String THY_ICAO = "THY";


   /* @Override
    public List<Airport> getAirports() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://airlabs.co/api/v9/airports?_fields=name,iata_code,icao_code&api_key=c7e5b46c-cab6-4b7c-985e-eef74ed6caf4"))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        JSONObject json = new JSONObject(response.body());
        JSONArray jsonArray = json.getJSONArray("response");

        List<Airport> airports = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Airport airport = objectMapper.readValue(jsonObject.toString(), Airport.class); //The toString method is called on each JSONObject to convert it to a JSON string that can be parsed by the readValue method.
            airports.add(airport);
        }
        return airports;
    }*/

    @Override
    public HashMap<String, String> extractSearchParameters(MultiValueMap<String, String> formData) {
        HashMap<String, String> searchParameters = new HashMap<>();

        searchParameters.put("flightType", formData.getFirst("flight-type"));
        searchParameters.put("departureAirport", formData.getFirst("departure-airport"));
        searchParameters.put("departureCity", formData.getFirst("departure-airport-codes").split(",")[0]);
        searchParameters.put("departureAirportIataCode", formData.getFirst("departure-airport-codes").split(",")[1]);
        searchParameters.put("departureAirportIcaoCode", formData.getFirst("departure-airport-codes").split(",")[2]);
        searchParameters.put("arrivalAirport", formData.getFirst("arrival-airport"));
        searchParameters.put("arrivalCity", formData.getFirst("arrival-airport-codes").split(",")[0]);
        searchParameters.put("arrivalAirportIataCode", formData.getFirst("arrival-airport-codes").split(",")[1]);
        searchParameters.put("arrivalAirportIcaoCode", formData.getFirst("arrival-airport-codes").split(",")[2]);
        searchParameters.put("airlineIataCode", THY_IATA);
        searchParameters.put("airlineIcaoCode", THY_ICAO);
        searchParameters.put("departureDate", formData.getFirst("departure-date"));
        searchParameters.put("returnDate", formData.getFirst("return-date"));
        searchParameters.put("passengers", formData.getFirst("passengers"));

        return searchParameters;
    }

    @Override
    public String generateFlightSearchAPIToken(HashMap<String, String> parameters) {
        return String.format(
                "https://airlabs.co/api/v9/schedules?dep_iata=%s&dep_icao=%s&arr_iata=%s&arr_icao=%s&airline_icao=%s&airline_iata=%s&api_key=c7e5b46c-cab6-4b7c-985e-eef74ed6caf4" +
                        "&_fields=flight_iata,dep_iata,dep_time,arr_iata,arr_time,status,duration,aircraft_icao",
                parameters.get("departureAirportIataCode"),
                parameters.get("departureAirportIcaoCode"),
                parameters.get("arrivalAirportIataCode"),
                parameters.get("arrivalAirportIcaoCode"),
                parameters.get("airlineIcaoCode"),
                parameters.get("airlineIataCode"));
    }

    @Override
    public List<FlightDTO> getSearchedFlights(HashMap<String, String> parameters) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(generateFlightSearchAPIToken(parameters)))
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        JSONObject json = new JSONObject(response.body());
        JSONArray jsonArray = json.getJSONArray("response");

        List<FlightDTO> flights = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            FlightDTO flight = objectMapper.readValue(jsonObject.toString(), FlightDTO.class);

            int price = Integer.parseInt(flight.getDuration())  * 13;
            flight.setPrice(price);
            flight.setId((long) (i + 1));
            flight.setDuration(convertDurationToHours(Integer.parseInt(flight.getDuration())));
            flights.add(flight);
        }
        return flights;
    }

    @Override
    public String convertDurationToHours(int duration) {
        int hours = duration / 60;
        int minutes = (duration - (hours * 60)) / 5;
        return String.format("%sh %sm", hours, minutes*5);
    }


}
