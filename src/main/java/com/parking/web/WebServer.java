package com.parking.web;

import com.parking.model.*;
import com.parking.service.ParkingLotService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Embedded HTTP Web Server providing REST APIs and static Web UI hosting.
 * Zero external dependencies (uses standard Java com.sun.net.httpserver).
 */
public class WebServer {
    private final int port;
    private final ParkingLotService parkingService;
    private HttpServer server;

    public WebServer(int port, ParkingLotService parkingService) {
        this.port = port;
        this.parkingService = parkingService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // REST API endpoints
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/park", new ParkHandler());
        server.createContext("/api/unpark", new UnparkHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/reset", new ResetHandler());

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // Default executor
        server.start();
        System.out.println("🌐 Web Dashboard live at: http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // --- Handlers ---

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsOptions(exchange);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            List<ParkingSlot> slots = parkingService.getAllSlots();
            List<Ticket> activeTickets = parkingService.getActiveTickets();
            List<Ticket> allTickets = parkingService.getAllTickets();

            double totalRevenue = allTickets.stream()
                    .filter(Ticket::isPaid)
                    .mapToDouble(Ticket::getFee)
                    .sum();

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"totalRevenue\":").append(String.format("%.2f", totalRevenue)).append(",");
            json.append("\"slots\":[").append(slotsToJson(slots)).append("],");
            json.append("\"activeTickets\":[").append(ticketsToJson(activeTickets)).append("]");
            json.append("}");

            sendJsonResponse(exchange, 200, json.toString());
        }
    }

    private class ParkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsOptions(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String licensePlate = extractJsonValue(body, "licensePlate");
            String vehicleTypeStr = extractJsonValue(body, "vehicleType");

            if (licensePlate == null || vehicleTypeStr == null) {
                sendError(exchange, 400, "Missing required parameters: licensePlate or vehicleType");
                return;
            }

            try {
                VehicleType type = VehicleType.valueOf(vehicleTypeStr.toUpperCase());
                Vehicle vehicle = switch (type) {
                    case BIKE -> new Bike(licensePlate);
                    case CAR -> new Car(licensePlate);
                    case TRUCK -> new Truck(licensePlate);
                };

                Ticket ticket = parkingService.parkVehicle(vehicle);
                sendJsonResponse(exchange, 200, ticketToJson(ticket));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class UnparkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsOptions(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String ticketId = extractJsonValue(body, "ticketId");
            String hoursStr = extractJsonValue(body, "hours");

            if (ticketId == null) {
                sendError(exchange, 400, "Missing required parameter: ticketId");
                return;
            }

            try {
                long hours = (hoursStr != null) ? Long.parseLong(hoursStr) : 1;
                Ticket ticketOpt = parkingService.getAllTickets().stream()
                        .filter(t -> t.getTicketId().equalsIgnoreCase(ticketId) || t.getLicensePlate().equalsIgnoreCase(ticketId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

                LocalDateTime exitTime = ticketOpt.getEntryTime().plusHours(hours);
                Ticket completed = parkingService.unparkVehicle(ticketOpt.getTicketId(), exitTime);

                sendJsonResponse(exchange, 200, ticketToJson(completed));
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsOptions(exchange);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            List<Ticket> tickets = parkingService.getAllTickets();
            sendJsonResponse(exchange, 200, "[" + ticketsToJson(tickets) + "]");
        }
    }

    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendCorsOptions(exchange);
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                parkingService.setupParkingLot(5, 10, 5);
                sendJsonResponse(exchange, 200, "{\"message\":\"Parking lot reset successfully\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String pathStr = exchange.getRequestURI().getPath();
            if ("/".equals(pathStr)) {
                pathStr = "/index.html";
            }

            Path filePath = Paths.get("web", pathStr.substring(1));
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendError(exchange, 404, "File Not Found");
                return;
            }

            String contentType = switch (getFileExtension(filePath.toString())) {
                case "html" -> "text/html";
                case "css" -> "text/css";
                case "js" -> "application/javascript";
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                default -> "application/octet-stream";
            };

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    // --- Utility Methods ---

    private void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendCorsOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJsonResponse(exchange, status, "{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private String slotsToJson(List<ParkingSlot> slots) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            ParkingSlot s = slots.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(s.getSlotId()).append("\",");
            sb.append("\"type\":\"").append(s.getSlotType()).append("\",");
            sb.append("\"occupied\":").append(s.isOccupied());
            if (s.isOccupied() && s.getParkedVehicle() != null) {
                sb.append(",\"vehicle\":{");
                sb.append("\"licensePlate\":\"").append(s.getParkedVehicle().getLicensePlate()).append("\",");
                sb.append("\"vehicleType\":\"").append(s.getParkedVehicle().getType()).append("\"");
                sb.append("}");
            }
            sb.append("}");
            if (i < slots.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private String ticketsToJson(List<Ticket> tickets) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tickets.size(); i++) {
            sb.append(ticketToJson(tickets.get(i)));
            if (i < tickets.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private String ticketToJson(Ticket t) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"ticketId\":\"").append(t.getTicketId()).append("\",");
        sb.append("\"licensePlate\":\"").append(t.getLicensePlate()).append("\",");
        sb.append("\"vehicleType\":\"").append(t.getVehicleType()).append("\",");
        sb.append("\"slotId\":\"").append(t.getSlotId()).append("\",");
        sb.append("\"entryTime\":\"").append(t.getEntryTime().format(fmt)).append("\",");
        sb.append("\"exitTime\":").append(t.getExitTime() != null ? "\"" + t.getExitTime().format(fmt) + "\"" : "null").append(",");
        sb.append("\"fee\":").append(String.format("%.2f", t.getFee())).append(",");
        sb.append("\"paid\":").append(t.isPaid());
        sb.append("}");
        return sb.toString();
    }

    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) return null;
        String searchKey = "\"" + key + "\":";
        int idx = json.indexOf(searchKey);
        if (idx == -1) return null;

        int start = idx + searchKey.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private String getFileExtension(String name) {
        int last = name.lastIndexOf('.');
        return last == -1 ? "" : name.substring(last + 1).toLowerCase();
    }
}
