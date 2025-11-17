package ru.chtcholeg.aichat.core

enum class ResponseFormat(val idInDatabase: String) {
    PLAIN_TEXT("plain-text"),
    JSON("json"),
    XML("xml"),
    ;
}