package com.meerkat.map;

import com.meerkat.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CupFile {

    private static final Pattern pattern = Pattern.compile("\\s*,?\\s*(?:\"([^\"]*)\"|([^,]*))");

    /**
     * @param file File to read from
     */
    public static ArrayList<Cup> readAll(File file) {
        var result = new ArrayList<Cup>();
        Log.i("Opening CupFile " + file.getPath());
        String line = "";
        try (var reader = new BufferedReader(new FileReader(file))) {
            while (reader.ready()) {
                line = reader.readLine().trim();
                if (line.startsWith("*")) continue;
                Matcher matcher = pattern.matcher(line);
                int i;
                var fields = new String[11];
                for (i = 0; matcher.find() && i < fields.length; i++) {
                    fields[i] = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
                }
                if (i < 11) {
                    Log.e("Cup file line error: %s", line);
                    continue;
                }
                result.add(new Cup(fields));
            }
        } catch (NumberFormatException e) {
            Log.e("Cup file invalid line: %s", line);
        } catch (FileNotFoundException e) {
            Log.e("Cup file not found: %s", file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("Cup file IOException: %s", file.getAbsolutePath());
        }
        return result;
    }
}
