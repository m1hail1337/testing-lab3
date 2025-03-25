package org.itmo.testing.lab2.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.itmo.testing.lab2.controller.UserAnalyticsController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAnalyticsIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final LocalDateTime DAY_BEFORE_YESTERDAY = LocalDateTime.now().minusDays(2);
    private Javalin app;
    private final int port = 7000;

    @BeforeAll
    void setUp() {
        app = UserAnalyticsController.createApp();
        app.start(port);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    void tearDown() {
        app.stop();
    }

    @Order(1)
    @DisplayName("Тест регистрации пользователя")
    @ParameterizedTest
    @MethodSource("provideRegistrationData")
    void testUserRegistration(Map<String, String> queryParams, int status, String body) {
        given()
                .queryParams(queryParams)
                .when()
                .post("/register")
                .then()
                .statusCode(status)
                .body(equalTo(body));
    }

    private Stream<Arguments> provideRegistrationData() {
        return Stream.of(
            Arguments.of(
                Map.of("userId", "user1", "userName", "Alice"),
                HttpStatus.SC_OK,
                "User registered: true"
            ),
            Arguments.of(
                Map.of("userName", "Alice"),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            ),
            Arguments.of(
                Map.of("userId", "user1"),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            )
            // Arguments.of(
            //     Map.of("userId", "user1", "userName", "Alice"),
            //     HttpStatus.SC_BAD_REQUEST,
            //     "User already registered"
            // )
        );
    }

    @Order(2)
    @DisplayName("Тест записи сессии")
    @ParameterizedTest
    @MethodSource("provideSessionData")
    void testRecordSession(Map<String, String> queryParams, int status, String body) {
        given()
                .queryParams(queryParams)
                .when()
                .post("/recordSession")
                .then()
                .statusCode(status)
                .body(equalTo(body));
    }

    private Stream<Arguments> provideSessionData() {
        return Stream.of(
            Arguments.of(
                Map.of("userId", "user1", "loginTime", DAY_BEFORE_YESTERDAY.minusDays(3).toString(), "logoutTime", DAY_BEFORE_YESTERDAY.toString()),
                HttpStatus.SC_OK,
                "Session recorded"
            ),
            Arguments.of(
                Map.of("loginTime", DAY_BEFORE_YESTERDAY.minusDays(3).toString(), "logoutTime", DAY_BEFORE_YESTERDAY.toString()),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            ),
            Arguments.of(
                Map.of("userId", "user1", "logoutTime", DAY_BEFORE_YESTERDAY.toString()),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            ),
            Arguments.of(
                Map.of("userId", "user1", "loginTime", DAY_BEFORE_YESTERDAY.minusDays(3).toString()),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            ),
            Arguments.of(
                Map.of("userId", "user123123123", "loginTime", DAY_BEFORE_YESTERDAY.minusDays(3).toString(), "logoutTime", DAY_BEFORE_YESTERDAY.toString()),
                HttpStatus.SC_BAD_REQUEST,
                "Invalid data: User not found"
            )
        );
    }

    @Test
    @Order(3)
    @DisplayName("Тест получения общего времени активности")
    void testGetTotalActivity() {
        given()
                .queryParam("userId", "user1")
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("Total activity:"))
                .body(containsString("minutes"));
    }

    @Disabled
    @Test
    @Order(4)
    @DisplayName("Тест получения для несуществующего юзера")
    void testGetTotalActivityForNotExistsUser() {
        given()
            .queryParam("userId", "userNotExists")
            .when()
            .get("/totalActivity")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .body(equalTo("No sessions found for user"));
    }

    @Disabled
    @Test
    @Order(5)
    @DisplayName("Тест получения для юзера у которого нет сессий")
    void testGetTotalActivityForUserWithoutSessions() {
        given()
            .queryParam("userId", "userWithoutSessions")
            .queryParam("userName", "Bob")
            .when()
            .post("/register");
        given()
            .queryParam("userId", "userWithoutSessions")
            .when()
            .get("/totalActivity")
            .then()
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .body(containsString("No sessions found for user"));
    }

    @Order(6)
    @DisplayName("Тест получения неактивных пользователей")
    @ParameterizedTest
    @MethodSource("provideInactiveUsersData")
    void testGetInactiveUsers(Map<String, String> queryParams, int status, String body) {
        given()
            .queryParams(queryParams)
            .when()
            .get("/inactiveUsers")
            .then()
            .statusCode(status)
            .contentType(status == 200 ? ContentType.JSON : ContentType.TEXT)
            .body(equalTo(body));
    }

    private Stream<Arguments> provideInactiveUsersData() throws JsonProcessingException {
        return Stream.of(
            Arguments.of(Map.of("days", "1"), HttpStatus.SC_OK, MAPPER.writeValueAsString(List.of("user1"))),
            Arguments.of(Map.of("days", "2"), HttpStatus.SC_OK, MAPPER.writeValueAsString(List.of())),
            Arguments.of(Map.of(), HttpStatus.SC_BAD_REQUEST, "Missing days parameter"),
            Arguments.of(Map.of("days", "abc"), HttpStatus.SC_BAD_REQUEST, "Invalid number format for days")
        );
    }

    @Order(7)
    @DisplayName("Тест получения месячной активности")
    @ParameterizedTest
    @MethodSource("provideMonthlyActivity")
    void testGetMonthlyActivity(Map<String, String> queryParams, int status, String body) {
        given()
            .queryParams(queryParams)
            .when()
            .get("/monthlyActivity")
            .then()
            .statusCode(status)
            .contentType(status == 200 ? ContentType.JSON : ContentType.TEXT)
            .body(equalTo(body));
    }

    private Stream<Arguments> provideMonthlyActivity() throws JsonProcessingException {
        return Stream.of(
            Arguments.of(
                Map.of("userId", "user1", "month", YearMonth.from(DAY_BEFORE_YESTERDAY).toString()),
                HttpStatus.SC_OK,
                MAPPER.writeValueAsString(Map.of(DAY_BEFORE_YESTERDAY.minusDays(3).toLocalDate().toString(), 4320))
            ),
            Arguments.of(
                Map.of("userId", "user1", "month", YearMonth.from(DAY_BEFORE_YESTERDAY.minusMonths(1)).toString()),
                HttpStatus.SC_OK,
                MAPPER.writeValueAsString(Map.of())
            ),
            Arguments.of(
                Map.of("month", YearMonth.from(DAY_BEFORE_YESTERDAY).toString()),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            ),
            Arguments.of(
                Map.of("userId", "user1"),
                HttpStatus.SC_BAD_REQUEST,
                "Missing parameters"
            ),
            Arguments.of(
                Map.of("userId", "user1", "month", DAY_BEFORE_YESTERDAY.toString()),
                HttpStatus.SC_BAD_REQUEST,
                "Invalid data: Text '" + DAY_BEFORE_YESTERDAY + "' could not be parsed, unparsed text found at index 7"
            ),
            Arguments.of(
                Map.of("userId", "user1", "month", "abc"),
                HttpStatus.SC_BAD_REQUEST,
                "Invalid data: Text 'abc' could not be parsed at index 0"
            )
        );
    }
}
