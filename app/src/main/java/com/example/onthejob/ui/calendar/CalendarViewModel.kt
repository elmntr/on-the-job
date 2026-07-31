package com.example.onthejob.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onthejob.data.entry.Entry
import com.example.onthejob.data.entry.EntryRepository
import com.example.onthejob.data.entry.effectiveLocalDate
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val entryRepository: EntryRepository = EntryRepository(),
) : ViewModel() {

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    // Reuses EntryRepository.getEntries() as-is — same live listener LogFeed
    // uses, no new Firestore query. Fine at current scale (single user,
    // Spark tier, no date-range filter needed yet); revisit only if entry
    // volume ever makes fetching full history wasteful.
    val entries: StateFlow<List<Entry>> =
        (userId?.let { entryRepository.getEntries(it) } ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val entriesByDate: StateFlow<Map<LocalDate, List<Entry>>> =
        entries.map { list -> list.filter { it.effectiveLocalDate != null }.groupBy { it.effectiveLocalDate!! } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val monthGrid: StateFlow<List<LocalDate?>> =
        currentMonth.map { buildMonthGrid(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildMonthGrid(YearMonth.now()))

    val selectedDayEntries: StateFlow<List<Entry>> =
        combine(selectedDate, entriesByDate) { date, byDate -> byDate[date].orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Placeholder monthly summary ("N days logged / M days elapsed this
    // month"). Mirrors LogFeedViewModel's PLACEHOLDER_REQUIRED_HOURS pattern:
    // the mockup shows "18 / 22 days" but never defines the denominator
    // (weekdays only? working days? all calendar days?). Using "all calendar
    // days elapsed so far this month" as the simplest well-defined stand-in —
    // revisit once there's a real attendance-day definition.
    val monthSummary: StateFlow<Pair<Int, Int>> =
        combine(currentMonth, entriesByDate) { month, byDate ->
            val today = LocalDate.now()
            val daysElapsed = when {
                month == YearMonth.from(today) -> today.dayOfMonth
                month.isBefore(YearMonth.from(today)) -> month.lengthOfMonth()
                else -> 0
            }
            val daysLogged = (1..daysElapsed).count { day -> byDate.containsKey(month.atDay(day)) }
            daysLogged to daysElapsed
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    fun selectDate(date: LocalDate) { _selectedDate.value = date }
    fun goToPreviousMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }
    fun goToNextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
}

/** Sunday-first month grid, padded with nulls to full weeks. */
private fun buildMonthGrid(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    val firstDow = firstOfMonth.dayOfWeek.value % 7 // ISO Sunday=7 -> 0, Monday=1 -> 1, ...
    val daysInMonth = yearMonth.lengthOfMonth()
    val leading = List(firstDow) { null }
    val days = (1..daysInMonth).map { yearMonth.atDay(it) }
    val total = leading.size + days.size
    val trailing = List((7 - total % 7) % 7) { null }
    return leading + days + trailing
}