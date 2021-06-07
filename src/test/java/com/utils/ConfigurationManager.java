package com.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationManager {
    private String baseUrl;
    private String email;
    private String password;
    private String browser;
    private String projectHomeDirectory = System.getProperty("user.dir");
    private final String configurationLocation = projectHomeDirectory + "/src/test/resources//Configuration.properties";

    public ConfigurationManager() {
        loadData();
    }


    private void loadData() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(getConfigurationLocation()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("There is something wrong with the configuration file. Does it exist? " +
                    "File location: " + getConfigurationLocation());
        }
        email = properties.getProperty("email");
        password = properties.getProperty("password");
        baseUrl = properties.getProperty("baseUrl");
        browser = properties.getProperty("browser");
    }

    public String getBrowser() {
        return browser;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getConfigurationLocation() {
        return configurationLocation;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
