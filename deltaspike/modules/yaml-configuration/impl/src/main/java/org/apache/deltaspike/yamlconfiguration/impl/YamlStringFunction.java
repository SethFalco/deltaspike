/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.deltaspike.yamlconfiguration.impl;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Since the {@link org.apache.deltaspike.core.impl.config.MapConfigSource} insists that we call super, and Java
 * requires super be the first call of a method; we use this {@link Function}
 * to help as get around that limitation to perform more methods
 * while calling super.
 *
 * @since 1.9.5
 */
public class YamlStringFunction implements Function<String, Map<String, Object>>
{
    private final Logger log = Logger.getLogger(YamlStringFunction.class.getName());

    /**
     * @param configPath The path to the configuration file.
     * @return A nested map representing all YAML properties.
     */
    @Override
    public Map<String, Object> apply(String configPath)
    {
        try (InputStream inputStream = YamlConfigSource.class.getClassLoader().getResourceAsStream(configPath))
        {
            if (inputStream != null)
            {
                return new Yaml().load(inputStream);
            }
        }
        catch (IOException ex)
        {
            log.log(Level.SEVERE, "This hopefully should never produce any IOExceptions.", ex);
        }

        log.log(Level.WARNING, "Using YamlConfigSource, but {0} was not found on the classpath.", configPath);
        return new HashMap<>();
    }
}
