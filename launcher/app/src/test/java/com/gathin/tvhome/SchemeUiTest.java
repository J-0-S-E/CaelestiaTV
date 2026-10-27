package com.gathin.tvhome;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SchemeUiTest {
    private static final String[] SCHEMES = {
            "caelestia-dark", "catppuccin-mocha", "dracula-dark", "everforest-dark", "gruvbox-dark", "nord",
            "rosepine", "tokyonight",
            "caelestia-light", "catppuccin-latte", "everforest-light", "gruvbox-light", "rosepine-dawn"};

    private static Palette scheme(String name) throws Exception {
        try (InputStream in = SchemeUiTest.class.getResourceAsStream("/schemes/" + name + ".txt")) {
            assertTrue(name + " is missing from src/test/resources/schemes", in != null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            Palette p = Palette.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            assertTrue(name + " parsed to nothing", p.custom);
            return p;
        }
    }

    @Test public void contrastIsWcag() {
        assertEquals(21.0, SchemeUi.contrast(0xFF000000, 0xFFFFFFFF), 0.01);
        assertEquals(1.0, SchemeUi.contrast(0xFF123456, 0xFF123456), 0.0001);
        assertEquals(SchemeUi.contrast(0xFF336699, 0xFFEEEEEE), SchemeUi.contrast(0xFFEEEEEE, 0xFF336699), 0.0001);
    }

    @Test public void thereAreThirteenSchemesAndTheLightOnesAreLight() throws Exception {
        assertEquals(13, SCHEMES.length);
        for (String name : SCHEMES) {
            boolean light = name.endsWith("light") || name.equals("catppuccin-latte") || name.equals("rosepine-dawn");
            assertEquals(name, light, scheme(name).isLight);
        }
    }

    @Test public void textReadsOnTheSurfacesItIsDrawnOn() throws Exception {
        List<String> weak = new ArrayList<>();
        for (String name : SCHEMES) {
            Palette p = scheme(name);
            check(weak, name, "onSurface on surfaceContainerHigh (choice rows, tiles)", p.onSurface, p.surfaceContainerHigh, 4.5);
            check(weak, name, "onSurface on surfaceContainer (panels, pills)", p.onSurface, p.surfaceContainer, 4.5);
            check(weak, name, "onSurface on surface (panel bottom)", p.onSurface, p.surface, 4.5);
            check(weak, name, "onSurface on background (labels)", p.onSurface, p.background, 4.5);
            check(weak, name, "onSurfaceVariant on background (hints)", p.onSurfaceVariant, p.background, 4.5);
            check(weak, name, "onSurfaceVariant on surfaceContainer (dim lines)", p.onSurfaceVariant, p.surfaceContainer, 4.5);
            check(weak, name, "onPrimary on primary (focused rows and pills)", p.onPrimary, p.primary, 4.5);
        }
        assertTrue("Unreadable pairs:\n" + String.join("\n", weak), weak.isEmpty());
    }

    private static void check(List<String> weak, String scheme, String what, int fg, int bg, double min) {
        double ratio = SchemeUi.contrast(fg, bg);
        if (ratio < min) weak.add(String.format("%s: %s is %.2f, below %.1f", scheme, what, ratio, min));
    }

    @Test public void readableLeavesGoodTextAloneAndNudgesFaintText() {
        assertEquals(0xFF111111, Palette.readable(0xFF111111, 4.5, 0xFFFFFFFF));
        int faint = Palette.readable(0xFF999999, 4.5, 0xFFF0F0F0);   // grey on near-white: darkened
        assertTrue(SchemeUi.contrast(faint, 0xFFF0F0F0) >= 4.5);
        assertTrue(Palette.luminance(faint) < Palette.luminance(0xFF999999));
        int dim = Palette.readable(0xFF666666, 4.5, 0xFF101010);     // grey on near-black: lightened
        assertTrue(SchemeUi.contrast(dim, 0xFF101010) >= 4.5);
        assertTrue(Palette.luminance(dim) > Palette.luminance(0xFF666666));
    }

    @Test public void edgeInkIsDarkOnLightSchemesAndWhiteOnDarkOnes() throws Exception {
        assertEquals(0xFFFFFFFF, SchemeUi.edgeInk(scheme("dracula-dark")));
        Palette latte = scheme("catppuccin-latte");
        assertEquals(latte.onSurface, SchemeUi.edgeInk(latte));
        assertTrue(SchemeUi.contrast(SchemeUi.edgeInk(latte), latte.background) > 4.5);
        assertTrue(SchemeUi.contrast(SchemeUi.edgeInk(Palette.defaults()), Palette.defaults().background) > 4.5);
    }
}
