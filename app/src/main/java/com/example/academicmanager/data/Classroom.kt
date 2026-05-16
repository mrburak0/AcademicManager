package com.example.academicmanager.data

// Sınıf tipi sabitleri — Firestore string olarak saklar
object ClassroomType {
    const val LECTURE      = "LECTURE"       // Derslik
    const val LAB          = "LAB"           // Laboratuvar
    const val COMPUTER_LAB = "COMPUTER_LAB"  // Bilgisayar Laboratuvarı

    fun displayName(type: String): String = when (type) {
        LECTURE      -> "Derslik"
        LAB          -> "Laboratuvar"
        COMPUTER_LAB -> "Bilgisayar Lab"
        else         -> type
    }

    val all = listOf(LECTURE, LAB, COMPUTER_LAB)
}

data class Classroom(
    val id: String = "",
    val name: String = "",
    val capacity: Int = 0,
    val classroomType: String = ClassroomType.LECTURE
)
