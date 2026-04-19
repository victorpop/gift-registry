package com.giftregistry.data.fcm

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("UNCHECKED_CAST")
class FcmTokenRepositoryImplTest {

    private fun buildRepo(
        uid: String? = "u1",
        setTask: com.google.android.gms.tasks.Task<Void> = Tasks.forResult(null),
        deleteTask: com.google.android.gms.tasks.Task<Void> = Tasks.forResult(null),
    ): Triple<FcmTokenRepositoryImpl, FirebaseAuth, FirebaseFirestore> {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val tokensCol = mockk<CollectionReference>()
        val tokenDoc = mockk<DocumentReference>()

        if (uid != null) {
            val firebaseUser = mockk<FirebaseUser>()
            every { auth.currentUser } returns firebaseUser
            every { firebaseUser.uid } returns uid
        } else {
            every { auth.currentUser } returns null
        }

        every { firestore.collection("users") } returns usersCol
        every { usersCol.document(any()) } returns userDoc
        every { userDoc.collection("fcmTokens") } returns tokensCol
        every { tokensCol.document(any()) } returns tokenDoc
        every { tokenDoc.set(any(), any<SetOptions>()) } returns setTask
        every { tokenDoc.delete() } returns deleteTask

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        return Triple(repo, auth, firestore)
    }

    @Test
    fun `registerToken writes doc with correct path and merge`() = runTest {
        val mapSlot = slot<Map<String, Any?>>()
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val tokensCol = mockk<CollectionReference>()
        val tokenDoc = mockk<DocumentReference>()

        val firebaseUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "u1"
        every { firestore.collection("users") } returns usersCol
        every { usersCol.document("u1") } returns userDoc
        every { userDoc.collection("fcmTokens") } returns tokensCol
        every { tokensCol.document("tok1") } returns tokenDoc
        every { tokenDoc.set(capture(mapSlot), any<SetOptions>()) } returns Tasks.forResult(null)

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        val result = repo.registerToken("tok1")

        assertTrue(result.isSuccess)
        val captured = mapSlot.captured
        assertEquals("tok1", captured["token"])
        assertEquals("android", captured["platform"])
        assertTrue(captured.containsKey("createdAt"))
        assertTrue(captured.containsKey("lastSeenAt"))
    }

    @Test
    fun `registerToken without signed-in user is a no-op`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        every { auth.currentUser } returns null

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        val result = repo.registerToken("tok1")

        assertTrue(result.isSuccess)
        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun `registerToken with blank token is a no-op`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val firebaseUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "u1"

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        val result = repo.registerToken("")

        assertTrue(result.isSuccess)
        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun `registerToken wraps Firebase exception in Result_failure`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val tokensCol = mockk<CollectionReference>()
        val tokenDoc = mockk<DocumentReference>()

        val firebaseUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "u1"
        every { firestore.collection("users") } returns usersCol
        every { usersCol.document(any()) } returns userDoc
        every { userDoc.collection("fcmTokens") } returns tokensCol
        every { tokensCol.document(any()) } returns tokenDoc
        every { tokenDoc.set(any(), any<SetOptions>()) } returns Tasks.forException(RuntimeException("boom"))

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        val result = repo.registerToken("tok1")

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `deleteToken without signed-in user is a no-op`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        every { auth.currentUser } returns null

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        val result = repo.deleteToken("tok1")

        assertTrue(result.isSuccess)
        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun `deleteToken with signed-in user deletes at correct path`() = runTest {
        val auth = mockk<FirebaseAuth>()
        val firestore = mockk<FirebaseFirestore>()
        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        val tokensCol = mockk<CollectionReference>()
        val tokenDoc = mockk<DocumentReference>()

        val firebaseUser = mockk<FirebaseUser>()
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "u1"
        every { firestore.collection("users") } returns usersCol
        every { usersCol.document("u1") } returns userDoc
        every { userDoc.collection("fcmTokens") } returns tokensCol
        every { tokensCol.document("tok1") } returns tokenDoc
        every { tokenDoc.delete() } returns Tasks.forResult(null)

        val repo = FcmTokenRepositoryImpl(auth, firestore)
        val result = repo.deleteToken("tok1")

        assertTrue(result.isSuccess)
        verify(exactly = 1) { tokenDoc.delete() }
    }
}
