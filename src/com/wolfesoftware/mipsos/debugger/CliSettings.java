package com.wolfesoftware.mipsos.debugger;

import java.lang.reflect.Field;

import com.wolfesoftware.mipsos.common.Util;

public class CliSettings
{
    public static final String settingsPath = ".mips-os_settings";
    public String autoCommand = "registers; list";
    public int listRadius = 5;
    public int reigsterDisplayWidth = 4;

    private CliSettings()
    {
    }

    public static CliSettings load()
    {
        CliSettings settings = new CliSettings();
        String[] lines = null;
        if (lines == null) {
            // look in ./
            try {
                lines = Util.readLines(settingsPath);
            } catch (RuntimeException e) {
            }
        }
        if (lines == null) {
            // look in ~/
            String homeDir = System.getenv("HOME");
            if (homeDir == null) {
                // try windows
                String homeDrive = System.getenv("HOMEDRIVE");
                String homePath = System.getenv("HOMEPATH");
                homeDir = homeDrive + homePath;
            }
            try {
                lines = Util.readLines(homeDir + "/" + settingsPath);
            } catch (RuntimeException e) {
            }
        }
        if (lines != null) {
            for (String line : lines) {
                line = line.trim();
                if (line.equals("") || line.startsWith("#"))
                    continue;
                String[] keyAndValue = line.split("=", 2);
                String key = keyAndValue[0], value = keyAndValue[1];
                try {
                    Field field = CliSettings.class.getField(key);
                    field.set(settings, value);
                } catch (NoSuchFieldException e) {
                    throw null;
                } catch (IllegalAccessException e) {
                    throw null;
                }
            }
        }
        return settings;
    }
}
