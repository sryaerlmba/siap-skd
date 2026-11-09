package com.belajar.cpns.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        QuestionEntity::class,
        SessionEntity::class,
        AnswerEntity::class,
        SessionResultEntity::class,
        TagStatEntity::class,
        ReviewQueueEntity::class,
        MetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SkdDatabase : RoomDatabase() {
    abstract fun questions(): QuestionDao
    abstract fun sessions(): SessionDao
    abstract fun answers(): AnswerDao
    abstract fun results(): ResultDao
    abstract fun tagStats(): TagStatDao
    abstract fun reviewQueue(): ReviewQueueDao
    abstract fun meta(): MetaDao

    companion object {
        fun build(context: Context): SkdDatabase =
            Room.databaseBuilder(context, SkdDatabase::class.java, "siap_skd.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
