from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

OUT = "/home/kevs/StudioProjects/FitPulse/docs/Final Project_FitPulse_KevinNoh_KennyPeralta.docx"

doc = Document()

# Page + base style
for section in doc.sections:
    section.top_margin = Inches(0.85)
    section.bottom_margin = Inches(0.85)
    section.left_margin = Inches(0.9)
    section.right_margin = Inches(0.9)

style = doc.styles["Normal"]
style.font.name = "Calibri"
style.font.size = Pt(11)
style.paragraph_format.space_after = Pt(6)

GREEN = RGBColor(0x00, 0x87, 0x5A)
DARK = RGBColor(0x1F, 0x2A, 0x24)


def h(text, level=1):
    p = doc.add_heading(text, level=level)
    for run in p.runs:
        run.font.color.rgb = GREEN if level == 1 else DARK
    return p


def para(text="", bold=False, italic=False, size=None, align=None, color=None):
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.bold = bold
    run.italic = italic
    if size:
        run.font.size = Pt(size)
    if color:
        run.font.color.rgb = color
    if align is not None:
        p.alignment = align
    return p


def bullet(text, bold_prefix=None):
    p = doc.add_paragraph(style="List Bullet")
    if bold_prefix:
        r = p.add_run(bold_prefix)
        r.bold = True
    p.add_run(text)
    return p


def numbered(text):
    p = doc.add_paragraph(style="List Number")
    p.add_run(text)
    return p


def shade(cell, hex_color):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tc_pr.append(shd)


def code(text):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(8)
    p.paragraph_format.left_indent = Inches(0.15)
    run = p.add_run(text)
    run.font.name = "Consolas"
    run.font.size = Pt(8.5)
    pPr = p._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), "F2F6F3")
    pPr.append(shd)
    return p


def table(headers, rows, widths=None, header_fill="00875A"):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr = t.rows[0].cells
    for i, header in enumerate(headers):
        hdr[i].text = ""
        run = hdr[i].paragraphs[0].add_run(header)
        run.bold = True
        run.font.size = Pt(10)
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        shade(hdr[i], header_fill)
    for row in rows:
        cells = t.add_row().cells
        for i, value in enumerate(row):
            cells[i].text = ""
            run = cells[i].paragraphs[0].add_run(str(value))
            run.font.size = Pt(9.5)
    if widths:
        for row in t.rows:
            for i, width in enumerate(widths):
                row.cells[i].width = Inches(width)
    doc.add_paragraph()
    return t


def screenshot_box(caption, hint):
    t = doc.add_table(rows=1, cols=1)
    t.style = "Table Grid"
    cell = t.rows[0].cells[0]
    cell.text = ""
    p = cell.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(f"\n[ INSERT SCREENSHOT HERE ]\n{caption}\n{hint}\n")
    r.italic = True
    r.font.color.rgb = RGBColor(0x66, 0x66, 0x66)
    shade(cell, "F7F7F7")
    doc.add_paragraph()


