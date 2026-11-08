# kotlinx.serialization keeps generated serializers through its own rules; keep seed models readable in stack traces.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class io.lemoncore.opendatadex.**$$serializer { *; }
-keepclassmembers class io.lemoncore.opendatadex.** {
    *** Companion;
}
