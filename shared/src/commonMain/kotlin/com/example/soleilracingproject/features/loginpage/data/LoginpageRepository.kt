package com.example.soleilracingproject.features.loginpage.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface LoginAuthentication {
    val currentUserId: String?
    val userIdChanges: Flow<String?>
    suspend fun login(email: String, pass: String): FirebaseUser?
    suspend fun register(email: String, pass: String): FirebaseUser?
    suspend fun logout()
}

class LoginpageRepository : LoginAuthentication {

    private val auth: FirebaseAuth
        get() = Firebase.auth

    override val userIdChanges: Flow<String?> = auth.authStateChanged.map { it?.uid }

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    override suspend fun login(email: String, pass: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, pass)
        return result.user
    }

    override suspend fun register(email: String, pass: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, pass)
        return result.user
    }

    override suspend fun logout() {
        auth.signOut()
    }
}
