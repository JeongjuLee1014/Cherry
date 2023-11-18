package com.example.cherry.message

data class Message(
    val message: String?,
    val sendId: String?
) {
    constructor(): this("", "")
}
