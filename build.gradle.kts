plugins {
    id("java-library")
    id("maven-publish")
}

allprojects {
    group = "com.github.aecsocket"
    version = "1.4"
    description = "Common utilities for Minecraft projects"
}

subprojects {
    apply<JavaLibraryPlugin>()

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks {
        javadoc {
            val opt = options as StandardJavadocDocletOptions
            opt.encoding = "UTF-8"
            opt.links(
                    "https://docs.oracle.com/en/java/javase/17/docs/api/",
                    "https://guava.dev/releases/snapshot-jre/api/docs/",
                    "https://configurate.aoeu.xyz/4.1.2/apidocs/",
                    "https://jd.adventure.kyori.net/api/4.9.3/",
                    "https://www.javadoc.io/doc/io.leangen.geantyref/geantyref/1.3.11/",

                    "https://papermc.io/javadocs/paper/1.18/",
                    "https://javadoc.commandframework.cloud/",
                    "https://aadnk.github.io/ProtocolLib/Javadoc/"
            )

            opt.addBooleanOption("html5", true)
            opt.addStringOption("-release", "17")
            opt.linkSource()
        }

        compileJava {
            options.encoding = Charsets.UTF_8.name()
        }

        test {
            useJUnitPlatform()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
