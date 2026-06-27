package com.example.url_shortener.service;


import java.util.Random;

// Encodes a number into a Short base62 string(0-9,1-z,A-Z)
// feed it a unique value from a DB sequence, so every code come out unique.

// it's a utility no other class can extend it
public final class Base62 {
    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length(); // 62

    // private constructor prevents anyone from creating a base62 object(new Base62()) from outside
    // cuz every method is static , call Base62().encode(..) directly on the class , need no instance
    private Base62() {
        //The private constructor enforces "this is a utility class, don't instantiate it."

    }

    public static String encode(long value) {
        if (value==0){
            return "0";
        }
        StringBuilder sb=new StringBuilder();
        while (value>0){
            int remainder = (int) (value%BASE);
            sb.append(ALPHABET.charAt(remainder));
            value/=BASE;
        }
        return sb.reverse().toString();
    }


}
