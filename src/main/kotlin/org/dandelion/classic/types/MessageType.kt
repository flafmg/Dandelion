package org.dandelion.classic.types

enum class MessageType(val code: Int) {
    Chat(0),
    Status1(1),
    Status2(2),
    Status3(3),
    BottomRight1(11),
    BottomRight2(12),
    BottomRight3(13),
    Announcement(100);

    companion object {
        fun fromCode(code: Int): MessageType? = values().find { it.code == code }
    }
}
