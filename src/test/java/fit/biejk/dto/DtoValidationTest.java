package fit.biejk.dto;

import fit.biejk.entity.BidStatus;
import fit.biejk.entity.UserRole;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static final String PASSWORD = "Str0ng-password";

    private static final String TITLE = "Fix the fence";

    private static final String DESCRIPTION = "Replace three broken boards in the fence";

    private static final int PRICE = 100;

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

    private static OrderCreateRequest order(final String title, final String description) {
        return new OrderCreateRequest(null, title, description, PRICE, null);
    }

    private static OrderCreateRequest orderWith(final Integer price, final LocalDateTime deadline) {
        return new OrderCreateRequest(null, TITLE, DESCRIPTION, price, deadline);
    }

    private static OrderUpdateRequest orderUpdate(final String title, final String description, final Long version) {
        return new OrderUpdateRequest(null, null, description, title, null, null, null, version);
    }

    private static OrderUpdateRequest orderUpdate(final LocalDateTime deadline) {
        return new OrderUpdateRequest(null, null, null, null, null, deadline, null, 0L);
    }

    /**
     * A field of a partial update: {@code null} is a field that is not sent, otherwise the text that is sent.
     */
    private static Optional<String> sent(final String text) {
        return text == null ? null : Optional.of(text);
    }

    private static CustomerUpdateRequest customer(final String firstName, final String lastName, final String phone) {
        return new CustomerUpdateRequest(sent(firstName), sent(lastName), sent(phone), null, 0L);
    }

    private static ProfessionalUpdateRequest professional(final String description) {
        return new ProfessionalUpdateRequest(null, null, null, null, sent(description), null, 0L);
    }

    private static BidUpdateRequest bidUpdate(final LocalDateTime finalDeadline, final Long version) {
        return new BidUpdateRequest(BidStatus.APPROVED, finalDeadline, PRICE, version);
    }

    private static BidCreateRequest bid(final String description, final Integer price) {
        return new BidCreateRequest(description, price);
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
        assertEquals(Set.of("deadline"), invalidFields(orderWith(PRICE, LocalDateTime.now().minusDays(1))));
        assertTrue(invalidFields(orderWith(PRICE, LocalDateTime.now().plusDays(1))).isEmpty());
        assertTrue(invalidFields(orderWith(PRICE, null)).isEmpty());
    }

    @Test
    void partialOrderUpdateChecksTheDeadlineOnlyWhenItIsSent() {
        assertEquals(Set.of("deadline"), invalidFields(orderUpdate(LocalDateTime.now().minusHours(1))));
    }

    @Test
    void finalDeadlineIsRequiredAndMustNotBeInThePast() {
        assertEquals(Set.of("finalDeadline"), invalidFields(bidUpdate(null, 0L)));
        assertEquals(Set.of("finalDeadline"), invalidFields(bidUpdate(LocalDateTime.now().minusDays(1), 0L)));
        assertTrue(invalidFields(bidUpdate(LocalDateTime.now().plusDays(1), 0L)).isEmpty());
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
        assertEquals(Set.of("price"), invalidFields(orderWith(1_000_001, null)));
    }

    @Test
    void everyPatchRequiresTheVersion() {
        assertEquals(Set.of("version"), invalidFields(orderUpdate(null, null, null)));
        assertEquals(Set.of("version"), invalidFields(new CustomerUpdateRequest(null, null, null, null, null)));
        assertEquals(Set.of("version"),
                invalidFields(new ProfessionalUpdateRequest(null, null, null, null, null, null, null)));
        assertEquals(Set.of("version"), invalidFields(bidUpdate(LocalDateTime.now().plusDays(1), null)));
    }

    @Test
    void versionCannotBeNegative() {
        assertEquals(Set.of("version"), invalidFields(orderUpdate(null, null, -1L)));
    }

    @Test
    void partialOrderUpdateMayOmitEveryField() {
        assertTrue(invalidFields(orderUpdate(null, null, 0L)).isEmpty());
    }

    @Test
    void partialOrderUpdateRejectsATooShortText() {
        assertEquals(Set.of("title", "description"), invalidFields(orderUpdate("abc", "too short", 0L)));
    }

    @Test
    void bidDescriptionIsRequiredAndLimited() {
        assertEquals(Set.of("description"), invalidFields(bid(null, PRICE)));
        assertEquals(Set.of("description"), invalidFields(bid(repeat(2001), PRICE)));
    }

    @Test
    void bidPriceIsRequired() {
        assertEquals(Set.of("price"), invalidFields(bid("I can do it", null)));
    }

    @Test
    void orderPriceIsRequired() {
        assertEquals(Set.of("price"), invalidFields(orderWith(null, null)));
    }

    @Test
    void bidPriceIsNonNegativeAndCapped() {
        assertEquals(Set.of("price"), invalidFields(bid("I can do it", -1)));
        assertEquals(Set.of("price"), invalidFields(bid("I can do it", 1_000_001)));
        assertTrue(invalidFields(bid("I can do it", 0)).isEmpty());
    }

    @Test
    void profileNamesAllowLettersOfAnyScript() {
        assertTrue(invalidFields(customer("José", "О'Коннор-Смит", null)).isEmpty());
    }

    @Test
    void profileNamesRejectMarkupAndLongValues() {
        assertEquals(Set.of("firstName", "lastName"), invalidFields(customer("<script>", repeat(51), null)));
    }

    @Test
    void phoneAllowsDigitsPlusAndSeparatorsOnly() {
        assertTrue(invalidFields(customer(null, null, "+7 (999) 123-45-67")).isEmpty());
        assertEquals(Set.of("phoneNumber"), invalidFields(customer(null, null, "call me")));
        assertEquals(Set.of("phoneNumber"), invalidFields(customer(null, null, "+" + "1".repeat(20))));
    }

    @Test
    void anEmptyTextThatClearsAFieldIsValid() {
        assertTrue(invalidFields(customer("", "", "")).isEmpty());
        assertTrue(invalidFields(professional("")).isEmpty());
    }

    @Test
    void professionalDescriptionIsLimited() {
        assertEquals(Set.of("description"), invalidFields(professional(repeat(2001))));
    }

    @Test
    void reviewCommentRejectsControlCharacters() {
        assertEquals(Set.of("comment"), invalidFields(new ReviewCreateRequest("Great work\u0000!", 5)));
    }

    @Test
    void textIsTrimmedAndEmptyTextIsNoText() {
        OrderUpdateRequest blank = orderUpdate("   ", "", 0L);
        OrderCreateRequest padded = order("  " + TITLE + " \n", DESCRIPTION);

        assertNull(blank.title());
        assertNull(blank.description());
        assertEquals(TITLE, padded.title());
    }

    @Test
    void aTextFieldThatCanBeClearedIsEmptyWhenCleared() {
        CustomerUpdateRequest cleared = customer("   ", "", null);

        assertEquals(Optional.empty(), cleared.firstName());
        assertEquals(Optional.empty(), cleared.lastName());
        assertNull(cleared.phoneNumber());
        assertEquals(Optional.of("Ann"), customer(" Ann ", null, null).firstName());
    }

    @Test
    void passwordsAndCodesAreNotTrimmed() {
        ResetPasswordRequest request = new ResetPasswordRequest(" a@b.io ", " pass word ", " pass word ", " 123 ");

        assertEquals("a@b.io", request.email());
        assertEquals(" pass word ", request.newPassword());
        assertEquals(" 123 ", request.verificationCode());
    }
}
