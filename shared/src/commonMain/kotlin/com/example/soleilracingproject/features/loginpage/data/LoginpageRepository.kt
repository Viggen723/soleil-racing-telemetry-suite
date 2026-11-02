package com.example.soleilracingproject.features.loginpage.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoginpageRepository {

    private val auth: FirebaseAuth
        get() = Firebase.auth

    val currentUserFlow: Flow<FirebaseUser?> = auth.authStateChanged

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, pass: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email, pass)
        return result.user
    }

    suspend fun register(email: String, pass: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email, pass)
        return result.user
    }

    suspend fun logout() {
        auth.signOut()
    }
}