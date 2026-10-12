package com.bharatexplore.service;

import com.bharatexplore.dto.DestinationDtos.*;
import com.bharatexplore.dto.PlannerDtos.PlanRequest;
import com.bharatexplore.dto.PlannerDtos.PlanResponse;
import com.bharatexplore.exception.ApiException;
import com.bharatexplore.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a personalised, day-wise itinerary. Gemini does the heavy lifting when an API key is configured;
 * otherwise (or if the AI call fails) a deterministic planner uses the destination data in MySQL.
 */
@Service
public class PlannerService {

    private static final Logger log = LoggerFactory.getLogger(PlannerService.class);

    static final Set<String> INTERESTS = Set.of("Heritage", "Nature", "Adventure", "Spiritual", "Food",
            "Beaches", "Shopping", "Nightlife", "Wildlife", "Relaxation");
    static final Set<String> STYLES = Set.of("Relaxed", "Balanced", "Packed", "Luxury", "Backpacker");

    private static final String[] PLACE_TIMES = {"09:00 AM", "11:30 AM", "03:00 PM", "05:30 PM"};

    private final DestinationService destinations;
    private final GeminiClient gemini;
    private final PlanService plans;
    private final AuthService auth;
    private final ObjectMapper mapper;

    public PlannerService(DestinationService destinations, GeminiClient gemini, PlanService plans,
                          AuthService auth, ObjectMapper mapper) {
        this.destinations = destinations;
        this.gemini = gemini;
        this.plans = plans;
        this.auth = auth;
        this.mapper = mapper;
    }

