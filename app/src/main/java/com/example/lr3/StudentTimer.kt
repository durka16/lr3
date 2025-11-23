package com.example.lr3

data class StudentTimer (
    val id: Int,
    val name: String,
    var currentTimeMillis:Long = 0L,
    var isRunning: Boolean = false)