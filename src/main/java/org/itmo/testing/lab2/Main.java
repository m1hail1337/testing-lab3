package org.itmo.testing.lab2;

import org.itmo.testing.lab2.controller.UserAnalyticsController;

public class Main {
    private static final int PORT = 8080;
    public static void main(String[] args) {
        UserAnalyticsController.createApp().start(PORT);
    }
}
