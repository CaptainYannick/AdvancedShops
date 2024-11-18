package io.captainyannick.advancedShops.core.utils;

import org.bukkit.inventory.ItemStack;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class TextUtils {
    protected static final List<Charset> supportedCharsets = new ArrayList();

    public static String formatText(String text) {
        return formatText(text, false);
    }

    public static String formatText(String text, boolean capitalize) {
        if (text != null && !text.equals("")) {
            if (capitalize) {
                text = text.substring(0, 1).toUpperCase() + text.substring(1);
            }

            return (new HexText(text)).translateColorCodes().parseHex().toString();
        } else {
            return "";
        }
    }

    public static List<String> formatText(List<String> list) {
        return (List)list.stream().map(TextUtils::formatText).collect(Collectors.toList());
    }

    public static String convertToInvisibleLoreString(String s) {
        if (s != null && !s.equals("")) {
            StringBuilder hidden = new StringBuilder();
            char[] var2 = s.toCharArray();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                char c = var2[var4];
                hidden.append('§').append(';').append('§').append(c);
            }

            return hidden.toString();
        } else {
            return "";
        }
    }

    public static String convertToInvisibleString(String s) {
        if (s != null && !s.equals("")) {
            StringBuilder hidden = new StringBuilder();
            char[] var2 = s.toCharArray();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                char c = var2[var4];
                hidden.append('§').append(c);
            }

            return hidden.toString();
        } else {
            return "";
        }
    }

    public static String convertFromInvisibleString(String s) {
        return s != null && !s.equals("") ? s.replaceAll("§;§|§", "") : "";
    }

    public static Charset detectCharset(File f, Charset def) {
        byte[] buffer = new byte[2048];
        boolean var3 = true;

        int read;
        try {
            FileInputStream input = new FileInputStream(f);
            Throwable var5 = null;

            try {
                read = input.read(buffer);
            } catch (Throwable var15) {
                var5 = var15;
                throw var15;
            } finally {
                if (input != null) {
                    if (var5 != null) {
                        try {
                            input.close();
                        } catch (Throwable var14) {
                            var5.addSuppressed(var14);
                        }
                    } else {
                        input.close();
                    }
                }

            }
        } catch (Exception var17) {
            return null;
        }

        return read != -1 ? detectCharset(buffer, read, def) : def;
    }

    public static Charset detectCharset(BufferedInputStream reader, Charset def) {
        byte[] buffer = new byte[2048];

        int read;
        try {
            reader.mark(2048);
            read = reader.read(buffer);
            reader.reset();
        } catch (Exception var5) {
            return null;
        }

        return read != -1 ? detectCharset(buffer, read, def) : def;
    }

    public static Charset detectCharset(byte[] data, int len, Charset def) {
        if (len > 4) {
            if (data[0] == -1 && data[1] == -2) {
                return StandardCharsets.UTF_16LE;
            }

            if (data[0] == -2 && data[1] == -1) {
                return StandardCharsets.UTF_16BE;
            }

            if (data[0] == -17 && data[1] == -69 && data[2] == -65) {
                return StandardCharsets.UTF_8;
            }
        }

        Iterator var3 = supportedCharsets.iterator();

        Charset charset;
        do {
            if (!var3.hasNext()) {
                return def;
            }

            charset = (Charset)var3.next();
        } while(charset == null || !isCharset(data, len, charset));

        return charset;
    }

    public static boolean isCharset(byte[] data, int len, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException var4) {
            return false;
        }
    }

    static {
        supportedCharsets.add(StandardCharsets.UTF_8);
        supportedCharsets.add(StandardCharsets.ISO_8859_1);

        try {
            supportedCharsets.add(Charset.forName("windows-1253"));
            supportedCharsets.add(Charset.forName("ISO-8859-7"));
        } catch (Exception var1) {
        }

        supportedCharsets.add(StandardCharsets.US_ASCII);
    }

    public static String formatItemName(ItemStack item) {
        if (item == null || item.getType() == null) return "Unknown Item";

        String rawName = item.getType().toString(); // Get the enum name (e.g., SPRUCE_LOG)
        String[] words = rawName.split("_"); // Split by underscores (e.g., [SPRUCE, LOG])

        // Capitalize each word and join them with a space
        StringBuilder formattedName = new StringBuilder();
        for (String word : words) {
            formattedName.append(word.substring(0, 1).toUpperCase()) // First letter capitalized
                    .append(word.substring(1).toLowerCase()) // Remaining letters lowercase
                    .append(" ");
        }

        return formattedName.toString().trim(); // Remove trailing space
    }
}
