plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")

}
architectury {
    common("forge", "fabric")
    platformSetupLoomIde()
}

dependencies {

    //minecraft("net.minecraft:minecraft:${property("minecraft_version")}")
    //mappings(loom.officialMojangMappings())
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modCompileOnly("com.cobblemon:mod:${property("cobblemon_version")}")
    // alL fabric dependencies:
    modCompileOnly("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    modCompileOnly("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    modImplementation("dev.architectury:architectury:${property("architectury_version")}")
    modImplementation("ca.landonjw.gooeylibs:api:${property("gooeylibs_version")}")


    // Database
    //api("org.mongodb:mongodb-driver-reactivestreams:5.1.2")
    api("org.mongodb:mongodb-driver-sync:5.1.2")

    // Lombok
    annotationProcessor("org.projectlombok:lombok:1.18.20")
    implementation("org.projectlombok:lombok:1.18.20")

    // LuckPerms
    api("net.luckperms:api:${property("luckperms_version")}")

    // Kyori Adventure
    modImplementation(files("/libs/CobbleUtils-common-1.1.1.jar"))
    api("net.kyori:examination-api:1.3.0")
    api("net.kyori:examination-string:1.3.0")
    api("net.kyori:adventure-api:4.14.0")
    api("net.kyori:adventure-key:4.14.0")
    api("net.kyori:adventure-nbt:4.14.0")
    api("net.kyori:adventure-text-serializer-plain:4.14.0")
    api("net.kyori:adventure-text-serializer-legacy:4.14.0")
    api("net.kyori:adventure-text-serializer-gson:4.14.0")
    api("net.kyori:adventure-text-serializer-json:4.14.0")
    api("net.kyori:adventure-text-minimessage:4.14.0")
    api("net.kyori:adventure-text-logger-slf4j:4.14.0")
    api("net.kyori:event-api:5.0.0-SNAPSHOT")
}

