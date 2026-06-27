package com.example.url_shortener.service;

import java.util.HashSet;
import java.util.Set;

public final class ReservedAliases {
    private static final  Set<String> RESERVED_ALIASES = Set.of(
            "api", "admin", "login", "logout", "register",
            "health", "actuator", "metrics", "static", "assets",
            "favicon", "robots", "about", "help", "settings"
    );

    private ReservedAliases() {}

    public static boolean isReserved(String alias) {
        return alias != null && RESERVED_ALIASES.contains(alias.toLowerCase());
    }

}
