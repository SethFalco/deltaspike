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
package org.apache.deltaspike.test.core.impl.config.converter;

import org.apache.deltaspike.core.impl.config.converter.UuidConverter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

/**
 * @author seth@elypia.org (Seth Falco)
 * @since 1.9.5
 */
public class UuidConverterTest
{
    private UuidConverter converter;

    @Before
    public void before() {
        converter = new UuidConverter();
    }

    @Test
    public void testConverteringPattern()
    {
        final UUID expected = UUID.fromString("cebfb280-a7b0-4561-bc75-f71a1a08f66b");
        final UUID actual = converter.convert("cebfb280-a7b0-4561-bc75-f71a1a08f66b");

        Assert.assertEquals(expected, actual);
    }
}