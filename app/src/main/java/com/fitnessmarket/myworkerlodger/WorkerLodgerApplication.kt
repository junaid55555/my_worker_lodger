package com.fitnessmarket.myworkerlodger

import android.app.Application
import com.fitnessmarket.myworkerlodger.data.SqliteHousingStore
import com.fitnessmarket.myworkerlodger.domain.Housing
import java.time.LocalDate

/** Builds the single [Housing] entry point the screens share. */
class WorkerLodgerApplication : Application() {
    val housing: Housing by lazy {
        Housing(store = SqliteHousingStore(this), today = { LocalDate.now() })
    }
}
