package com.timomcgrath.rtp

enum class Permissions(val permission: String) {
    SELF("rtp.self"),
    OTHER("rtp.other"),
    RELOAD("rtp.reload"),
}