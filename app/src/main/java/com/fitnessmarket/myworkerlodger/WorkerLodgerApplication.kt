package com.fitnessmarket.myworkerlodger

import android.app.Application
import com.fitnessmarket.myworkerlodger.data.SqliteHousingStore
import com.fitnessmarket.myworkerlodger.domain.Housing
import com.fitnessmarket.myworkerlodger.domain.Today
import java.time.LocalDate

/** Builds the single [Housing] entry point the screens share, and the clock it uses. */
class WorkerLodgerApplication : Application() {
    val today: Today = Today { LocalDate.now() }

    val housing: Housing by lazy {
        Housing(store = SqliteHousingStore(this), today = today)
    }
}
