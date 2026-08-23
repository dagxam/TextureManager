package me.dagxam.texturemanager;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Определяет версию сервера и безопасный формат ресурс-пака.
 */
public final class PackFormatResolver {

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private PackFormatResolver() {
    }

    public static int resolvePackFormat() {
        String version = Bukkit.getMinecraftVersion();
        Matcher matcher = VERSION.matcher(version);
        if (!matcher.find()) {
            return 15;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));

        if (major == 1 && minor >= 21 && patch >= 9) return 75;
        if (major == 1 && minor >= 21 && patch >= 6) return 63;
        if (major == 1 && minor >= 21 && patch >= 5) return 55;
        if (major == 1 && minor >= 21 && patch >= 4) return 46;
        if (major == 1 && minor >= 21 && patch >= 2) return 42;
        if (major == 1 && minor >= 21) return 34;
        if (major == 1 && minor >= 20 && patch >= 5) return 32;
        if (major == 1 && minor >= 20 && patch >= 3) return 22;
        if (major == 1 && minor >= 20 && patch >= 2) return 18;
        if (major == 1 && minor >= 20) return 15;
        if (major == 1 && minor == 19 && patch >= 4) return 13;
        if (major == 1 && minor == 19) return 12;
        if (major == 1 && minor == 18) return 9;
        return 8;
    }
}
