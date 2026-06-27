package ru.mogcommunity.rbrproject.data.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class SnapshotTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        Snapshot snapshot = new Snapshot("s1", "p1", "Title", 5000L,
                "desc", true, "NullPointerException", "http://img.jpg", "plan");

        assertEquals("s1", snapshot.getId());
        assertEquals("p1", snapshot.getProjectId());
        assertEquals("Title", snapshot.getTitle());
        assertEquals(5000L, snapshot.getTimestamp());
        assertEquals("desc", snapshot.getDescription());
        assertTrue(snapshot.isHasError());
        assertEquals("NullPointerException", snapshot.getErrorLog());
        assertEquals("http://img.jpg", snapshot.getImageUrl());
        assertEquals("plan", snapshot.getAiAnalysisPlan());
    }

    @Test
    public void defaultConstructor_fieldsAreNull() {
        Snapshot snapshot = new Snapshot();

        assertNull(snapshot.getTitle());
        assertNull(snapshot.getDescription());
        assertNull(snapshot.getErrorLog());
        assertNull(snapshot.getImageUrl());
        assertNull(snapshot.getAiAnalysisPlan());
        assertNull(snapshot.getTags());
        assertNull(snapshot.getSecondaryImages());
        assertFalse(snapshot.isHasError());
    }

    @Test
    public void tagsAndSecondaryImages_settersWork() {
        Snapshot snapshot = new Snapshot("s1", "p1", "T", 0L, "", false, "", "", "");

        snapshot.setTags("sda:4, scl:5");
        snapshot.setSecondaryImages("img1.jpg,img2.jpg");

        assertEquals("sda:4, scl:5", snapshot.getTags());
        assertEquals("img1.jpg,img2.jpg", snapshot.getSecondaryImages());
    }

    @Test
    public void hasError_togglesCorrectly() {
        Snapshot snapshot = new Snapshot("s1", "p1", "T", 0L, "", false, "", "", "");

        assertFalse(snapshot.isHasError());
        snapshot.setHasError(true);
        assertTrue(snapshot.isHasError());
        snapshot.setHasError(false);
        assertFalse(snapshot.isHasError());
    }

    @Test
    public void aiAnalysisPlan_updatesCorrectly() {
        Snapshot snapshot = new Snapshot("s1", "p1", "T", 0L, "", false, "", "", "");

        assertNotNull(snapshot.getAiAnalysisPlan());
        assertTrue(snapshot.getAiAnalysisPlan().isEmpty());

        snapshot.setAiAnalysisPlan("Step 1: check wiring\nStep 2: measure voltage");
        assertEquals("Step 1: check wiring\nStep 2: measure voltage", snapshot.getAiAnalysisPlan());
    }
}
