package org.itmo.testing.lab2.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserStatusServiceTest {

    private UserAnalyticsService userAnalyticsService;
    private UserStatusService userStatusService;

    // неверно работало verify
    @BeforeEach
    void setUp() {
        userAnalyticsService = mock(UserAnalyticsService.class);
        userStatusService = new UserStatusService(userAnalyticsService);
    }

    @ParameterizedTest
    @MethodSource("provideUserActivityAndStatus")
    public void testGetUserStatus(long activityTime, String expectedStatus) {
        // Настроим поведение mock-объекта
        when(userAnalyticsService.getTotalActivityTime("user123")).thenReturn(activityTime);

        String status = userStatusService.getUserStatus("user123");
        verify(userAnalyticsService, times(1)).getTotalActivityTime("user123");

        assertEquals(expectedStatus, status);
    }

    private Stream<Arguments> provideUserActivityAndStatus() {
        return Stream.of(
            Arguments.of(119L, "Active"),
            Arguments.of(120L, "Highly active"),
            Arguments.of(121L, "Highly active"),
            Arguments.of(59L, "Inactive"),
            Arguments.of(60L, "Active"),
            Arguments.of(61L, "Active"),
            Arguments.of(0L, "Inactive"),
            Arguments.of(999L, "Highly active")
        );
    }

    @ParameterizedTest
    @MethodSource("provideSessions")
    public void testGetUserLastSessionDate(LocalDateTime logout, List<UserAnalyticsService.Session> sessions) {
        when(userAnalyticsService.getUserSessions("user123")).thenReturn(sessions);

        Optional<String> date = userStatusService.getUserLastSessionDate("user123");
        verify(userAnalyticsService, times(1)).getUserSessions("user123");

        assertTrue(date.isPresent());
        assertEquals(logout.toLocalDate().toString(), date.get());
    }

    private Stream<Arguments> provideSessions() {
        return Stream.of(
            Arguments.of(
                LocalDateTime.now().minusDays(3),
                List.of(new UserAnalyticsService.Session(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(3)))
            ),
            Arguments.of(
                LocalDateTime.now().minusDays(3),
                List.of(
                    new UserAnalyticsService.Session(LocalDateTime.now().minusDays(14), LocalDateTime.now().minusDays(13)),
                    new UserAnalyticsService.Session(LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(5)),
                    new UserAnalyticsService.Session(LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(3))
                )
            )
        );
    }

    @Disabled
    @ParameterizedTest
    @EmptySource
    public void testGetUserLastSessionDate_WithoutSessions(List<UserAnalyticsService.Session> sessions) {
        when(userAnalyticsService.getUserSessions("user123")).thenReturn(sessions);

        Optional<String> date = userStatusService.getUserLastSessionDate("user123");
        verify(userAnalyticsService, times(1)).getUserSessions("user123");

        assertTrue(date.isEmpty());
    }
}
