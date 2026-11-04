package fit.biejk.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fit.biejk.entity.BidStatus;
import fit.biejk.entity.Category;
import fit.biejk.entity.Location;
import fit.biejk.entity.OrderStatus;
import fit.biejk.entity.UserRole;
import fit.biejk.exception.ErrorCode;
import fit.biejk.exception.ErrorResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JSON the API sends and reads. The expected texts are what the API answered with JSON-B before the move to
 * Jackson, so a client sees no change; the only difference is that Jackson also writes the fields that are
 * {@code null}, which are removed here before comparing (the error {@code details} is checked on its own).
 */
class JsonFormatTest {

    private static final UUID A = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final LocalDateTime T = LocalDateTime.of(2026, 11, 5, 10, 30, 15);

    private static final int PRICE = 100;

    /**
     * Same settings as Quarkus (see {@code quarkus.jackson.*} in application.properties).
     */
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonNode withoutNulls(final Object value) throws Exception {
        return strip(parse(value));
    }

    private JsonNode strip(final JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> fields = new LinkedHashMap<>();
            node.fields().forEachRemaining(e -> {
                if (!e.getValue().isNull()) {
                    fields.put(e.getKey(), strip(e.getValue()));
                }
            });
            return mapper.valueToTree(fields);
        }
        if (node.isArray()) {
            List<JsonNode> items = new java.util.ArrayList<>();
            node.forEach(i -> items.add(strip(i)));
            return mapper.valueToTree(items);
        }
        return node;
    }

    private JsonNode json(final String text) throws Exception {
        return mapper.readTree(text);
    }

    /**
     * The JSON as it is sent: written to a text and read again, so the numbers have the same node types as in the
     * expected text.
     */
    private JsonNode parse(final Object value) throws Exception {
        return mapper.readTree(mapper.writeValueAsString(value));
    }

    private static Category category() {
        Category c = new Category();
        c.setId(A);
        c.setVersion(3L);
        c.setName("Plumbing");
        return c;
    }

    private static Location location() {
        Location l = new Location();
        l.setId(B);
        l.setVersion(1L);
        l.setName("Berlin");
        return l;
    }

    @Test
    void orderResponse() throws Exception {
        OrderResponse o = new OrderResponse();
        o.setId(A);
        o.setVersion(2L);
        o.setCustomerId(B);
        o.setCategories(List.of(category()));
        o.setStatus(OrderStatus.CREATED);
        o.setTitle("Fix");
        o.setPrice(PRICE);
        o.setCreatedAt(T);
        o.setLocation(location());

        assertEquals(json("{\"categories\":[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Plumbing\"}],"
                + "\"createdAt\":\"2026-11-05T10:30:15\",\"customerId\":\"22222222-2222-2222-2222-222222222222\","
                + "\"id\":\"11111111-1111-1111-1111-111111111111\",\"location\":{\"id\":"
                + "\"22222222-2222-2222-2222-222222222222\",\"name\":\"Berlin\"},\"price\":100,\"status\":\"CREATED\","
                + "\"title\":\"Fix\",\"version\":2}"), withoutNulls(o));
    }

    @Test
    void theVersionOfANestedEntityIsNotShown() throws Exception {
        assertTrue(mapper.valueToTree(category()).has("name"));
        assertTrue(!mapper.valueToTree(category()).has("version"));
        assertTrue(!mapper.valueToTree(location()).has("version"));
        assertTrue(!mapper.valueToTree(location()).has("users"));
        assertTrue(!mapper.valueToTree(location()).has("orders"));
    }

    @Test
    void bidResponse() throws Exception {
        BidResponse b = new BidResponse();
        b.setId(A);
        b.setVersion(0L);
        b.setOrderId(B);
        b.setProfessionalId(A);
        b.setDescription("ok");
        b.setPrice(5);
        b.setStatus(BidStatus.CREATED);

        assertEquals(json("{\"description\":\"ok\",\"id\":\"11111111-1111-1111-1111-111111111111\","
                + "\"orderId\":\"22222222-2222-2222-2222-222222222222\",\"price\":5,"
                + "\"professionalId\":\"11111111-1111-1111-1111-111111111111\",\"status\":\"CREATED\","
                + "\"version\":0}"), withoutNulls(b));
    }

    @Test
    void customerResponse() throws Exception {
        CustomerResponse c = new CustomerResponse();
        c.setId(A);
        c.setVersion(4L);
        c.setEmail("a@b.io");
        c.setFirstName("Ann");
        c.setCreateAt(T);
        c.setLocation(location());

        assertEquals(json("{\"createAt\":\"2026-11-05T10:30:15\",\"email\":\"a@b.io\",\"firstName\":\"Ann\","
                + "\"id\":\"11111111-1111-1111-1111-111111111111\",\"location\":{\"id\":"
                + "\"22222222-2222-2222-2222-222222222222\",\"name\":\"Berlin\"},\"version\":4}"), withoutNulls(c));
    }

    @Test
    void professionalResponse() throws Exception {
        ProfessionalResponse p = new ProfessionalResponse();
        p.setId(A);
        p.setVersion(1L);
        p.setAverageRating(4.5);
        p.setEmail("p@b.io");
        p.setCategories(List.of(category()));
        p.setDescription("d");

        assertEquals(json("{\"averageRating\":4.5,\"categories\":[{\"id\":"
                + "\"11111111-1111-1111-1111-111111111111\",\"name\":\"Plumbing\"}],\"description\":\"d\","
                + "\"email\":\"p@b.io\",\"id\":\"11111111-1111-1111-1111-111111111111\",\"version\":1}"),
                withoutNulls(p));
    }

    @Test
    void reviewAndAuthResponse() throws Exception {
        ReviewResponse r = new ReviewResponse();
        r.setId(A);
        r.setComment("good");
        r.setRating(5);

        assertEquals(json("{\"comment\":\"good\",\"id\":\"11111111-1111-1111-1111-111111111111\",\"rating\":5}"),
                withoutNulls(r));
        assertEquals(json("{\"token\":\"tok\"}"), withoutNulls(new AuthResponse("tok")));
    }

    @Test
    void errorResponseKeepsTheNullDetails() throws Exception {
        assertEquals(json("{\"error\":{\"code\":\"NOT_FOUND\",\"details\":null,\"message\":\"Not found\"}}"),
                parse(ErrorResponse.of(ErrorCode.NOT_FOUND, "Not found", null)));
        assertEquals(json("{\"error\":{\"code\":\"VALIDATION_ERROR\",\"details\":{\"title\":\"must not be blank\"},"
                + "\"message\":\"Some fields are invalid\"}}"),
                parse(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Some fields are invalid",
                        Map.of("title", "must not be blank"))));
    }

    @Test
    void createRequestIsReadWithAnIsoDateAndIgnoresUnknownFields() throws Exception {
        OrderCreateRequest request = mapper.readValue("{\"title\":\"T\",\"description\":\"D\",\"price\":7,"
                + "\"deadline\":\"2026-12-01T10:00:00\",\"unknown\":1}", OrderCreateRequest.class);

        assertEquals("T", request.getTitle());
        assertEquals(7, request.getPrice());
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), request.getDeadline());
    }

    @Test
    void enumsAreReadByName() throws Exception {
        AuthRequest request = mapper.readValue("{\"email\":\"a@b.io\",\"password\":\"x\",\"role\":\"CUSTOMER\"}",
                AuthRequest.class);

        assertEquals(UserRole.CUSTOMER, request.getRole());
    }

    @Test
    void aFieldThatIsNotSentStaysNull() throws Exception {
        OrderUpdateRequest request = mapper.readValue("{\"version\":3,\"price\":9,\"status\":\"CANCELLED\"}",
                OrderUpdateRequest.class);

        assertEquals(3L, request.getVersion());
        assertEquals(9, request.getPrice());
        assertEquals(OrderStatus.CANCELLED, request.getStatus());
        assertNull(request.getTitle());
    }
}
