package com.example.url_shortener.security;

import com.example.url_shortener.domain.Client;

//Holds the authenticated client for the duration of one request thread.

public final class CurrentClient {
    private static final ThreadLocal<Client> CURRENT_CLIENT = new ThreadLocal<>();
    private CurrentClient() {

    }

    public static void set(Client client) {
        CURRENT_CLIENT.set(client);
    }

    public static Client get() {
        return CURRENT_CLIENT.get();
    }

    public static void clear() {
        CURRENT_CLIENT.remove();
    }
}
