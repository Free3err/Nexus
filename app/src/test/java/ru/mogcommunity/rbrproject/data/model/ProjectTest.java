package ru.mogcommunity.rbrproject.data.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProjectTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        Project project = new Project("id1", "Name", "Description", 12345L);

        assertEquals("id1", project.getId());
        assertEquals("Name", project.getName());
        assertEquals("Description", project.getDescription());
        assertEquals(12345L, project.getCreatedAt());
        assertEquals(0, project.getSnapshotsCount());
        assertEquals("", project.getConfigEnv());
        assertEquals("", project.getChatSummary());
        assertEquals("", project.getLastSummarizedMessageId());
    }

    @Test
    public void defaultConstructor_fieldsAreNull() {
        Project project = new Project();

        assertNull(project.getName());
        assertNull(project.getDescription());
        assertNull(project.getConfigEnv());
        assertNull(project.getChatSummary());
        assertNull(project.getLastSummarizedMessageId());
        assertEquals(0, project.getSnapshotsCount());
        assertEquals(0L, project.getCreatedAt());
    }

    @Test
    public void settersAndGetters_workCorrectly() {
        Project project = new Project();

        project.setId("test_id");
        project.setName("New Name");
        project.setDescription("New Desc");
        project.setCreatedAt(99999L);
        project.setSnapshotsCount(5);
        project.setConfigEnv("env=production");
        project.setChatSummary("summary text");
        project.setLastSummarizedMessageId("msg_42");

        assertEquals("test_id", project.getId());
        assertEquals("New Name", project.getName());
        assertEquals("New Desc", project.getDescription());
        assertEquals(99999L, project.getCreatedAt());
        assertEquals(5, project.getSnapshotsCount());
        assertEquals("env=production", project.getConfigEnv());
        assertEquals("summary text", project.getChatSummary());
        assertEquals("msg_42", project.getLastSummarizedMessageId());
    }

    @Test
    public void chatSummary_defaultsToEmptyInFullConstructor() {
        Project project = new Project("id", "n", "d", 0L);

        assertNotNull(project.getChatSummary());
        assertTrue(project.getChatSummary().isEmpty());
    }

    @Test
    public void lastSummarizedMessageId_defaultsToEmptyInFullConstructor() {
        Project project = new Project("id", "n", "d", 0L);

        assertNotNull(project.getLastSummarizedMessageId());
        assertTrue(project.getLastSummarizedMessageId().isEmpty());
    }
}
