package io.github.strogolsky.profinder.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * The reference data is public: the list of locations and of categories needs no token.
 */
@QuarkusTest
class ReferenceDataResourceTest extends ApiTestBase {

    @Test
    void locationsAreReadWithoutAToken() {
        given().get(API + "/locations").then()
                .statusCode(200)
                .body("name", hasItems("Prague", "Brno"))
                .body("[0].id", notNullValue());
    }

    @Test
    void categoriesAreReadWithoutAToken() {
        given().get(API + "/categories").then()
                .statusCode(200)
                .body("$", hasSize(20))
                .body("name", hasItems("Haircut", "Fitness Coaching"));
    }

    @Test
    void locationsDoNotExposeTheirOrders() {
        given().get(API + "/locations").then().statusCode(200).body("[0].orders", nullValue());
    }
}
