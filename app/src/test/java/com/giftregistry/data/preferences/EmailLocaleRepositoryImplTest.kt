package com.giftregistry.data.preferences

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailLocaleRepositoryImplTest {

    private fun buildDocumentRef(
        snapshot: DocumentSnapshot,
        setTask: com.google.android.gms.tasks.Task<Void> = Tasks.forResult(null),
    ): Pair<FirebaseFirestore, DocumentReference> {
        val firestore = mockk<FirebaseFirestore>()
        val usersCol = mockk<CollectionReference>()
        val userDoc = mockk<DocumentReference>()
        every { firestore.collection("users") } returns usersCol
        every { usersCol.document(any()) } returns userDoc
        every { userDoc.get() } returns Tasks.forResult(snapshot)
        every { userDoc.set(any(), any<SetOptions>()) } returns setTask
        return Pair(firestore, userDoc)
    }

    private fun signedInAuth(uid: String): FirebaseAuth {
        val auth = mockk<FirebaseAuth>()
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        every { user.uid } returns uid
        return auth
    }

    private fun signedOutAuth(): FirebaseAuth {
        val auth = mockk<FirebaseAuth>()
        every { auth.currentUser } returns null
        return auth
    }

    @Test
    fun `getEmailLocale returns en when field is en`() = runTest {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.getString("preferredLocale") } returns "en"
        val (firestore, _) = buildDocumentRef(snap)
        val auth = signedInAuth("u1")

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        assertEquals("en", repo.getEmailLocale())
    }

    @Test
    fun `getEmailLocale returns ro when field is ro`() = runTest {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.getString("preferredLocale") } returns "ro"
        val (firestore, _) = buildDocumentRef(snap)
        val auth = signedInAuth("u1")

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        assertEquals("ro", repo.getEmailLocale())
    }

    @Test
    fun `getEmailLocale returns null when field missing`() = runTest {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.getString("preferredLocale") } returns null
        val (firestore, _) = buildDocumentRef(snap)
        val auth = signedInAuth("u1")

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        assertNull(repo.getEmailLocale())
    }

    @Test
    fun `getEmailLocale returns null when not signed in`() = runTest {
        val auth = signedOutAuth()
        val firestore = mockk<FirebaseFirestore>()

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        val result = repo.getEmailLocale()

        assertNull(result)
        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun `getEmailLocale returns null for unsupported value`() = runTest {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.getString("preferredLocale") } returns "fr"
        val (firestore, _) = buildDocumentRef(snap)
        val auth = signedInAuth("u1")

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        assertNull(repo.getEmailLocale())
    }

    @Test
    fun `setEmailLocale ro writes doc`() = runTest {
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        val (firestore, userDoc) = buildDocumentRef(snap)
        val auth = signedInAuth("u1")

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        val result = repo.setEmailLocale("ro")

        assertTrue(result.isSuccess)
        verify(exactly = 1) { userDoc.set(mapOf("preferredLocale" to "ro"), any<SetOptions>()) }
    }

    @Test
    fun `setEmailLocale fr returns failure with IllegalArgumentException`() = runTest {
        val auth = signedInAuth("u1")
        val firestore = mockk<FirebaseFirestore>()

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        val result = repo.setEmailLocale("fr")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `setEmailLocale when not signed in returns failure`() = runTest {
        val auth = signedOutAuth()
        val firestore = mockk<FirebaseFirestore>()

        val repo = EmailLocaleRepositoryImpl(auth, firestore)
        val result = repo.setEmailLocale("en")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
