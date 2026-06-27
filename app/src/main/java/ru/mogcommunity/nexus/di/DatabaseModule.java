package ru.mogcommunity.rbrproject.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import ru.mogcommunity.rbrproject.data.PreferenceManager;
import ru.mogcommunity.rbrproject.data.local.AppDatabase;
import ru.mogcommunity.rbrproject.data.local.ProjectDao;
import ru.mogcommunity.rbrproject.data.local.SnapshotDao;
import ru.mogcommunity.rbrproject.data.local.ChatMessageDao;
import ru.mogcommunity.rbrproject.data.remote.FirebaseManager;
import ru.mogcommunity.rbrproject.data.remote.GeminiClient;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return AppDatabase.getDatabase(context);
    }

    @Provides
    @Singleton
    public static ProjectDao provideProjectDao(AppDatabase database) {
        return database.projectDao();
    }

    @Provides
    @Singleton
    public static SnapshotDao provideSnapshotDao(AppDatabase database) {
        return database.snapshotDao();
    }

    @Provides
    @Singleton
    public static ChatMessageDao provideChatMessageDao(AppDatabase database) {
        return database.chatMessageDao();
    }

    @Provides
    @Singleton
    public static FirebaseManager provideFirebaseManager() {
        return FirebaseManager.getInstance();
    }

    @Provides
    @Singleton
    public static PreferenceManager providePreferenceManager(@ApplicationContext Context context) {
        return PreferenceManager.getInstance(context);
    }

    @Provides
    @Singleton
    public static GeminiClient provideGeminiClient() {
        return new GeminiClient();
    }
}
