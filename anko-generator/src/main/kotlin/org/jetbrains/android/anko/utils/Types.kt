/*
 * Copyright 2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.android.anko.utils

import org.objectweb.asm.Type

val Type.isVoid: Boolean
    get() = sort == Type.VOID

val Type.isSimpleType: Boolean
    get() = when (sort) {
        Type.BOOLEAN, Type.INT, Type.FLOAT, Type.DOUBLE, Type.LONG, Type.BYTE, Type.CHAR, Type.SHORT -> true
        Type.VOID -> true
        else -> false
    }

val Type.fqName: String
    get() = className.replace('/', '.').replace('$', '.')

private fun mapJavaToKotlinType(str: String): String {
    return when (str) {
        "java.lang.CharSequence" -> "kotlin.CharSequence"
        "java.lang.Number" -> "kotlin.Number"
        "java.lang.String" -> "kotlin.String"
        "java.lang.Integer" -> "kotlin.Int"
        "java.lang.Object" -> "kotlin.Any"
        "java.lang.Comparable" -> "kotlin.Comparable"
        "java.util.List" -> "kotlin.collections.List"
        "java.util.Set" -> "kotlin.collections.Set"
        "java.util.Map" -> "kotlin.collections.Map"
        else -> str
    }
}

fun Type.toKType(isNullable: Boolean = false): KType {
    return KType(asString(false), isNullable)
}

fun Type.asString(isNullable: Boolean = true): String {
    val nullability = if (isNullable) "?" else ""
    return when (sort) {
        Type.BOOLEAN -> "kotlin.Boolean"
        Type.INT -> "kotlin.Int"
        Type.FLOAT -> "kotlin.Float"
        Type.DOUBLE -> "kotlin.Double"
        Type.LONG -> "kotlin.Long"
        Type.BYTE -> "kotlin.Byte"
        Type.CHAR -> "kotlin.Char"
        Type.SHORT -> "kotlin.Short"
        Type.VOID -> "kotlin.Unit"
        Type.ARRAY -> when (elementType.sort) {
            Type.INT -> "kotlin.IntArray$nullability"
            Type.FLOAT -> "kotlin.FloatArray$nullability"
            Type.DOUBLE -> "kotlin.DoubleArray$nullability"
            Type.LONG -> "kotlin.LongArray$nullability"
            else -> "kotlin.Array<" + mapJavaToKotlinType(elementType.asString(isNullable = false)) + ">$nullability"
        }
        else -> mapJavaToKotlinType(fqName) + nullability
    }
}

fun Type.asJavaString(): String {
    return when (sort) {
        Type.BOOLEAN -> "boolean"
        Type.INT -> "int"
        Type.FLOAT -> "float"
        Type.DOUBLE -> "double"
        Type.LONG -> "long"
        Type.BYTE -> "byte"
        Type.CHAR -> "char"
        Type.SHORT -> "short"
        Type.VOID -> "void"
        Type.ARRAY -> elementType.asJavaString() + "[]"
        else -> fqName
    }
}

fun Type.getDefaultValue(onlyPrimitive: Boolean = false): String {
    return when (sort) {
        Type.BOOLEAN -> "false"
        Type.INT -> "0"
        Type.FLOAT -> "0.0"
        Type.DOUBLE -> "0.0"
        Type.LONG -> "0"
        Type.BYTE -> "0"
        Type.CHAR -> "\'\\u0000\'" //default value of a char
        Type.SHORT -> "0"
        Type.VOID -> ""
        else -> {
            if (onlyPrimitive) {
                return ""
            } else when (sort) {
                Type.ARRAY -> when (elementType.sort) {
                    Type.INT -> "IntArray()"
                    Type.FLOAT -> "FloatArray()"
                    Type.DOUBLE -> "DoubleArray()"
                    Type.LONG -> "LongArray()"
                    else -> "Array<" + mapJavaToKotlinType(elementType.asString(isNullable = false)) + ">()"
                }
                else -> mapJavaToKotlinType(fqName) + "()"
            }
        }
    }
}

fun genericTypeToKType(
    type: GenericType,
    isNullable: Boolean = false,
    variance: KType.Variance = KType.Variance.INVARIANT
): KType {
    val classifier = type.classifier

    val fqName = when (classifier) {
        is TopLevelClass -> mapJavaToKotlinType(classifier.internalName.replace('/', '.').replace('$', '.'))
        is BaseType -> Type.getType(classifier.descriptor.toString()).asString(isNullable = false)
        is TypeVariable -> classifier.name
        else -> error("Invalid classifier type: $classifier")
    }

    val args = type.arguments.map { arg ->
        when (arg) {
            is UnboundedWildcard -> KType.STAR_TYPE
            is NoWildcard -> genericTypeToKType(arg.genericType)
            is BoundedWildcard ->
                return when (arg.wildcard) {
                    Wildcard.EXTENDS -> genericTypeToKType(arg.bound, false, KType.Variance.COVARIANT)
                    Wildcard.SUPER -> genericTypeToKType(arg.bound, false, KType.Variance.CONTRAVARIANT)
                }
            else -> throw RuntimeException("Unexpected generic argument type: $arg")
        }
    }

    return KType(fqName, isNullable, variance, args, classifier is TypeVariable)
}

fun getPackageName(fqName: String): String {
    val indexOfFirstCapital = fqName.indexOfFirst(Char::isUpperCase)
    return fqName.substring(0, indexOfFirstCapital).substringBeforeLast('.')
}

fun getJavaClassName(fqName: String): String {
    return fqName.substring(getPackageName(fqName).length + 1)
}