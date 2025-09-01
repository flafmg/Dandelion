package org.dandelion.classic.util

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

class DatabaseManager(private val dbPath: String) {
    private var connection: Connection? = null

    fun connect(): Connection {
        if (connection?.isClosed != false) {
            val dbFile = File(dbPath)
            if (!dbFile.exists()) {
                dbFile.parentFile?.mkdirs()
                dbFile.createNewFile()
            }
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        }
        return connection!!
    }

    fun executeUpdate(sql: String): Int {
        return connect().createStatement().executeUpdate(sql)
    }

    fun executeQuery(sql: String): ResultSet {
        return connect().createStatement().executeQuery(sql)
    }

    fun prepareStatement(sql: String): PreparedStatement {
        return connect().prepareStatement(sql)
    }

    fun close() {
        connection?.close()
    }
}