    public PlanResponse generate(String userEmail, PlanRequest req) {
        User user = auth.requireUser(userEmail);

        DestinationDetail dest = destinations.findByName(req.destination())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "We don't have \"" + req.destination().trim() + "\" yet. Available: "
                                + String.join(", ", destinations.allNames())));

        // Only whitelisted values ever reach the AI prompt, so free text from the form cannot inject instructions.
        List<String> interests = Optional.ofNullable(req.interests()).orElse(List.of()).stream()
                .filter(Objects::nonNull).filter(INTERESTS::contains).distinct().toList();
        String food = normaliseFood(req.foodPreference());
        String style = req.travelStyle() != null && STYLES.contains(req.travelStyle())
                ? req.travelStyle() : "Balanced";

        ObjectNode prefs = mapper.createObjectNode();
        prefs.put("destination", dest.name());
        prefs.put("days", req.days());
        prefs.put("budget", req.budget());
        prefs.set("interests", mapper.valueToTree(interests));
        prefs.put("foodPreference", food);
        prefs.put("travelStyle", style);

        JsonNode itinerary = null;
        String source = "rule-based";
        if (gemini.isEnabled()) {
            itinerary = tryGemini(dest, req.days(), req.budget(), interests, food, style);
            if (itinerary != null) source = "gemini";
        }
        if (itinerary == null) {
            itinerary = buildFallback(dest, req.days(), req.budget(), interests, food, style);
        }

        return plans.save(user, dest.name(), req.days(), source, prefs, itinerary);
    }

    // ------------------------------------------------------------------ Gemini

    private JsonNode tryGemini(DestinationDetail d, int days, int budget, List<String> interests,
                               String food, String style) {
        String prompt = buildPrompt(d, days, budget, interests, food, style);
        Optional<String> raw = gemini.generateJson(prompt);
        if (raw.isEmpty()) return null;
        try {
            JsonNode node = mapper.readTree(raw.get());
            if (node.isObject() && node.path("days").isArray() && node.path("days").size() > 0) {
                return node;
            }
            log.warn("Gemini returned JSON without a usable 'days' array; using fallback");
        } catch (Exception ex) {
            log.warn("Gemini returned invalid JSON; using fallback: {}", ex.getMessage());
        }
        return null;
    }

    private String buildPrompt(DestinationDetail d, int days, int budget, List<String> interests,
                               String food, String style) {
        String places = d.places().stream()
                .map(p -> "- " + p.name() + " [" + p.category() + ", " + p.visitDuration() + "]: " + p.description())
                .collect(Collectors.joining("\n"));
        String hotels = d.hotels().stream()
                .map(h -> "- " + h.name() + " [" + h.priceCategory() + ", approx INR " + h.approxPricePerNight() + "/night]")
                .collect(Collectors.joining("\n"));
        String restaurants = d.restaurants().stream()
                .map(r -> "- " + r.name() + " [" + r.cuisine() + ", " + r.foodType() + ", " + r.priceCategory() + "]: " + r.specialty())
                .collect(Collectors.joining("\n"));

        return """
                You are an expert India travel planner. Create a personalised %d-day itinerary for %s, %s.

                Traveller preferences:
                - Total budget for the whole trip (per person, INR): %d
                - Interests: %s
                - Food preference: %s
                - Travel style: %s

                Known data from our database. Prefer these places, hotels and restaurants, and you may add other
                well-known local options if needed:
                Tourist places:
                %s
                Hotels:
                %s
                Restaurants:
                %s

                Rules:
                - Respect the food preference strictly (VEG means no non-vegetarian suggestions).
                - Keep the plan realistic for the travel style; do not overpack days or ignore travel time.
                - Prices are indicative estimates in INR and may be outdated; say so in budgetNote.
                - Output ONLY valid JSON, no markdown, with exactly this shape:
                {
                  "title": string,
                  "summary": string,
                  "estimatedTotalCostINR": number,
                  "budgetNote": string,
                  "days": [
                    {
                      "day": number,
                      "title": string,
                      "activities": [ { "time": string, "title": string, "description": string } ],
                      "stay": string,
                      "meals": { "breakfast": string, "lunch": string, "dinner": string }
                    }
                  ],
                  "recommendedHotels": [ { "name": string, "reason": string, "approxPricePerNightINR": number } ],
                  "recommendedRestaurants": [ { "name": string, "reason": string } ],
                  "travelTips": [ string ]
                }
                The "days" array must contain exactly %d entries.
                """.formatted(days, d.name(), d.state(), budget,
                interests.isEmpty() ? "No specific interests (give a balanced mix)" : String.join(", ", interests),
                food, style, places, hotels, restaurants, days);
    }

    // ---------------------------------------------------------------- Fallback

    private JsonNode buildFallback(DestinationDetail d, int days, int budget, List<String> interests,
                                   String food, String style) {
        int perDayBudget = budget / days;
        String tier = perDayBudget < 3000 ? "BUDGET" : perDayBudget < 9000 ? "MID" : "LUXURY";
        if ("Luxury".equals(style)) tier = "LUXURY";
        if ("Backpacker".equals(style)) tier = "BUDGET";
        final String budgetTier = tier;

        HotelDto hotel = pickHotel(d.hotels(), budgetTier);
        List<RestaurantDto> eats = filterRestaurants(d.restaurants(), food);

        // Places the traveller cares about come first.
        List<PlaceDto> ordered = new ArrayList<>(d.places());
        ordered.sort(Comparator.comparingInt(p -> interests.contains(p.category()) ? 0 : 1));

        int perDay = switch (style) {
            case "Relaxed" -> 2;
            case "Packed" -> 4;
            default -> 3;
        };
        perDay = Math.max(1, Math.min(perDay, (int) Math.ceil(ordered.size() / (double) days)));

        ObjectNode root = mapper.createObjectNode();
        root.put("title", days + "-Day " + d.name() + " Trip");
        root.put("summary", "A " + style.toLowerCase() + " " + days + "-day plan for " + d.name() + ", " + d.state()
                + (interests.isEmpty() ? "." : ", focused on " + String.join(", ", interests).toLowerCase() + ".")
                + " Best time to visit: " + d.bestTimeToVisit() + ".");

        ArrayNode daysNode = root.putArray("days");
        int cursor = 0;
        int eatIndex = 0;
        for (int day = 1; day <= days; day++) {
            ObjectNode dayNode = daysNode.addObject();
            dayNode.put("day", day);
            ArrayNode acts = dayNode.putArray("activities");

            List<PlaceDto> todays = new ArrayList<>();
            for (int i = 0; i < perDay && cursor < ordered.size(); i++) todays.add(ordered.get(cursor++));

            String lunch = eats.isEmpty() ? "Local restaurant of your choice" : eats.get(eatIndex++ % eats.size()).name();
            String dinner = eats.isEmpty() ? "Local restaurant of your choice" : eats.get(eatIndex++ % eats.size()).name();

            if (todays.isEmpty()) {
                dayNode.put("title", "Leisure day in " + d.name());
                addActivity(acts, "10:00 AM", "Explore local markets and cafes",
                        "Free day to revisit a favourite spot, shop, or simply wander through the neighbourhoods.");
            } else {
                dayNode.put("title", todays.get(0).name() + (todays.size() > 1 ? " and more" : ""));
                for (int i = 0; i < todays.size(); i++) {
                    PlaceDto p = todays.get(i);
                    addActivity(acts, PLACE_TIMES[i], p.name(), p.description() + " Allow about " + p.visitDuration() + ".");
                    if (i == 1 || (todays.size() == 1)) {
                        // lunch break sits between the morning and afternoon sights
                        addActivity(acts, "01:00 PM", "Lunch at " + lunch, "Try the local specialities.");
                    }
                }
            }
            addActivity(acts, "07:30 PM", "Dinner at " + dinner, "Wind down the day with a good meal.");

            dayNode.put("stay", hotel == null ? "Choose a stay that fits your budget" : hotel.name());
            ObjectNode meals = dayNode.putObject("meals");
            meals.put("breakfast", "At your hotel or a nearby cafe");
            meals.put("lunch", lunch);
            meals.put("dinner", dinner);
        }

        ArrayNode hotelsNode = root.putArray("recommendedHotels");
        d.hotels().stream().sorted(Comparator.comparingInt(h -> tierDistance(h.priceCategory(), budgetTier)))
                .limit(3).forEach(h -> {
                    ObjectNode n = hotelsNode.addObject();
                    n.put("name", h.name());
                    n.put("reason", h.description());
                    if (h.approxPricePerNight() != null) n.put("approxPricePerNightINR", h.approxPricePerNight());
                });

        ArrayNode eatsNode = root.putArray("recommendedRestaurants");
        eats.stream().limit(4).forEach(r -> {
            ObjectNode n = eatsNode.addObject();
            n.put("name", r.name());
            n.put("reason", r.specialty() == null ? r.cuisine() : r.cuisine() + ": " + r.specialty());
        });

        int nights = Math.max(0, days - 1);
        int stayCost = hotel == null || hotel.approxPricePerNight() == null ? 0 : nights * hotel.approxPricePerNight();
        int foodPerDay = switch (budgetTier) { case "BUDGET" -> 600; case "MID" -> 1500; default -> 3500; };
        int transportPerDay = switch (budgetTier) { case "BUDGET" -> 400; case "MID" -> 900; default -> 2500; };
        int total = stayCost + days * (foodPerDay + transportPerDay);
        root.put("estimatedTotalCostINR", total);
        root.put("budgetNote", "Rough per-person estimate (single occupancy) covering stay, food and local transport. "
                + "Excludes flights/trains and entry tickets. Prices are indicative and may be outdated. "
                + (total <= budget ? "This should fit your budget."
                : "This may exceed your budget; consider a more budget-friendly stay or fewer days."));

        ArrayNode tips = root.putArray("travelTips");
        tips.add("Best time to visit: " + d.bestTimeToVisit() + ".");
        tips.add("Book popular hotels and train tickets early, especially in peak season.");
        tips.add("Carry water, sunscreen and some cash for small vendors.");
        tips.add("This plan was generated without AI (rule-based mode). Add a Gemini API key on the server for richer itineraries.");
        return root;
    }

    private void addActivity(ArrayNode acts, String time, String title, String description) {
        ObjectNode a = acts.addObject();
        a.put("time", time);
        a.put("title", title);
        a.put("description", description);
    }

    private HotelDto pickHotel(List<HotelDto> hotels, String tier) {
        return hotels.stream().min(Comparator.comparingInt(h -> tierDistance(h.priceCategory(), tier))).orElse(null);
    }

    private int tierDistance(String a, String b) {
        List<String> order = List.of("BUDGET", "MID", "LUXURY");
        return Math.abs(order.indexOf(a) - order.indexOf(b));
    }

    private List<RestaurantDto> filterRestaurants(List<RestaurantDto> all, String food) {
        return all.stream().filter(r -> switch (food) {
            case "VEG" -> r.foodType().equals("VEG") || r.foodType().equals("BOTH");
            case "NON_VEG" -> r.foodType().equals("NON_VEG") || r.foodType().equals("BOTH");
            default -> true;
        }).toList();
    }

    private String normaliseFood(String raw) {
        if (raw == null) return "ANY";
        String v = raw.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (v) {
            case "VEG", "VEGETARIAN" -> "VEG";
            case "NON_VEG", "NONVEG", "NON_VEGETARIAN" -> "NON_VEG";
            default -> "ANY";
        };
    }
}
