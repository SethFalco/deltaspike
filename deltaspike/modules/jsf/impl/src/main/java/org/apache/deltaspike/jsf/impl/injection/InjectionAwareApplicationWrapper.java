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
package org.apache.deltaspike.jsf.impl.injection;

import org.apache.deltaspike.jsf.api.config.JsfModuleConfig;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ApplicationWrapper;
import javax.faces.convert.Converter;
import javax.faces.validator.Validator;

public class InjectionAwareApplicationWrapper extends ApplicationWrapper
{
    private final Application wrapped;
    private final boolean containerManagedConvertersEnabled;
    private final boolean containerManagedValidatorsEnabled;

    public InjectionAwareApplicationWrapper(Application wrapped, JsfModuleConfig jsfModuleConfig)
    {
        this.wrapped = wrapped;
        this.containerManagedConvertersEnabled = jsfModuleConfig.isContainerManagedConvertersEnabled();
        this.containerManagedValidatorsEnabled = jsfModuleConfig.isContainerManagedValidatorsEnabled();
    }

    @Override
    public Converter createConverter(Class<?> targetClass)
    {
        return managedOrDefaultConverter(this.wrapped.createConverter(targetClass));
    }

    @Override
    public Converter createConverter(String converterId)
    {
        return managedOrDefaultConverter(this.wrapped.createConverter(converterId));
    }

    private Converter managedOrDefaultConverter(Converter defaultResult)
    {
        if (!this.containerManagedConvertersEnabled)
        {
            return defaultResult;
        }
        if (defaultResult == null)
        {
            return null;
        }

        Converter result = ManagedArtifactResolver.resolveManagedConverter(defaultResult.getClass());

        if (result == null)
        {
            return defaultResult;
        }
        else
        {
            return new ConverterWrapper(result);
        }
    }

    @Override
    public Validator createValidator(String validatorId) throws FacesException
    {
        return managedOrDefaultValidator(this.wrapped.createValidator(validatorId));
    }

    private Validator managedOrDefaultValidator(Validator defaultResult)
    {
        if (!this.containerManagedValidatorsEnabled)
        {
            return defaultResult;
        }
        if (defaultResult == null)
        {
            return null;
        }

        Validator result = ManagedArtifactResolver.resolveManagedValidator(defaultResult.getClass());

        if (result == null)
        {
            return defaultResult;
        }
        else
        {
            return new ValidatorWrapper(result);
        }
    }

    @Override
    public Application getWrapped()
    {
        return wrapped;
    }
}