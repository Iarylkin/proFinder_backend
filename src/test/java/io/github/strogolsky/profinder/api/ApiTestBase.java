package io.github.strogolsky.profinder.api;

/**
 * Base of the tests of the REST resources: the whole application runs on real databases and the requests
 * go over HTTP. Quarkus resets the RestAssured base path before every test and does not add
 * {@code quarkus.rest.path}, so a test builds its paths from {@link #API}.
 */
public abstract class ApiTestBase {

    /** The root path of the REST API: {@code quarkus.rest.path} plus the version. */
    protected static final String API = "/api/v1";
}
