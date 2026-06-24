package ru.mogcommunity.rbr_project.data.repository;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.data.local.ChatMessageDao;
import ru.mogcommunity.rbr_project.data.local.ProjectDao;
import ru.mogcommunity.rbr_project.data.local.SnapshotDao;
import ru.mogcommunity.rbr_project.data.model.ChatMessage;
import ru.mogcommunity.rbr_project.data.model.Project;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.data.remote.FirebaseManager;

public class ProjectRepositoryTest {

    @Mock private ProjectDao projectDao;
    @Mock private SnapshotDao snapshotDao;
    @Mock private ChatMessageDao chatMessageDao;
    @Mock private FirebaseManager firebaseManager;
    @Mock private PreferenceManager preferenceManager;
    @Mock private android.content.Context context;

    private ProjectRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new ProjectRepository(
                projectDao, snapshotDao, chatMessageDao,
                firebaseManager, preferenceManager, context
        );
    }

    @Test
    public void getAllProjects_returnsLiveData() {
        MutableLiveData<List<Project>> expected = new MutableLiveData<>();
        when(projectDao.getAllProjects()).thenReturn(expected);

        LiveData<List<Project>> result = repository.getAllProjects();

        assertSame(expected, result);
        verify(projectDao).getAllProjects();
    }

    @Test
    public void getProjectByIdSync_returnsProject() {
        Project project = new Project("p1", "Test", "Desc", 1000L);
        when(projectDao.getProjectById("p1")).thenReturn(project);

        Project result = repository.getProjectByIdSync("p1");

        assertNotNull(result);
        assertEquals("p1", result.getId());
        assertEquals("Test", result.getName());
    }

    @Test
    public void getProjectByIdSync_returnsNullForMissingId() {
        when(projectDao.getProjectById("missing")).thenReturn(null);

        Project result = repository.getProjectByIdSync("missing");

        assertNull(result);
    }

    @Test
    public void getSnapshotsForProject_returnsLiveData() {
        MutableLiveData<List<Snapshot>> expected = new MutableLiveData<>();
        when(snapshotDao.getSnapshotsForProject("p1")).thenReturn(expected);

        LiveData<List<Snapshot>> result = repository.getSnapshotsForProject("p1");

        assertSame(expected, result);
    }

    @Test
    public void getSnapshotsForProjectSync_returnsList() {
        Snapshot s1 = new Snapshot("s1", "p1", "Snap1", 100L, "desc", false, "", "", "");
        Snapshot s2 = new Snapshot("s2", "p1", "Snap2", 200L, "desc2", true, "error", "", "");
        when(snapshotDao.getSnapshotsForProjectSync("p1")).thenReturn(Arrays.asList(s1, s2));

        List<Snapshot> result = repository.getSnapshotsForProjectSync("p1");

        assertEquals(2, result.size());
        assertEquals("s1", result.get(0).getId());
        assertFalse(result.get(0).isHasError());
        assertTrue(result.get(1).isHasError());
    }

    @Test
    public void getChatMessagesForProject_returnsLiveData() {
        MutableLiveData<List<ChatMessage>> expected = new MutableLiveData<>();
        when(chatMessageDao.getMessagesForProject("p1")).thenReturn(expected);

        LiveData<List<ChatMessage>> result = repository.getChatMessagesForProject("p1");

        assertSame(expected, result);
    }

    @Test
    public void getChatMessagesForProjectSync_returnsList() {
        ChatMessage m1 = new ChatMessage("m1", "p1", "user", "hello", 100L);
        ChatMessage m2 = new ChatMessage("m2", "p1", "ai", "hi there", 200L);
        when(chatMessageDao.getMessagesForProjectSync("p1")).thenReturn(Arrays.asList(m1, m2));

        List<ChatMessage> result = repository.getChatMessagesForProjectSync("p1");

        assertEquals(2, result.size());
        assertEquals("user", result.get(0).getSender());
        assertEquals("ai", result.get(1).getSender());
    }

    @Test
    public void getLastSuccessfulSnapshot_returnsSnapshot() {
        Snapshot s = new Snapshot("s1", "p1", "OK", 100L, "desc", false, "", "", "plan");
        when(snapshotDao.getLastSuccessfulSnapshot("p1")).thenReturn(s);

        Snapshot result = repository.getLastSuccessfulSnapshot("p1");

        assertNotNull(result);
        assertEquals("s1", result.getId());
        assertFalse(result.isHasError());
    }

    @Test
    public void getLastSuccessfulSnapshot_returnsNullWhenNone() {
        when(snapshotDao.getLastSuccessfulSnapshot("p1")).thenReturn(null);

        Snapshot result = repository.getLastSuccessfulSnapshot("p1");

        assertNull(result);
    }

    @Test
    public void getAllGallerySnapshots_returnsLiveData() {
        MutableLiveData<List<Snapshot>> expected = new MutableLiveData<>();
        when(snapshotDao.getAllSnapshotsWithImages()).thenReturn(expected);

        LiveData<List<Snapshot>> result = repository.getAllSnapshotsWithImages();

        assertSame(expected, result);
    }
}
