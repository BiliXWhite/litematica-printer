//#if MC > 12104
package me.aleksilassila.litematica.printer.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the compiled render method with GPU boundaries replaced; does not require an OpenGL context. */
class HighlightRenderLifecycleTest {
    @BeforeEach
    void reset() {
        HighlightRenderFixture.reset();
    }

    @Test
    void releasesOwnedBuffersAfterEveryFrame() throws Exception {
        var render = new HighlightRenderFixture();
        for (int frame = 0; frame < 100; frame++) {
            render.draw();
            assertEquals(0, HighlightRenderFixture.contexts, "render context leaked at frame " + frame);
            assertEquals(0, HighlightRenderFixture.meshes, "mesh allocation leaked at frame " + frame);
        }
        assertEquals(100, HighlightRenderFixture.draws);
        assertNull(HighlightRenderFixture.logged);
    }

    @Test
    void preservesTheFogStateOfTheCaller() throws Exception {
        var previousFog = HighlightRenderFixture.fog;
        new HighlightRenderFixture().draw();
        assertSame(previousFog, HighlightRenderFixture.fog);
    }

    @ParameterizedTest
    @EnumSource(value = HighlightRenderFixture.Failure.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void releasesBuffersAndRestoresFogWhenRenderingFails(HighlightRenderFixture.Failure failure) throws Exception {
        HighlightRenderFixture.failure = failure;
        var previousFog = HighlightRenderFixture.fog;
        new HighlightRenderFixture().draw();
        assertAll(
                () -> assertEquals(0, HighlightRenderFixture.contexts, "context must close after " + failure),
                () -> assertEquals(0, HighlightRenderFixture.meshes, "mesh must close after " + failure),
                () -> assertSame(previousFog, HighlightRenderFixture.fog, "fog must restore after " + failure),
                () -> assertSame(HighlightRenderFixture.injected, HighlightRenderFixture.logged)
        );
    }
}
//#endif
