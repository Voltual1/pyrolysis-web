package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any?
): DataStore<UserCredentials> {
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = serializer,
            producePath = {
                FileKit.filesDir.file.resolve("user_credentials.preferences_pb").absolutePath.toPath()
            }
        )
    )
}

actual fun createPreferenceDataStore(
    fileName: String,
    context: Any?
): DataStore<Preferences> {
    return PreferenceDataStoreFactory.create(
        produceFile = {
            File(FileKit.filesDir.file, "datastore/$fileName")
        }
    )
}