# Anko-Legacy

[Anko](https://github.com/Kotlin/anko) is deprecated but the code is still useful. I have just extracted the class parser from the [generator](https://github.com/Kotlin/anko/tree/master/anko/library/generator) module, so that we can use it as a standalone library.

Add dependency below if you would like to give it a try:

```gradle
dependencies {
    implementation("com.bennyhuo.kotlin:anko-asm-parser:0.1")
}
```