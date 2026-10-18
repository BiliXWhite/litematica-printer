package me.aleksilassila.litematica.printer;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModDependencyMetadataTest {
    @Test void processedMetadataRequiresTheFabricApisUsedDuringInitialization() throws Exception {
        try (var stream = getClass().getResourceAsStream("/fabric.mod.json")) {
            assertNotNull(stream);
            var metadata = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var dependencies = metadata.getAsJsonObject("depends");
            // A loader must reject an installation missing the events used by the entrypoint.
            for (String module : new String[]{"fabric-content-registries-v0", "fabric-lifecycle-events-v1", "fabric-networking-api-v1"}) {
                assertTrue(dependencies.has("fabric-api") || dependencies.has(module),
                        "Standalone metadata does not require " + module);
            }
        }
    }
}
