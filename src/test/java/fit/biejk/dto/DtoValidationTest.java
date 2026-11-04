package fit.biejk.dto;

import fit.biejk.entity.BidStatus;
import fit.biejk.entity.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static final String PASSWORD = "Str0ng-password";

    private static final String TITLE = "Fix the fence";

    private static final String DESCRIPTION = "Replace three broken boards in the fence";

    private static ValidatorFactory factory;

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> invalidFields(final Object dto) {
        return validator.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static String repeat(final int length) {
        return "a".repeat(length);
    }

    private static OrderUpdateRequest orderUpdate() {
        OrderUpdateRequest request = new OrderUpdateRequest();
        request.setVersion(0L);
        return request;
    }

    private static ProfileUpdateRequest profileUpdate() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setVersion(0L);
        return request;
    }

    private static ProfessionalUpdateRequest professionalUpdate() {
        ProfessionalUpdateRequest request = new ProfessionalUpdateRequest();
        request.setVersion(0L);
        return request;
    }

    private static BidUpdateRequest responseUpdate() {
        BidUpdateRequest request = new BidUpdateRequest();
        request.setVersion(0L);
        return request;
    }

    private static OrderCreateRequest order(final String title, final String description) {
        OrderCreateRequest dto = new OrderCreateRequest();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setPrice(100);
        return dto;
    }

    @Test
    void authRequestAcceptsValidInput() {
        assertTrue(invalidFields(new AuthRequest("user@example.com", PASSWORD, UserRole.CUSTOMER)).isEmpty());
    }

    @Test
    void authRequestRequiresEmailPasswordAndRole() {
        assertEquals(Set.of("email", "password", "role"), invalidFields(new AuthRequest(null, null, null)));
    }

    @Test
    void emailIsLimitedTo254Characters() {
        String email = repeat(250) + "@a.io";

        assertEquals(Set.of("email"), invalidFields(new AuthRequest(email, PASSWORD, UserRole.CUSTOMER)));
    }

    @Test
    void passwordIsLimitedTo128Characters() {
        assertEquals(Set.of("password"),
                invalidFields(new AuthRequest("user@example.com", repeat(129), UserRole.CUSTOMER)));
    }

    @Test
    void passwordNeedsAtLeast8Characters() {
        assertEquals(Set.of("password"),
                invalidFields(new AuthRequest("user@example.com", "short", UserRole.CUSTOMER)));
    }

    @Test
    void orderDeadlineMustNotBeInThePast() {
        OrderCreateRequest dto = order(TITLE, DESCRIPTION);
        dto.setDeadline(LocalDateTime.now().minusDays(1));
        assertEquals(Set.of("deadline"), invalidFields(dto));

        dto.setDeadline(LocalDateTime.now().plusDays(1));
        assertTrue(invalidFields(dto).isEmpty());

        dto.setDeadline(null);
        assertTrue(invalidFields(dto).isEmpty());
    }

    @Test
    void partialOrderUpdateChecksTheDeadlineOnlyWhenItIsSent() {
        OrderUpdateRequest request = orderUpdate();
        request.setDeadline(LocalDateTime.now().minusHours(1));

        assertEquals(Set.of("deadline"), invalidFields(request));
    }

    @Test
    void finalDeadlineIsRequiredAndMustNotBeInThePast() {
        BidUpdateRequest request = responseUpdate();
        request.setStatus(BidStatus.APPROVED);
        assertEquals(Set.of("finalDeadline"), invalidFields(request));

        request.setFinalDeadline(LocalDateTime.now().minusDays(1));
        assertEquals(Set.of("finalDeadline"), invalidFields(request));

        request.setFinalDeadline(LocalDateTime.now().plusDays(1));
        assertTrue(invalidFields(request).isEmpty());
    }

    @Test
    void newPasswordNeedsAtLeast8Characters() {
        assertEquals(Set.of("newPassword"),
                invalidFields(new ChangePasswordRequest(PASSWORD, "short", "short")));
        assertTrue(invalidFields(new ChangePasswordRequest("old", PASSWORD, PASSWORD)).isEmpty());
    }

    @Test
    void resetPasswordChecksTheNewPasswordLength() {
        assertEquals(Set.of("newPassword"),
                invalidFields(new ResetPasswordRequest("user@example.com", "short", "short", "123456")));
    }

    @Test
    void orderRequiresTitleAndDescription() {
        assertEquals(Set.of("title", "description"), invalidFields(order(null, null)));
        assertTrue(invalidFields(order(TITLE, DESCRIPTION)).isEmpty());
    }

    @Test
    void orderTitleAndDescriptionHaveLengthLimits() {
        assertEquals(Set.of("title", "description"), invalidFields(order("abcd", "too short")));
        assertEquals(Set.of("title", "description"), invalidFields(order(repeat(121), repeat(5001))));
        assertTrue(invalidFields(order(repeat(120), repeat(5000))).isEmpty());
    }

    @Test
    void controlCharactersAreRejected() {
        assertEquals(Set.of("title"), invalidFields(order("Fix\u0000the fence", DESCRIPTION)));
        assertEquals(Set.of("title"), invalidFields(order("Fix\nthe fence", DESCRIPTION)));
    }

    @Test
    void lineBreaksAreAllowedInADescription() {
        assertTrue(invalidFields(order(TITLE, DESCRIPTION + "\nSecond line\twith a tab")).isEmpty());
        assertEquals(Set.of("description"), invalidFields(order(TITLE, DESCRIPTION + "\u0007")));
    }

    @Test
    void orderPriceHasACap() {
        OrderCreateRequest dto = order(TITLE, DESCRIPTION);
        dto.setPrice(1_000_001);

        assertEquals(Set.of("price"), invalidFields(dto));
    }

    @Test
    void everyPatchRequiresTheVersion() {
        assertEquals(Set.of("version"), invalidFields(new OrderUpdateRequest()));
        assertEquals(Set.of("version"), invalidFields(new ProfileUpdateRequest()));
        assertEquals(Set.of("version"), invalidFields(new ProfessionalUpdateRequest()));

        BidUpdateRequest bid = new BidUpdateRequest();
        bid.setStatus(BidStatus.APPROVED);
        bid.setFinalDeadline(LocalDateTime.now().plusDays(1));
        assertEquals(Set.of("version"), invalidFields(bid));
    }

    @Test
    void versionCannotBeNegative() {
        OrderUpdateRequest request = orderUpdate();
        request.setVersion(-1L);

        assertEquals(Set.of("version"), invalidFields(request));
    }

    @Test
    void partialOrderUpdateMayOmitEveryField() {
        assertTrue(invalidFields(orderUpdate()).isEmpty());
    }

    @Test
    void partialOrderUpdateRejectsABlankOrTooShortText() {
        OrderUpdateRequest request = orderUpdate();
        request.setTitle("      ");
        request.setDescription("too short");

        assertEquals(Set.of("title", "description"), invalidFields(request));
    }

    @Test
    void responseDescriptionIsRequiredAndLimited() {
        BidCreateRequest dto = new BidCreateRequest();
        dto.setPrice(100);

        assertEquals(Set.of("description"), invalidFields(dto));

        dto.setDescription(repeat(2001));
        assertEquals(Set.of("description"), invalidFields(dto));
    }

    @Test
    void responsePriceIsRequired() {
        BidCreateRequest dto = new BidCreateRequest();
        dto.setDescription("I can do it");

        assertEquals(Set.of("price"), invalidFields(dto));
    }

    @Test
    void orderPriceIsRequired() {
        OrderCreateRequest dto = order(TITLE, DESCRIPTION);
        dto.setPrice(null);

        assertEquals(Set.of("price"), invalidFields(dto));
    }

    @Test
    void responsePriceIsNonNegativeAndCapped() {
        BidCreateRequest dto = new BidCreateRequest();
        dto.setDescription("I can do it");
        dto.setPrice(-1);
        assertEquals(Set.of("price"), invalidFields(dto));

        dto.setPrice(1_000_001);
        assertEquals(Set.of("price"), invalidFields(dto));

        dto.setPrice(0);
        assertTrue(invalidFields(dto).isEmpty());
    }

    @Test
    void profileNamesAllowLettersOfAnyScript() {
        ProfileUpdateRequest request = profileUpdate();
        request.setFirstName("José");
        request.setLastName("О'Коннор-Смит");

        assertTrue(invalidFields(request).isEmpty());
    }

    @Test
    void profileNamesRejectMarkupAndLongValues() {
        ProfileUpdateRequest request = profileUpdate();
        request.setFirstName("<script>");
        request.setLastName(repeat(51));

        assertEquals(Set.of("firstName", "lastName"), invalidFields(request));
    }

    @Test
    void phoneAllowsDigitsPlusAndSeparatorsOnly() {
        ProfileUpdateRequest request = profileUpdate();
        request.setPhoneNumber("+7 (999) 123-45-67");
        assertTrue(invalidFields(request).isEmpty());

        request.setPhoneNumber("call me");
        assertEquals(Set.of("phoneNumber"), invalidFields(request));

        request.setPhoneNumber("+" + "1".repeat(20));
        assertEquals(Set.of("phoneNumber"), invalidFields(request));
    }

    @Test
    void professionalDescriptionIsLimited() {
        ProfessionalUpdateRequest request = professionalUpdate();
        request.setDescription(repeat(2001));

        assertEquals(Set.of("description"), invalidFields(request));
    }

    @Test
    void reviewCommentRejectsControlCharacters() {
        ReviewCreateRequest dto = new ReviewCreateRequest();
        dto.setComment("Great work\u0000!");
        dto.setRating(5);

        Set<ConstraintViolation<ReviewCreateRequest>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("comment", violations.iterator().next().getPropertyPath().toString());
    }
}