# ---------------------------------------------------------------- cover
p = para("SMART DEVICES DEVELOPMENT", bold=True, size=12, align=WD_ALIGN_PARAGRAPH.CENTER, color=GREEN)
p = para("Practice 5 · Wear OS + Phone Project", size=11, align=WD_ALIGN_PARAGRAPH.CENTER)
doc.add_paragraph()
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("FitPulse")
r.bold = True
r.font.size = Pt(34)
r.font.color.rgb = GREEN
p = para("A Multi-Device Fitness Tracking Application", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
para("Two-way workout control with MessageClient and live goal synchronization with DataClient", italic=True, align=WD_ALIGN_PARAGRAPH.CENTER)
doc.add_paragraph()
table(
    ["Field", "Value"],
    [
        ["Students", "Kevin Noh · Kenny Peralta"],
        ["Group", "TI91"],
        ["Course", "Smart Devices Development"],
        ["Deliverable", "Functional source code + demo video + project report"],
        ["Devices", "Android Phone (API 36) + Wear OS Watch (API 36.1)"],
        ["Stack", "Kotlin · Jetpack Compose · Wear Compose Material 3 · Play Services Wearable 20.0.1"],
        ["Repository", "https://github.com/KevSyndrome/wearOS-smartDevices.git"],
    ],
    widths=[1.5, 5.0],
)
doc.add_page_break()

# ---------------------------------------------------------------- objective
h("Objective", 1)
para(
    "To implement a complete, real-world multi-device communication loop between an Android phone "
    "and a Wear OS watch using the Google Wearable Data Layer. The project implements two related "
    "features selected from the FitPulse Functional Requirements Document: one command/event feature "
    "built on MessageClient (Start/Stop Workout) and one configuration-synchronization feature built "
    "on DataClient (Daily Step Goal)."
)

h("Prerequisites", 1)
for item in [
    "Completed: Multi-Device Application Functional Requirements Document (FitPulse, Kevin Noh & Kenny Peralta).",
    "Completed: Designing Multi-Device Interfaces assignment.",
    "Completed: Practice 4 — Two-Way Reset Signal (MessageClient).",
    "Android Studio with a paired Phone emulator (API 36) and Wear OS emulator (API 36.1).",
    "Both modules share the applicationId com.example.fitpulse so the Data Layer can discover the peer app.",
]:
    bullet(item)

# ---------------------------------------------------------------- task
h("The Task: FitPulse", 1)

h("Goal", 2)
para(
    "Build the FitPulse phone + watch experience where the watch is the fast, glanceable control "
    "surface during exercise and the phone is the authoritative hub that stores history and owns the "
    "user's configuration. Every state change must travel through the Wearable Data Layer and be "
    "reflected on the other device."
)

h("Details", 2)
para(
    "FitPulse is a fitness-tracking ecosystem originally specified for phone, watch and smart display. "
    "This project implements the phone ↔ watch slice, using the two features that explicitly require "
    "device-to-device communication in the Feature Adaptation Table:"
)
table(
    ["#", "Feature", "Wearable API", "Direction", "What the user sees"],
    [
        ["1", "Start / Stop Workout", "MessageClient", "Watch → Phone → Watch",
         "One-tap start/stop on the wrist. The phone acknowledges, logs the session, computes duration and calories, and replies with a summary."],
        ["2", "Daily Step Goal", "DataClient", "Phone → Watch",
         "The phone sets the goal, persists it, and replicates it as a data item. The watch shows the new goal at a glance."],
    ],
    widths=[0.3, 1.25, 1.0, 1.25, 2.7],
)
para(
    "Both features were deliberately chosen because they exercise the two different Wearable APIs "
    "required by the assignment, and because they are complementary: the command feature represents "
    "real-time action, while the configuration feature represents durable, replicated state."
)

h("Design principles applied", 2)
for item in [
    "Glanceability: the watch shows only the goal, the elapsed time and one primary button.",
    "Phone as authoritative hub: the phone owns the start timestamp, duration, calories, history and goal value.",
    "Watch as immediate control surface: starting a workout takes exactly one tap.",
    "Immediate feedback: every command produces an acknowledgement that updates the watch status text.",
    "Consistent identity: both screens use the same FitPulse green, typography hierarchy and student names.",
]:
    bullet(item)

# ---------------------------------------------------------------- communication plan
h("Step 0 — Communication Plan", 1)
para(
    "Before writing code, both modules received the exact same constants and data models. This is "
    "critical: if the path strings differ by even one character, the message is silently ignored."
)

h("0.1 Feature 1 — MessageClient paths (commands / events)", 2)
table(
    ["Constant", "Value", "Direction", "Payload"],
    [
        ["PATH_START_WORKOUT", "/fitpulse/workout/start", "Watch → Phone", "WorkoutCommand JSON"],
        ["PATH_STOP_WORKOUT", "/fitpulse/workout/stop", "Watch → Phone", "WorkoutCommand JSON"],
        ["PATH_WORKOUT_ACK", "/fitpulse/workout/ack", "Phone → Watch", "WorkoutAck JSON"],
    ],
    widths=[1.7, 2.05, 1.2, 1.5],
)
para(
    "MessageClient is fire-and-forget, so the acknowledgement path is what turns a one-way command into "
    "the complete two-way loop required by the assignment."
)

h("0.2 Feature 2 — DataClient path and keys (configuration sync)", 2)
table(
    ["Constant", "Value", "Type", "Purpose"],
    [
        ["PATH_DAILY_GOAL", "/fitpulse/config/daily_goal", "Data item path", "Replicated configuration item"],
        ["KEY_DAILY_GOAL", "daily_step_goal", "Int", "The authoritative goal in steps"],
        ["KEY_GOAL_UPDATED_AT", "goal_updated_at", "Long", "Epoch millis of the last change"],
    ],
    widths=[1.7, 2.2, 1.0, 1.6],
)
para(
    "The phone is the only writer of this data item. The watch is a read-only consumer, which avoids "
    "conflicting writes and keeps the phone as the single source of truth."
)

h("0.3 Structured data models and JSON serialization", 2)
para(
    "Both modules include the same DataModels.kt file. Each model exposes toJsonBytes()/fromJsonBytes() "
    "using org.json (bundled with Android), so no extra serialization dependency is required. "
    "Deserialization is wrapped in runCatching so a malformed payload can never crash the receiver."
)
code(
"""data class WorkoutCommand(
    val type: String,          // START or STOP
    val timestamp: Long,
    val elapsedSeconds: Long = 0L,
) {
    fun toJsonBytes(): ByteArray = JSONObject().apply {
        put("type", type)
        put("timestamp", timestamp)
        put("elapsedSeconds", elapsedSeconds)
    }.toString().toByteArray(Charsets.UTF_8)

    companion object {
        fun fromJsonBytes(bytes: ByteArray?): WorkoutCommand? = runCatching {
            val json = JSONObject(String(bytes ?: return null, Charsets.UTF_8))
            WorkoutCommand(
                type = json.getString("type"),
                timestamp = json.getLong("timestamp"),
                elapsedSeconds = json.optLong("elapsedSeconds", 0L),
            )
        }.getOrNull()
    }
}"""
)
para(
    "WorkoutAck carries the result of the command (STARTED / STOPPED / ERROR), the computed duration, "
    "calories and the current goal. WorkoutSession is stored on the phone as JSON inside SharedPreferences "
    "and is never transmitted over the wire."
)

# ---------------------------------------------------------------- structure
h("Project Structure", 1)
doc.paragraphs[-1]._element.getparent().remove(doc.paragraphs[-1]._element)
code(
"""FitPulse/
├── settings.gradle.kts                 includes :mobile and :wear
├── gradle/libs.versions.toml           shared dependency catalog
├── mobile/
│   └── src/main/java/com/example/fitpulse/mobile/
│       ├── MainActivity.kt             authoritative hub + phone UI
│       ├── common/WearableConstants.kt paths and keys (Feature 1 + 2)
│       ├── common/DataModels.kt        JSON models shared with the watch
│       └── data/FitPulseRepository.kt  goal + workout history storage
└── wear/
    └── src/main/java/com/example/fitpulse/
        ├── presentation/MainActivity.kt watch client + watch UI
        ├── presentation/theme/Theme.kt
        └── common/
            ├── WearableConstants.kt
            └── DataModels.kt"""
)
para(
    "The project started from a clean Wear OS + Phone template with two modules. The wear module uses "
    "compileSdk 36.1 and minSdk 30; the phone module uses compileSdk 36 and minSdk 26."
)

# ---------------------------------------------------------------- dependencies
h("Step 1 — Dependencies", 1)
para("Both mobile/build.gradle.kts and wear/build.gradle.kts include the required libraries:")
code(
"""implementation(libs.play.services.wearable)
implementation(libs.kotlinx.coroutines.play.services)
implementation(libs.androidx.lifecycle.runtime.ktx)"""
)
para(
    "play-services-wearable provides MessageClient, DataClient and NodeClient. "
    "kotlinx-coroutines-play-services adds await(), which lets the code replace callback pyramids with "
    "linear coroutine code. lifecycle-runtime-ktx provides lifecycleScope, which automatically cancels "
    "in-flight communication work when the activity is destroyed."
)
para("Version catalog entries:", bold=True)
code(
"""[versions]
playServicesWearable = "20.0.1"
kotlinxCoroutinesPlayServices = "1.10.2"
lifecycleRuntimeKtx = "2.9.4"

[libraries]
play-services-wearable = { group = "com.google.android.gms", name = "play-services-wearable", version.ref = "playServicesWearable" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "kotlinxCoroutinesPlayServices" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }"""
)
para(
    "Both modules declare the same applicationId (com.example.fitpulse). The Data Layer maps a path "
    "to the peer application using that applicationId, so a mismatch would break discovery even if the "
    "devices are paired."
)

# ---------------------------------------------------------------- step 2 constants
h("Step 2 — Shared constants", 1)
para(
    "WearableConstants.kt is a single object that groups the two features. Keeping every path and key "
    "in one place is what allows onMessageReceived and onDataChanged to route events without collisions."
)
code(
"""object WearableConstants {
    // Feature 1 - MessageClient
    const val PATH_START_WORKOUT = "/fitpulse/workout/start"
    const val PATH_STOP_WORKOUT  = "/fitpulse/workout/stop"
    const val PATH_WORKOUT_ACK   = "/fitpulse/workout/ack"
    const val CMD_START = "START";  const val CMD_STOP = "STOP"
    const val ACK_STARTED = "STARTED"
    const val ACK_STOPPED = "STOPPED"
    const val ACK_ERROR = "ERROR"

    // Feature 2 - DataClient
    const val PATH_DAILY_GOAL = "/fitpulse/config/daily_goal"
    const val KEY_DAILY_GOAL = "daily_step_goal"
    const val KEY_GOAL_UPDATED_AT = "goal_updated_at"
    const val DEFAULT_DAILY_GOAL = 8_000
}"""
)

# ---------------------------------------------------------------- step 3 phone message
h("Step 3 — Phone: receiving and routing commands (Feature 1)", 1)
para(
    "The phone implements MessageClient.OnMessageReceivedListener. The when statement on the event path "
    "is the collision-avoidance mechanism required by the assignment: START and STOP are handled by "
    "separate functions even though they share one listener."
)
code(
"""class MainActivity : ComponentActivity(),
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val command = WorkoutCommand.fromJsonBytes(messageEvent.data)

        when (messageEvent.path) {
            WearableConstants.PATH_START_WORKOUT -> {
                command ?: return sendAck(messageEvent.sourceNodeId,
                    WorkoutAck(WearableConstants.ACK_ERROR, "Malformed START payload"))
                handleStartCommand(messageEvent.sourceNodeId)
            }
            WearableConstants.PATH_STOP_WORKOUT -> {
                command ?: return sendAck(messageEvent.sourceNodeId,
                    WorkoutAck(WearableConstants.ACK_ERROR, "Malformed STOP payload"))
                handleStopCommand(messageEvent.sourceNodeId)
            }
        }
    }"""
)
para(
    "The phone is authoritative: on START it records its own System.currentTimeMillis(); on STOP it "
    "computes duration from that timestamp instead of trusting the watch. This prevents clock drift on "
    "the watch from corrupting the stored session."
)
code(
"""private fun handleStopCommand(nodeId: String) {
    if (!isWorkoutActive) {
        sendAck(nodeId, WorkoutAck(WearableConstants.ACK_ERROR,
            "There is no active workout to stop"))
        return
    }

    val now = System.currentTimeMillis()
    val durationSeconds = ((now - workoutStartMillis) / 1_000L).coerceAtLeast(1L)
    val calories = ceil(durationSeconds / 60.0 *
        WearableConstants.CALORIES_PER_MINUTE).toInt()

    val session = WorkoutSession(
        id = workoutStartMillis,
        startedAt = workoutStartMillis,
        endedAt = now,
        durationSeconds = durationSeconds,
        calories = calories,
    )
    sessions = repository.addSession(session)   // persisted on the phone
    isWorkoutActive = false

    sendAck(nodeId, WorkoutAck(
        status = WearableConstants.ACK_STOPPED,
        message = "Workout saved",
        sessionId = session.id,
        durationSeconds = durationSeconds,
        calories = calories,
        dailyGoal = dailyGoal,
    ))
}"""
)
para(
    "sendAck() uses lifecycleScope + await() so the network call never blocks the main thread and is "
    "automatically cancelled if the activity is destroyed."
)
code(
"""private fun sendAck(nodeId: String, ack: WorkoutAck) {
    lifecycleScope.launch {
        runCatching {
            messageClient.sendMessage(
                nodeId,
                WearableConstants.PATH_WORKOUT_ACK,
                ack.toJsonBytes(),
            ).await()
        }.onFailure { error ->
            statusMessage = "Could not reach the watch: ${error.message}"
        }
    }
}"""
)

# ---------------------------------------------------------------- step 4 watch message
h("Step 4 — Watch: sending commands and consuming acknowledgements (Feature 1)", 1)
para(
    "The watch discovers the paired phone with NodeClient.connectedNodes, builds one typed "
    "WorkoutCommand, and sends it to its node id on the correct path. The command path is chosen with "
    "a when on the command type."
)
code(
"""private fun sendCommand(type: String) {
    if (isSending) return

    isSending = true
    lifecycleScope.launch {
        runCatching {
            val nodes = Wearable.getNodeClient(this@MainActivity)
                .connectedNodes.await()
            check(nodes.isNotEmpty()) { "No phone connected. Pair the watch first." }

            val command = WorkoutCommand(
                type = type,
                timestamp = System.currentTimeMillis(),
                elapsedSeconds = elapsed,
            )
            val path = when (type) {
                WearableConstants.CMD_START -> WearableConstants.PATH_START_WORKOUT
                else -> WearableConstants.PATH_STOP_WORKOUT
            }

            nodes.forEach { node ->
                messageClient.sendMessage(node.id, path, command.toJsonBytes()).await()
            }
        }.onFailure { statusMessage = it.message ?: "Could not reach the phone" }
        isSending = false
    }
}"""
)
para(
    "The watch only switches to the ACTIVE state after the phone confirms it. This is the key difference "
    "between a one-way send and the required two-way loop: the UI reflects confirmed state, not optimistic "
    "state."
)
code(
"""override fun onMessageReceived(messageEvent: MessageEvent) {
    if (messageEvent.path != WearableConstants.PATH_WORKOUT_ACK) return

    val ack = WorkoutAck.fromJsonBytes(messageEvent.data) ?: return
    isSending = false

    when (ack.status) {
        WearableConstants.ACK_STARTED -> {
            workoutStartMillis = System.currentTimeMillis()
            isWorkoutActive = true
            statusMessage = "Workout active · phone is logging"
        }
        WearableConstants.ACK_STOPPED -> {
            isWorkoutActive = false
            stoppedDuration = ack.durationSeconds
            statusMessage = "Saved: ${formatDuration(ack.durationSeconds)} · ${ack.calories} kcal"
        }
        else -> {
            isWorkoutActive = false
            statusMessage = ack.message
        }
    }
    if (ack.dailyGoal > 0) dailyGoal = ack.dailyGoal
}"""
)

# ---------------------------------------------------------------- step 5 data client
h("Step 5 — Phone: publishing the goal (Feature 2)", 1)
para(
    "publishDailyGoal() persists the value locally first, then writes a DataMap into a Data item. "
    "PutDataRequest.create(path) builds the wear:// URI, and DataMap.toByteArray() serializes the "
    "key/value payload. Because the phone and watch share applicationId, the item is replicated "
    "automatically when the watch is connected."
)
code(
"""private fun publishDailyGoal() {
    val steps = (goalDraft / WearableConstants.GOAL_STEP).roundToInt()
        .times(WearableConstants.GOAL_STEP)
        .coerceIn(WearableConstants.MIN_DAILY_GOAL,
                  WearableConstants.MAX_DAILY_GOAL)
    val updatedAt = System.currentTimeMillis()

    repository.saveDailyGoal(steps, updatedAt)   // phone owns the value

    lifecycleScope.launch {
        runCatching {
            val map = DataMap().apply {
                putInt(WearableConstants.KEY_DAILY_GOAL, steps)
                putLong(WearableConstants.KEY_GOAL_UPDATED_AT, updatedAt)
            }
            val request = PutDataRequest
                .create(WearableConstants.PATH_DAILY_GOAL)
                .apply { data = map.toByteArray() }
            dataClient.putDataItem(request).await()
        }.onSuccess {
            statusMessage = "Goal ${formatSteps(steps)} steps synced to the watch"
        }
    }
}"""
)

# ---------------------------------------------------------------- step 6 watch data
h("Step 6 — Watch: receiving the replicated goal (Feature 2)", 1)
para(
    "In play-services-wearable 20.x, DataClient.OnDataChangedListener delivers a DataEventBuffer, not a "
    "single event. The code iterates the buffer, filters by TYPE_CHANGED and by the goal path, reads the "
    "DataMap from the item bytes, and releases the buffer. The path filter is what keeps the two features "
    "isolated inside the same listener."
)
code(
"""override fun onDataChanged(dataEvents: DataEventBuffer) {
    try {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem ?: return@forEach
            if (item.uri.path != WearableConstants.PATH_DAILY_GOAL) return@forEach

            val steps = DataMap.fromByteArray(item.data ?: return@forEach)
                .getInt(WearableConstants.KEY_DAILY_GOAL, dailyGoal)
            dailyGoal = steps
        }
    } finally {
        dataEvents.release()
    }
}"""
)
para(
    "The watch also calls readCurrentGoal() in onResume() to load the already-replicated item when the "
    "app opens, so the goal is correct even if the watch started after the phone last wrote it. "
    "The phone has a matching listener that adopts external changes, which keeps both devices consistent "
    "without a second source of truth."
)

# ---------------------------------------------------------------- step 7 ui
h("Step 7 — User interface", 1)
para(
    "Phone (Jetpack Compose Material 3): a scrollable hub with a LIVE WORKOUT card (state, elapsed timer, "
    "status message), a DAILY STEP GOAL card (slider from 2,000 to 20,000 in 500-step increments and a "
    "'Sync goal to watch' button), and a WORKOUT HISTORY card listing the latest sessions with duration "
    "and calories. A LaunchedEffect ticks the clock once per second while a workout is active."
)
para(
    "Watch (Wear Compose Material 3): a single glanceable screen with the FitPulse label, the current goal, "
    "a large elapsed timer, the workout state, one primary button that toggles START/STOP, the latest "
    "acknowledgement message, and both student names at the bottom. AppScaffold + ScreenScaffold provide "
    "the correct round-screen insets."
)

# ---------------------------------------------------------------- interface captures
h("Interface Captures (Running Emulators)", 1)
para(
    "The images below were captured from the built APKs running on the Phone emulator (API 36) and the "
    "Wear OS XL Round emulator (API 36.1). They show each app in its idle state. The next section "
    "contains the placeholders for the execution captures that prove the cross-device state changes."
)

def caption(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    r.italic = True
    r.font.size = Pt(9)
    r.font.color.rgb = RGBColor(0x55, 0x55, 0x55)
    return p

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.add_run().add_picture("/home/kevs/StudioProjects/FitPulse/docs/screenshots/phone_hub.png", width=Inches(2.35))
caption("Figure A — Phone hub idle: live workout card, daily goal slider and empty history.")

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
p.add_run().add_picture("/home/kevs/StudioProjects/FitPulse/docs/screenshots/watch_idle.png", width=Inches(2.1))
caption("Figure B — Wear OS client idle: goal at a glance, one-tap START and student names.")

# ---------------------------------------------------------------- screenshots
h("Screenshots of Execution", 1)
para(
    "The following captures were taken from the paired Phone (API 36.1) and Wear OS XL Round emulators "
    "running side by side in Android Studio. Each figure shows the state change on one device and the "
    "corresponding reaction on the other."
)
SHOTS = "/home/kevs/StudioProjects/FitPulse/docs/screenshots/"
for img, caption in [
    ("exec_1.png", "Figure 1 — Both apps idle: phone hub IDLE with 8,000-step goal, watch READY."),
    ("exec_2.png", "Figure 2 — Watch sends START (MessageClient): phone ACK switches both devices to ACTIVE with the same elapsed time."),
    ("exec_3.png", "Figure 3 — Watch sends STOP: phone computes 00:26 · 4 kcal, saves it to history and the watch shows the summary."),
    ("exec_4.png", "Figure 4 — Phone syncs a 10,000-step goal (DataClient): the watch goal line updates."),
]:
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(SHOTS + img, width=Inches(5.2))
    c = doc.add_paragraph(); c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = c.add_run(caption); r.italic = True; r.font.size = Pt(9)

# ---------------------------------------------------------------- code explanation
h("Code Explanation", 1)
para(
    "This section explains the pieces of code that are essential to the project's functionality; the full "
    "source is included in the repository."
)
h("1. How the path is used", 2)
para(
    "A path is the routing key of the Data Layer. On the sender, it is the second argument of "
    "MessageClient.sendMessage(nodeId, path, data). On the receiver, it is the messageEvent.path field. "
    "The phone switches on that field to decide whether a command starts or stops a workout, and the watch "
    "filters on PATH_WORKOUT_ACK to ignore anything else. The goal feature uses the same idea with "
    "DataClient: PutDataRequest.create(PATH_DAILY_GOAL) on the phone and item.uri.path on the watch."
)
h("2. How the payload is sent and received", 2)
para(
    "The payload is a ByteArray. WorkoutCommand.toJsonBytes() converts the Kotlin object into UTF-8 JSON "
    "on the watch; WorkoutCommand.fromJsonBytes() reconstructs it on the phone. The same pattern is used "
    "for WorkoutAck in the opposite direction. This is the 'common data model + serialization' requirement: "
    "both modules compile the same DataModels.kt, so field names and types cannot drift."
)
h("3. Why coroutines are used", 2)
para(
    "Every Wearable call returns a Task. await() suspends the coroutine until the Task completes instead "
    "of nesting listeners. lifecycleScope ties all of that work to the activity lifecycle, so a screen "
    "rotation or a destroyed activity cancels pending sends instead of leaking them. All UI state writes "
    "happen on the main dispatcher, which is lifecycleScope's default."
)
h("4. Why the phone is authoritative", 2)
para(
    "The watch could compute duration and calories itself, but then two devices would own the same truth. "
    "The chosen design makes the phone compute and persist the session, then send the result back in the "
    "acknowledgement. The watch's timer is only a visual mirror of the confirmed session; the stored data "
    "always comes from the phone."
)
h("5. How the two features avoid collision", 2)
para(
    "Feature isolation happens at two levels. First, commands travel on MessageClient paths while the goal "
    "travels on a DataClient data item, which are different transports. Second, inside each listener the "
    "code filters by path before touching state. Adding a third feature later only requires a new constant "
    "and a new when branch; existing behavior is untouched."
)

# ---------------------------------------------------------------- testing
h("Testing and Demo Script", 1)
para("Automated build verification:", bold=True)
code("""$ ./gradlew :mobile:assembleDebug :wear:assembleDebug
BUILD SUCCESSFUL
mobile-debug.apk
wear-debug.apk""")
para("Manual test matrix:", bold=True)
table(
    ["#", "Action", "Expected result"],
    [
        ["T1", "Open both apps without pairing", "Watch shows 'No phone connected. Pair the watch first.' Phone shows IDLE."],
        ["T2", "Watch: tap START WORKOUT", "Phone switches to ACTIVE and starts logging; watch receives ACK_STARTED and shows ACTIVE + timer."],
        ["T3", "Watch: tap STOP WORKOUT", "Phone computes duration/calories, stores the session, history updates; watch shows 'Saved: mm:ss · N kcal'."],
        ["T4", "Phone: tap STOP while idle (simulated second command)", "Phone replies ACK_ERROR; watch shows the error message and remains IDLE."],
        ["T5", "Phone: move goal slider and tap Sync goal to watch", "Phone persists the goal; watch updates its goal line via onDataChanged."],
        ["T6", "Close and reopen the watch app", "Watch reloads the replicated goal with readCurrentGoal()."],
    ],
    widths=[0.4, 2.6, 4.0],
)
para(
    "Demo video outline (max 3 minutes): 0:00–0:30 architecture and the two chosen features; "
    "0:30–1:30 code walkthrough of WearableConstants, WorkoutCommand, the phone onMessageReceived/handleStop "
    "and the watch sendCommand/onMessageReceived; 1:30–2:30 live execution of T2, T3 and T5; "
    "2:30–3:00 conclusion and student names."
)

# ---------------------------------------------------------------- limitations
h("Limitations and Future Work", 1)
for item in [
    "The phone must be running to answer commands. Moving the listeners into a foreground service would let it respond with the screen off.",
    "History is stored in SharedPreferences for clarity of the demo; Room would be the next step for real analytics.",
    "Calories use a fixed demo rate (8 kcal/min). A production version would use heart rate and user profile data as described in the FRD.",
    "The smart display and cloud backup described in the FRD are outside the phone ↔ watch scope of this project.",
]:
    bullet(item)

h("Conclusion", 1)
para(
    "FitPulse demonstrates a complete multi-device loop with two distinct Wearable APIs. Feature 1 proves "
    "bidirectional command/acknowledgement with MessageClient; Feature 2 proves durable configuration "
    "replication with DataClient. Both share a typed JSON model, a single constants file, and a clean "
    "separation of responsibilities: the watch is the immediate control surface and the phone is the "
    "authoritative hub."
)
para("Kevin Noh · Kenny Peralta — TI91", bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)

# Metadata
doc.core_properties.title = "FitPulse — Smart Devices Development Final Project"
doc.core_properties.author = "Kevin Noh, Kenny Peralta"
doc.core_properties.subject = "Wear OS + Android phone multi-device project"

doc.save(OUT)
print("saved", OUT)
