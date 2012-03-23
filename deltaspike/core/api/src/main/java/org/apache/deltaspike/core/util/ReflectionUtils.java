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

package org.apache.deltaspike.core.util;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilities for common reflection based actions. Some are basic Java Reflection based, others are CDI based.
 */
//X TODO: Look at merging this with some of the other classes from CODI, or if they're really needed
//X TODO: Also some methods need JavaDoc
@Typed()
public abstract class ReflectionUtils
{
    private ReflectionUtils()
    {
        // prevent instantiation
    }

    /**
     * <p>
     * Perform a runtime cast. Similar to {@link Class#cast(Object)}, but useful
     * when you do not have a {@link Class} object for type you wish to cast to.
     * </p>
     * <p/>
     * <p>
     * {@link Class#cast(Object)} should be used if possible
     * </p>
     *
     * @param <T> the type to cast to
     * @param obj the object to perform the cast on
     * @return the casted object
     * @throws ClassCastException if the type T is not a subtype of the object
     * @see Class#cast(Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj)
    {
        return (T) obj;
    }

    /**
     * Get all the declared fields on the class hierarchy. This <b>will</b>
     * return overridden fields.
     *
     * @param clazz The class to search
     * @return the set of all declared fields or an empty set if there are none
     */
    public static Set<Field> getAllDeclaredFields(Class<?> clazz)
    {
        HashSet<Field> fields = new HashSet<Field>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass())
        {
            Collections.addAll(fields, c.getDeclaredFields());
        }
        return fields;
    }

    /**
     * Search the class hierarchy for a field with the given name. Will return
     * the nearest match, starting with the class specified and searching up the
     * hierarchy.
     *
     * @param clazz The class to search
     * @param name  The name of the field to search for
     * @return The field found, or null if no field is found
     */
    public static Field tryToFindDeclaredField(Class<?> clazz, String name)
    {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass())
        {
            try
            {
                return c.getDeclaredField(name);
            }
            catch (NoSuchFieldException e)
            {
                // No-op, we continue looking up the class hierarchy
            }
        }
        return null;
    }

    /**
     * Search the annotatedType for the field, returning the
     * {@link AnnotatedField}
     *
     * @param annotatedType The annotatedType to search
     * @param field         the field to search for
     * @return The {@link AnnotatedField} found, or null if no field is found
     */
    public static <X> AnnotatedField<? super X> getField(AnnotatedType<X> annotatedType, Field field)
    {
        for (AnnotatedField<? super X> annotatedField : annotatedType.getFields())
        {
            if (annotatedField.getDeclaringType().getJavaClass().equals(field.getDeclaringClass()) &&
                    annotatedField.getJavaMember().getName().equals(field.getName()))
            {
                return annotatedField;
            }
        }
        return null;
    }

    /**
     * Get all the declared methods on the class hierarchy. This <b>will</b>
     * return overridden methods.
     *
     * @param clazz The class to search
     * @return the set of all declared methods or an empty set if there are none
     */
    public static Set<Method> getAllDeclaredMethods(Class<?> clazz)
    {
        HashSet<Method> methods = new HashSet<Method>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass())
        {
            Collections.addAll(methods, c.getDeclaredMethods());
        }
        return methods;
    }

    private static String buildInvokeMethodErrorMessage(Method method, Object obj, Object... args)
    {
        StringBuilder message = new StringBuilder(String.format(
                "Details: Exception invoking method [%s] on object [%s], using arguments [", method.getName(), obj));
        if (args != null)
        {
            for (int i = 0; i < args.length; i++)
            {
                message.append(i > 0 ? ", " : "").append(args[i]);
            }
        }
        message.append("]");
        return message.toString();
    }

    /**
     * Set the accessibility flag on the {@link AccessibleObject} as described in
     * {@link AccessibleObject#setAccessible(boolean)} within the context of
     * a {@link java.security.PrivilegedAction}.
     *
     * @param <A>    member the accessible object type
     * @param member the accessible object
     * @return the accessible object after the accessible flag has been altered
     */
    @Deprecated //X TODO use SecurityService of OWB
    public static <A extends AccessibleObject> A setAccessible(A member)
    {
        AccessController.doPrivileged(new SetAccessiblePrivilegedAction(member));
        return member;
    }

    /**
     * <p>
     * Invoke the method on the instance, with any arguments specified, casting
     * the result of invoking the method to the expected return type.
     * </p>
     * <p/>
     * <p>
     * This method wraps {@link Method#invoke(Object, Object...)}, converting the
     * checked exceptions that {@link Method#invoke(Object, Object...)} specifies
     * to runtime exceptions.
     * </p>
     * <p/>
     * <p>
     * If instructed, this method attempts to set the accessible flag of the method in a
     * {@link java.security.PrivilegedAction} before invoking the method.
     * </p>
     *
     * @param setAccessible flag indicating whether method should first be set as
     *                      accessible
     * @param method        the method to invoke
     * @param instance      the instance to invoke the method
     * @param args          the arguments to the method
     * @return the result of invoking the method, or null if the method's return
     *         type is void
     * @throws RuntimeException            if this <code>Method</code> object enforces Java
     *                                     language access control and the underlying method is
     *                                     inaccessible or if the underlying method throws an exception or
     *                                     if the initialization provoked by this method fails.
     * @throws IllegalArgumentException    if the method is an instance method and
     *                                     the specified <code>instance</code> argument is not an instance
     *                                     of the class or interface declaring the underlying method (or
     *                                     of a subclass or implementor thereof); if the number of actual
     *                                     and formal parameters differ; if an unwrapping conversion for
     *                                     primitive arguments fails; or if, after possible unwrapping, a
     *                                     parameter value cannot be converted to the corresponding formal
     *                                     parameter type by a method invocation conversion.
     * @throws NullPointerException        if the specified <code>instance</code> is
     *                                     null and the method is an instance method.
     * @throws ClassCastException          if the result of invoking the method cannot be
     *                                     cast to the expectedReturnType
     * @throws ExceptionInInitializerError if the initialization provoked by this
     *                                     method fails.
     * @see Method#invoke(Object, Object...)
     */
    public static <T> T invokeMethod(Object instance, 
                                     Method method, Class<T> expectedReturnType,
                                     boolean setAccessible,
                                     Object... args)
    {
        if (setAccessible && !method.isAccessible())
        {
            setAccessible(method);
        }

        try
        {
            return expectedReturnType.cast(method.invoke(instance, args));
        }
        catch (Exception e)
        {
            String customMessage = createCustomMessage(e, method, instance, args);
            ExceptionUtils.changeAndThrowException(e, customMessage);
            //won't happen
            return null;
        }
    }

    private static String createCustomMessage(Exception e, Method method, Object targetObject, Object... arguments)
    {
        return e.getMessage() + buildInvokeMethodErrorMessage(method, targetObject, arguments);
    }

    /**
     * Extract the raw type, given a type.
     *
     * @param <T>  the type
     * @param type the type to extract the raw type from
     * @return the raw type, or null if the raw type cannot be determined.
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> getRawType(Type type)
    {
        if (type instanceof Class<?>)
        {
            return (Class<T>) type;
        }
        else if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class<?>)
        {
            return (Class<T>) ((ParameterizedType) type).getRawType();
        }
        return null;
    }

    /**
     * Check if a class is serializable.
     *
     * @param clazz The class to check
     * @return true if the class implements serializable or is a primitive (needed for type {@link Void}
     */
    public static boolean isSerializable(Class<?> clazz)
    {
        return clazz.isPrimitive() || Serializable.class.isAssignableFrom(clazz);
    }

    /**
     * Checks if class is final
     *
     * @param clazz The class to check
     * @return True if final, false otherwise
     */
    public static boolean isFinal(Class<?> clazz)
    {
        return Modifier.isFinal(clazz.getModifiers());
    }

    /**
     * Checks if member is final
     *
     * @param member The member to check
     * @return True if final, false otherwise
     */
    public static boolean isFinal(Member member)
    {
        return Modifier.isFinal(member.getModifiers());
    }

    /**
     * Checks if member is private
     *
     * @param member The member to check
     * @return True if final, false otherwise
     */
    public static boolean isPrivate(Member member)
    {
        return Modifier.isPrivate(member.getModifiers());
    }

    public static boolean isPackagePrivate(int mod)
    {
        return !(Modifier.isPrivate(mod) || Modifier.isProtected(mod) || Modifier.isPublic(mod));
    }

    /**
     * Checks if type is static
     *
     * @param type Type to check
     * @return True if static, false otherwise
     */
    public static boolean isStatic(Class<?> type)
    {
        return Modifier.isStatic(type.getModifiers());
    }

    /**
     * Checks if member is static
     *
     * @param member Member to check
     * @return True if static, false otherwise
     */
    public static boolean isStatic(Member member)
    {
        return Modifier.isStatic(member.getModifiers());
    }

    public static boolean isTransient(Member member)
    {
        return Modifier.isTransient(member.getModifiers());
    }

    /**
     * Checks if a method is abstract
     *
     * @param method
     * @return
     */
    public static boolean isAbstract(Method method)
    {
        return Modifier.isAbstract(method.getModifiers());
    }

    /**
     * Gets the actual type arguments of a class
     *
     * @param clazz The class to examine
     * @return The type arguments
     */
    public static Type[] getActualTypeArguments(Class<?> clazz)
    {
        if (clazz == null)
        {
            throw new IllegalArgumentException("null isn't supported");
        }

        return clazz.getTypeParameters();
    }

    /**
     * Gets the actual type arguments of a Type
     *
     * @param type The type to examine
     * @return The type arguments
     */
    public static Type[] getActualTypeArguments(Type type)
    {
        if (type instanceof Class)
        {
            return getActualTypeArguments((Class)type);
        }

        throw new IllegalArgumentException((type != null ? type.getClass().getName() : "null") + " isn't supported");
    }

    /**
     * Checks if raw type is array type
     *
     * @param rawType The raw type to check
     * @return True if array, false otherwise
     */
    public static boolean isArrayType(Class<?> rawType)
    {
        return rawType.isArray();
    }

    /**
     * Checks if type is parameterized type
     *
     * @param type The type to check
     * @return True if parameterized, false otherwise
     */
    public static boolean isParameterizedType(Class<?> type)
    {
        return type.getTypeParameters().length > 0;
    }

    public static boolean isParameterizedTypeWithWildcard(Class<?> type)
    {
        if (isParameterizedType(type))
        {
            return containsWildcards(type.getTypeParameters());
        }
        else
        {
            return false;
        }
    }

    private static boolean containsWildcards(Type[] types)
    {
        for (Type type : types)
        {
            if (type instanceof WildcardType)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isPrimitive(Type type)
    {
        Class<?> rawType = getRawType(type);
        return rawType != null && rawType.isPrimitive();
    }
}
