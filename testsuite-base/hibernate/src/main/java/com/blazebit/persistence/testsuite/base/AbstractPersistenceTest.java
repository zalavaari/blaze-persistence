/*
 * Copyright 2014 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.persistence.testsuite.base;

import java.util.Properties;

import com.blazebit.persistence.testsuite.base.AbstractJpaPersistenceTest;

/**
 *
 * @author Christian Beikov
 * @since 1.0
 */
public abstract class AbstractPersistenceTest extends AbstractJpaPersistenceTest {

    @Override
    protected Properties applyProperties(Properties properties) {
        properties.put("hibernate.connection.url", properties.get("javax.persistence.jdbc.url"));
        properties.put("hibernate.connection.password", properties.get("javax.persistence.jdbc.password"));
        properties.put("hibernate.connection.username", properties.get("javax.persistence.jdbc.user"));
        properties.put("hibernate.connection.driver_class", properties.get("javax.persistence.jdbc.driver"));
        if (System.getProperty("hibernate.dialect") != null) {
        	properties.put("hibernate.dialect", System.getProperty("hibernate.dialect"));
        } else if (properties.get("javax.persistence.jdbc.url").toString().contains("mysql")) {
        	// MySQL is drunk, it does stuff case insensitive by default...
        	properties.put("hibernate.dialect", SaneMySQLDialect.class.getName());
        	// Somethings not right with MySQL, we need c3p0...
            properties.put("hibernate.c3p0.min_size", 0);
            properties.put("hibernate.c3p0.max_size", 5);
            properties.put("hibernate.c3p0.timeout", 300);
            properties.put("hibernate.c3p0.max_statements", 50);
            properties.put("hibernate.c3p0.idle_test_period", 3000);
        }
        properties.put("hibernate.hbm2ddl.auto", "create-drop");
        
        // We use the following only for debugging purposes
        // Normally these settings should be disabled since the output would be too big TravisCI
//        properties.put("hibernate.show_sql", "true");
//        properties.put("hibernate.format_sql", "true");
        return properties;
    }
}