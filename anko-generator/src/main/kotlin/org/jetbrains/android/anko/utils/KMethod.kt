package org.jetbrains.android.anko.utils

class KMethod(val name: String,
              val typeParameters: List<TypeParameter>,
              val parameters: List<KVariable>,
              val returnType: KType)