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

package org.jetbrains.android.anko

import org.jetbrains.android.anko.sources.defaultSourceManager
import org.jetbrains.android.anko.utils.ImportList
import org.jetbrains.android.anko.utils.KMethod
import org.jetbrains.android.anko.utils.KType
import org.jetbrains.android.anko.utils.KVariable
import org.jetbrains.android.anko.utils.MethodNodeWithClass
import org.jetbrains.android.anko.utils.asJavaString
import org.jetbrains.android.anko.utils.asString
import org.jetbrains.android.anko.utils.fqName
import org.jetbrains.android.anko.utils.genericTypeToKType
import org.jetbrains.android.anko.utils.isSimpleType
import org.jetbrains.android.anko.utils.isVoid
import org.jetbrains.android.anko.utils.parseGenericMethodSignature
import org.jetbrains.android.anko.utils.toKType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.MethodNode

const val DEFAULT_NULLABILITY = false

private val specialLayoutParamsArguments = mapOf(
    "width" to "android.view.ViewGroup.LayoutParams.WRAP_CONTENT",
    "height" to "android.view.ViewGroup.LayoutParams.WRAP_CONTENT",
    "w" to "android.view.ViewGroup.LayoutParams.WRAP_CONTENT",
    "h" to "android.view.ViewGroup.LayoutParams.WRAP_CONTENT"
)

private val specialLayoutParamsNames = mapOf(
    "w" to "width", "h" to "height"
)

val MethodNode.parameterRawTypes: Array<Type>
    get() = Type.getArgumentTypes(desc)

fun getParameterKTypes(node: MethodNode): List<KType> {
    val parameterAnnotations = node.parameterAnnotations()
    if (node.signature == null) {
        return node.parameterRawTypes.mapIndexed { index, type ->
            KType(
                type.asString(false),
                isNullable = parameterAnnotations.getOrNull(index)?.nullable() ?: DEFAULT_NULLABILITY
            )
        }
    }

    val parsed = parseGenericMethodSignature(node.signature)
    return parsed.valueParameters.mapIndexed { index, type ->
        genericTypeToKType(
            type.genericType,
            isNullable = parameterAnnotations.getOrNull(index)?.nullable() ?: DEFAULT_NULLABILITY
        )
    }
}

fun MethodNode.parameterAnnotations(): List<List<AnnotationNode>> {
    val parameterAnnotations = ArrayList<List<AnnotationNode>>()

    for (i in 0 until maxOf(visibleAnnotableParameterCount, invisibleAnnotableParameterCount)) {
        val list = ArrayList<AnnotationNode>()
        invisibleParameterAnnotations?.getOrNull(i)?.let { list.addAll(it) }
        visibleParameterAnnotations?.getOrNull(i)?.let { list.addAll(it) }
        parameterAnnotations.add(list)
    }
    return parameterAnnotations
}

fun AnnotationNode?.nullable(): Boolean? {
    return when (this?.desc) {
        "Lorg/jetbrains/annotations/NotNull;" -> false
        "Lorg/jetbrains/annotations/Nullable;" -> true
        else -> null
    }
}

fun List<AnnotationNode>?.nullable(): Boolean? {
    return this?.firstNotNullOfOrNull { it.nullable() }
}

fun MethodNodeWithClass.toKMethod(): KMethod {
    val parameterTypes = getParameterKTypes(this.method)
    val localVariables = method.localVariables?.map { it.index to it }?.toMap() ?: emptyMap()

    val parameterRawTypes = this.method.parameterRawTypes
    val javaArgs = parameterRawTypes.map(Type::asJavaString)
    val parameterNames = defaultSourceManager.getParameterNames(clazz.fqName, method.name, javaArgs)
    val javaArgsString = javaArgs.joinToString()
    val methodAnnotationSignature =
        "${clazz.fqName} ${method.returnType.asJavaString()} ${method.name}($javaArgsString)"

    var nameIndex = if (method.isStatic) 0 else 1
    val parameters = parameterTypes.mapIndexed { index, type ->
        val isSimpleType = parameterRawTypes[index].isSimpleType
        val parameterName = parameterNames?.get(index) ?: localVariables[nameIndex]?.name ?: "p$index"
        nameIndex += parameterRawTypes[index].size
        KVariable(parameterName, type)
    }

    return KMethod(method.name, parameters, method.returnType.toKType())
}

fun MethodNodeWithClass.formatArguments(): String {
    return toKMethod().parameters.joinToString { "${it.name}: ${it.type}" }
}

fun MethodNodeWithClass.formatLayoutParamsArguments(
    importList: ImportList
): List<String> {
    return toKMethod().parameters.map { param ->
        val renderType = importList.let { it[param.type] }

        val defaultValue = specialLayoutParamsArguments[param.name]
        val realName = specialLayoutParamsNames.getOrElse(param.name, { param.name })

        if (defaultValue == null)
            "$realName: $renderType"
        else
            "$realName: $renderType = $defaultValue"
    }
}

fun MethodNodeWithClass.formatLayoutParamsArgumentsInvoke(): String {
    return toKMethod().parameters.joinToString { param ->
        val realName = specialLayoutParamsNames.getOrElse(param.name, { param.name })
        val explicitNotNull = if (param.type.isNullable) "!!" else ""
        "$realName$explicitNotNull"
    }
}

fun MethodNodeWithClass.formatArgumentsTypes(): String {
    return toKMethod().parameters.joinToString { it.type.toString() }
}

fun MethodNodeWithClass.formatArgumentsNames(): String {
    return toKMethod().parameters.joinToString { it.name }
}

fun MethodNode.isGetter(): Boolean {
    val isNonBooleanGetter = name.startsWith("get") && name.length > 3 && Character.isUpperCase(name[3])
    val isBooleanGetter = name.startsWith("is") && name.length > 2 && Character.isUpperCase(name[2])

    return (isNonBooleanGetter || isBooleanGetter) && parameterRawTypes.isEmpty() && !returnType.isVoid && isPublic
}

fun MethodNode.isNonListenerSetter(): Boolean {
    val isSetter = name.startsWith("set") && name.length > 3 && Character.isUpperCase(name[3])
    return isSetter && !(isListenerSetter() || name.endsWith("Listener")) && parameterRawTypes.size == 1 && isPublic
}

val MethodNode.isConstructor: Boolean
    get() = name == "<init>"

fun MethodNode.isListenerSetter(set: Boolean = true, add: Boolean = true): Boolean {
    return ((set && name.startsWith("setOn")) || (add && name.startsWith("add"))) && name.endsWith("Listener")
}

val MethodNode.isPublic: Boolean
    get() = (access and Opcodes.ACC_PUBLIC) != 0

val MethodNode.isOverridden: Boolean
    get() = (access and Opcodes.ACC_BRIDGE) != 0

val MethodNode.isStatic: Boolean
    get() = (access and Opcodes.ACC_STATIC) != 0

val MethodNode.isSynthetic: Boolean
    get() = (access and Opcodes.ACC_SYNTHETIC) != 0

val MethodNode.returnType: Type
    get() = Type.getReturnType(desc)