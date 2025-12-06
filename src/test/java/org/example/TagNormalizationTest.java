package org.example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class TagNormalizationTest {

    private static Method isUsefulTagMethod;
    private static Method canonicalTagKeyMethod;

    @BeforeAll
    static void setUp() throws Exception {
        Class<?> clazz = Class.forName("org.example.metadataScraper");

        isUsefulTagMethod = clazz.getDeclaredMethod("isUsefulTag", String.class);
        isUsefulTagMethod.setAccessible(true);

        canonicalTagKeyMethod = clazz.getDeclaredMethod("canonicalTagKey", String.class);
        canonicalTagKeyMethod.setAccessible(true);
    }

    private boolean callIsUseful(String tag) throws Exception {
        return (Boolean) isUsefulTagMethod.invoke(null, tag);
    }

    private String callCanonical(String tag) throws Exception {
        return (String) canonicalTagKeyMethod.invoke(null, tag);
    }

    @Test
    void isUsefulTag_filtersNullEmptyAndUrls() throws Exception {
        assertFalse(callIsUseful(null), "null should not be a useful tag");
        assertFalse(callIsUseful("   "), "blank should not be a useful tag");
        assertFalse(callIsUseful("http://example.com"), "URLs should be filtered");
        assertFalse(callIsUseful("www.example.com"), "www.* should be filtered");
        assertFalse(callIsUseful("tag@example.com"), "email-like tags should be filtered");
    }

    @Test
    void isUsefulTag_acceptsNormalGenreTags() throws Exception {
        assertTrue(callIsUseful("rock"), "simple genre should be accepted");
        assertTrue(callIsUseful("Indie Rock"), "multi-word genre should be accepted");
        assertTrue(callIsUseful("drum and bass"), "normal text genre should be accepted");
    }

    @Test
    void isUsefulTag_filtersLastfmStyleNoise() throws Exception {
        // matches pattern: contains digits + 'fm'
        assertFalse(callIsUseful("lastfm123"), "lastfm-style noisy tags should be filtered");
        assertFalse(callIsUseful("123fm"), "tags with digits + 'fm' should be filtered");
    }

    @Test
    void canonicalTagKey_normalizesToLowercaseAndRemovesSeparators() throws Exception {
        assertEquals("", callCanonical(null), "null should normalize to empty string");
        assertEquals("indierock", callCanonical(" Indie Rock "), "spaces + case removed");
        assertEquals("drumandbass", callCanonical("drum_and-bass"), "underscores/hyphens removed");
        assertEquals("synthpop", callCanonical("Synth Pop"), "spaces removed and lowercased");
    }
}
