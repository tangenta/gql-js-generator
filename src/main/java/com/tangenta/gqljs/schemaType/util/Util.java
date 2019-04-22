package com.tangenta.gqljs.schemaType.util;

public class Util {
    public static String strip(String type) {
        int low = 0;
        int up = type.length();
        boolean stripping = true;
        while (stripping) {
            stripping = false;
            if (type.charAt(up - 1) == '!') {
                up--;
                stripping = true;
            }
            if (type.charAt(low) == '[' && type.charAt(up - 1) == ']') {
                low++;
                up--;
                stripping = true;
            }
        }
        return type.substring(low, up);
    }

    public static void main(String[] args) {
        System.out.println(strip("[[Xtx!]]!"));
        System.out.println(strip("HostResult!"));
    }

}
