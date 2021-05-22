package com.github.ivpal.prefx;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasSize;

@Testcontainers
public class ApplicationTest {
    @Container
    private static final DockerComposeContainer<?> containers = new DockerComposeContainer<>(new File("docker-compose.test.yml"))
        .withExposedService("app", 8000)
        .waitingFor("app", Wait.defaultWaitStrategy());

    @Test
    public void completionsTest() {
        var spec = new RequestSpecBuilder()
            .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
            .setBaseUri("http://localhost:8000/")
            .build();

        given()
            .spec(spec)
            .get("/completions?prefix=Tw")
            .then()
            .assertThat()
            .statusCode(200)
            .body("$", hasSize(0));

        given()
            .spec(spec)
            .header("Content-Type", "application/json")
            .body(new JsonObject().put("query", "Twitter").put("completion", "Twitter").encode())
            .post("/completions")
            .then()
            .assertThat()
            .statusCode(200);

        var completions = given()
            .spec(spec)
            .get("/completions?prefix=Tw")
            .then()
            .assertThat()
            .statusCode(200)
            .extract()
            .as(String[].class);

        assertThat(completions, arrayWithSize(1));
        assertThat(completions, hasItemInArray("Twitter"));
    }
}
