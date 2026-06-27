package ru.mogcommunity.rbrproject.data.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChatMessageTest {

    @Test
    public void constructor_setsFieldsCorrectly() {
        ChatMessage msg = new ChatMessage("m1", "p1", "user", "hello world", 12345L);

        assertEquals("m1", msg.getId());
        assertEquals("p1", msg.getProjectId());
        assertEquals("user", msg.getSender());
        assertEquals("hello world", msg.getText());
        assertEquals(12345L, msg.getTimestamp());
    }

    @Test
    public void defaultConstructor_fieldsAreNull() {
        ChatMessage msg = new ChatMessage();

        assertNull(msg.getSender());
        assertNull(msg.getText());
        assertEquals(0L, msg.getTimestamp());
    }

    @Test
    public void settersAndGetters_workCorrectly() {
        ChatMessage msg = new ChatMessage();

        msg.setId("id1");
        msg.setProjectId("proj1");
        msg.setSender("ai");
        msg.setText("response text");
        msg.setTimestamp(99999L);

        assertEquals("id1", msg.getId());
        assertEquals("proj1", msg.getProjectId());
        assertEquals("ai", msg.getSender());
        assertEquals("response text", msg.getText());
        assertEquals(99999L, msg.getTimestamp());
    }

    @Test
    public void senderTypes_userAndAi() {
        ChatMessage userMsg = new ChatMessage("m1", "p1", "user", "question", 100L);
        ChatMessage aiMsg = new ChatMessage("m2", "p1", "ai", "answer", 200L);

        assertEquals("user", userMsg.getSender());
        assertEquals("ai", aiMsg.getSender());
    }
}
