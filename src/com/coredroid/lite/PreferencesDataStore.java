package com.coredroid.lite;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Save the objects in shared preferences
 */
public class PreferencesDataStore implements DataStore {

    private static final String CLASS_SUFFIX = "-Class";

    private static final String PREFS_NAME = "AppState";

    private static final String PERSISTENT_PREFS_NAME = "PersistentPrefs";

    private SharedPreferences settings;

    private SharedPreferences persistentSettings;

    private ObjectWriter mObjectWriter;

    private ObjectMapper mObjectMapper;

    public PreferencesDataStore(Context context) {
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        persistentSettings = context.getSharedPreferences(
                PERSISTENT_PREFS_NAME, 0);

        mObjectMapper = new ObjectMapper();

        mObjectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mObjectWriter = mObjectMapper.writer().withDefaultPrettyPrinter();

    }

    @Override
    public void clear() {
        settings.edit().clear().commit();
    }

    @Override
    public void dump(OutputStream out) throws IOException {
        PrintStream writer = new PrintStream(out);

        writer.append("Persistent\n");
        for (Entry entry : persistentSettings.getAll().entrySet()) {

            writer.append("\t").append(entry.getKey().toString()).append(":")
                    .append(mObjectWriter.writeValueAsString(entry.getValue()))
                    .append("\n");
        }

        writer.append("\nSettings\n");
        for (Entry entry : settings.getAll().entrySet()) {
            writer.append("\t").append(entry.getKey().toString()).append(":")
                    .append(mObjectWriter.writeValueAsString(entry.getValue()))
                    .append("\n");
        }
    }

    @Override
    public void save(String key, CoreObject obj) {
        long start = System.currentTimeMillis();
        if (obj == null) {
            SharedPreferences prefs = persistentSettings.contains(key) ? persistentSettings
                    : settings;
            Editor editor = prefs.edit();
            editor.remove(key);
            editor.commit();
        } else {
            SharedPreferences prefs = obj.isPersistent() ? persistentSettings
                    : settings;

            LogIt.d(this, "Save application preferences");
            Editor editor = prefs.edit();
            try {
                editor.putString(key, mObjectWriter.writeValueAsString(obj));
            } catch (JsonProcessingException e) {
                LogIt.e(PreferencesDataStore.class, e, e.getMessage());
            }
            editor.putString(key + CLASS_SUFFIX, obj != null ? obj.getClass()
                    .getName() : null);
            editor.commit();
        }
        LogIt.d(this, "TIMER " + key + ": "
                + (System.currentTimeMillis() - start));
    }

    @SuppressWarnings("unchecked")
    @Override
    public CoreObject get(String key) {
        SharedPreferences prefs = settings;
        String objString = prefs.getString(key, null);

        if (objString == null) {
            prefs = persistentSettings;
            objString = prefs.getString(key, null);
        }

        if (objString != null) {
            String classStr = prefs.getString(key + CLASS_SUFFIX, null);

            if (classStr != null) {
                try {
                    Class c = Class.forName(classStr);
                    LogIt.d(this, "Load application preferences");

                    return (CoreObject) mObjectMapper.readValue(objString, c);
                } catch (ClassNotFoundException e) {
                    LogIt.e(this, e, "Could not find class for entry", key,
                            e.getMessage(), objString);
                } catch (Exception e) {
                    LogIt.e(this,
                            e,
                            "Exception loading saved application preferences, discarding and recreating them - the user will be forced to login again",
                            e.getMessage(), objString);
                }
            } else {
                LogIt.w(this, "Could not find class type for entry", key,
                        classStr);
            }
        }
        return null;
    }

}
