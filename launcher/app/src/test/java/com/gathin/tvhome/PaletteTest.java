package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PaletteTest {
    private static final String DRACULA = "background 1A1B23\nonSurface F8F8F2\nsurfaceContainer 22232D\n"
            + "surfaceContainerHigh 2A2C38\nonSurfaceVariant F8F8F2\nprimary BD93F9\nonPrimary 1A1B23\n"
            + "secondary 50FA7B\ntertiary FF79C6\noutline 6272A4\noutlineVariant 2E3040\nerror FF5555\nsurface 1E1F28\n";

    @Test public void aFullFileFillsEveryRoleOpaque() {
        Palette p = Palette.parse(DRACULA);
        assertTrue(p.custom);
        assertEquals(0xFF1A1B23, p.background);
        assertEquals(0xFF1E1F28, p.surface);
        assertEquals(0xFF22232D, p.surfaceContainer);
        assertEquals(0xFF2A2C38, p.surfaceContainerHigh);
        assertEquals(0xFFF8F8F2, p.onSurface);
        assertEquals(0xFFBD93F9, p.primary);
        assertEquals(0xFF1A1B23, p.onPrimary);
        assertEquals(0xFF50FA7B, p.secondary);
        assertEquals(0xFFFF79C6, p.tertiary);
        assertEquals(0xFF6272A4, p.outline);
        assertEquals(0xFF2E3040, p.outlineVariant);
        assertEquals(0xFFFF5555, p.error);
        assertFalse(p.isLight);
    }

    @Test public void missingRolesKeepTheDefaults() {
        Palette p = Palette.parse("primary 112233\n");
        Palette d = Palette.defaults();
        assertTrue(p.custom);
        assertEquals(0xFF112233, p.primary);
        assertEquals(d.background, p.background);
        assertEquals(d.onSurface, p.onSurface);
        assertEquals(d.outlineVariant, p.outlineVariant);
    }

    @Test public void unknownKeysAreIgnored() {
        Palette p = Palette.parse("term0 FFFFFF\nsurfaceTint 123456\nprimary 445566\n");
        assertEquals(0xFF445566, p.primary);
        assertEquals(Palette.defaults().background, p.background);
    }

    @Test public void malformedLinesAreSkippedAndTheRestStillCounts() {
        Palette p = Palette.parse("primary\nsecondary ZZZZZZ\ntertiary 12345\nerror 1234567\n"
                + "outline #AABBCC\n   \n\nonPrimary 0A0B0C\r\nbackground 010203\r\n");
        Palette d = Palette.defaults();
        assertEquals(d.primary, p.primary);
        assertEquals(d.secondary, p.secondary);
        assertEquals(d.tertiary, p.tertiary);
        assertEquals(d.error, p.error);
        assertEquals(d.outline, p.outline);
        assertEquals(0xFF0A0B0C, p.onPrimary);
        assertEquals(0xFF010203, p.background);
    }

    @Test public void lowerCaseHexAndTabsWork() {
        Palette p = Palette.parse("primary\tbd93f9\n");
        assertEquals(0xFFBD93F9, p.primary);
    }

    @Test public void emptyOrGarbageGivesTheDefaultPalette() {
        assertSame(Palette.defaults(), Palette.parse(null));
        assertSame(Palette.defaults(), Palette.parse(""));
        assertSame(Palette.defaults(), Palette.parse("hello world\n\u0000\u0001 \n---"));
        assertFalse(Palette.defaults().custom);
    }

    @Test public void theDefaultsAreTodaysWhiteOnBlack() {
        Palette d = Palette.defaults();
        assertEquals(0xFF000000, d.background);
        assertEquals(0xFFFFFFFF, d.onSurface);
        assertEquals(0xFF1C1C1E, d.surfaceContainerHigh);
        assertFalse(d.isLight);
    }

    @Test public void lightnessFollowsTheBackground() {
        assertTrue(Palette.parse("background EFF1F5\n").isLight);
        assertTrue(Palette.parse("background FFFFFF\n").isLight);
        assertFalse(Palette.parse("background 1E1E2E\n").isLight);
        assertFalse(Palette.parse("background 000000\n").isLight);
        // A light surface does not make the scheme light, only the background does.
        assertFalse(Palette.parse("background 101010\nsurface FFFFFF\n").isLight);
    }

    @Test public void mixAndAlpha() {
        assertEquals(0xFF808080, Palette.mix(0xFF000000, 0xFFFFFFFF, 0.5f));
        assertEquals(0x99112233, Palette.withAlpha(0xFF112233, 0x99));
    }

    @Test public void displayNames() {
        assertEquals("Dracula dark", Palette.displayName("dracula-dark.txt"));
        assertEquals("Rosepine", Palette.displayName("rosepine"));
        assertEquals("Catppuccin mocha", Palette.displayName("catppuccin_mocha"));
    }
}
