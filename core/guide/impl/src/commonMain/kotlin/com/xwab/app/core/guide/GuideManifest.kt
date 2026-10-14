package com.xwab.app.core.guide

import com.xwab.app.core.guide.port.Guide
import com.xwab.app.core.guide.port.GuideId
import com.xwab.app.core.guide.port.GuideUse

/**
 * The guides this build ships. A guide's id is stored on devices — in the engine's source id and
 * the now-playing state — so keep it once it has shipped.
 *
 * The scripts were written for this app. They draw on published techniques: progressive muscle
 * relaxation and imagery distraction for winding down, and stimulus control for the guide heard out
 * of bed. None of them promises sleep, and the screens say the same.
 */
internal val guides: List<Guide> = listOf(
    Guide(
        id = GuideId(MUSCLE_RELAXATION),
        title = "Muscle Relaxation",
        description = "Tense and release each part of your body in turn, from your feet to your face. " +
            "A good place to start.",
        minutes = 8,
        languageTag = ENGLISH,
        use = GuideUse.WindDown,
    ),
    Guide(
        id = GuideId(QUIET_BEACH),
        title = "Quiet Beach",
        description = "Walk along an early-morning shore and rest to the sound of slow waves.",
        minutes = 4,
        languageTag = ENGLISH,
        use = GuideUse.WindDown,
    ),
    Guide(
        id = GuideId(FOREST_PATH),
        title = "Forest Path",
        description = "Follow a soft path through tall trees to a slow, clear stream.",
        minutes = 4,
        languageTag = ENGLISH,
        use = GuideUse.WindDown,
    ),
    Guide(
        id = GuideId(RAIN_CABIN),
        title = "Rain on the Cabin",
        description = "Rest in a warm cabin while steady rain falls on the roof.",
        minutes = 4,
        languageTag = ENGLISH,
        use = GuideUse.WindDown,
    ),
    Guide(
        id = GuideId(OUT_OF_BED),
        title = "Out of Bed",
        description = "For when sleep will not come: a calm, unhurried voice to keep you company " +
            "until you feel sleepy again.",
        minutes = 8,
        languageTag = ENGLISH,
        use = GuideUse.OutOfBed,
    ),
)

/**
 * Heard sitting up, out of bed, with the lights low: the listener has been lying awake. It gives
 * permission first, because effort and worry about sleep are what keep it away, then something
 * plain and slow to follow — a few ordinary chores, done unhurriedly in the mind. It never asks the
 * listener to lie down or close their eyes; it ends by leaving the choice of going back to bed to
 * their own sleepiness, as stimulus control does.
 */
private val outOfBedScript = GuideScript(
    OUT_OF_BED,
    listOf(
        line("You are up, and that is fine. Getting up when sleep will not come is the right thing to do.", 4),
        line("Sit somewhere comfortable, away from the bed, and keep the lights low.", 6),
        line("There is nothing you need to achieve now. Sleep is not something you can push. It comes on its own.", 6),
        line("For now, let's do something slow and ordinary, just in your mind.", 5),
        line("Picture a basket of clean towels, still a little warm.", 8),
        line("Take the first one out. It is pale blue. Shake it out once, and lay it flat.", 10),
        line("Fold it in half, lengthwise. Smooth the fold with your hand.", 10),
        line("Fold it in half again, and once more. Set it to one side.", 10),
        line("Take the next towel. This one is white, and a little larger.", 8),
        line("Shake it out. Lay it flat. Fold it in half, and smooth the fold.", 12),
        line("Fold it again, and again, and place it on top of the blue one.", 12),
        line("The pile is neat. There is no hurry to finish.", 10),
        line("Now picture a kitchen drawer full of spoons, forks and knives, all mixed together.", 8),
        line("Take out one spoon and put it on the left. Take out a fork and put it in the middle.", 10),
        line("Another spoon goes on the left, beside the first.", 10),
        line("A knife goes on the right. Then another fork, in the middle.", 12),
        line("Keep sorting at your own pace. Spoons on the left, forks in the middle, knives on the right.", 25),
        line("If your mind wanders off to something else, that is all right. Simply come back to the drawer.", 20),
        line("Now picture a quiet room, and walk slowly around it in your mind.", 8),
        line("There is a chair by the window. A lamp on a small table. A plant that needs a little water.", 12),
        line("Notice the colour of the walls. Notice the floor under your feet.", 12),
        line("Count the books on a shelf, slowly, one at a time. One. Two. Three. Four.", 15),
        line("Five. Six. Seven. If you lose count, start again at one. It does not matter.", 20),
        line("Let your shoulders drop, and your breath slow down on its own.", 15),
        line("Notice whether your eyelids feel heavy, or you are starting to yawn.", 8),
        line("If you feel sleepy now, go back to bed.", 5),
        line("If you do not, that is fine too. Stay up a little longer.", 3),
        line("Come back to bed when sleepiness arrives.", 5),
    ),
)

internal val guideScripts: List<GuideScript> = listOf(
    GuideScript(
        MUSCLE_RELAXATION,
        listOf(
            line("Welcome home. Your shift is over, and there is nothing else you need to do right now.", 3),
            line("Lie down somewhere comfortable, and let your eyes close when you are ready.", 5),
            line("You will gently tense one group of muscles at a time, and then let it go.", 2),
            line("Use only about half your strength. If any part of your body hurts, skip it and simply rest.", 3),
            line("Take a slow breath in through your nose.", 4),
            line("And let it out slowly.", 5),
            line("One more time. Breathe in.", 4),
            line("And breathe out, letting your body sink a little deeper.", 6),
            line("Let's begin with your feet. Curl your toes downward, and hold.", HOLD),
            line("And let them go. Notice the difference between tension and rest.", RELEASE),
            line("Now point your toes up toward your face, tightening your calves. Hold.", HOLD),
            line("And release. Let your lower legs grow heavy.", RELEASE),
            line("Press your knees together and tighten your thighs. Hold it.", HOLD),
            line("And let go. Feel the warmth spreading through your legs.", RELEASE),
            line("Squeeze the muscles of your hips and seat. Hold.", HOLD),
            line("And release. Let the bed carry your weight.", RELEASE),
            line("Gently tighten your stomach, as if bracing for a light push. Hold.", HOLD),
            line("And let it soften. Let your breath move freely.", RELEASE),
            line("Make loose fists with both hands. Squeeze.", HOLD),
            line("And open your hands. Let your fingers rest.", RELEASE),
            line("Bend your elbows and tighten your upper arms. Hold.", HOLD),
            line("And let your arms fall back, loose and heavy.", RELEASE),
            line("Lift your shoulders up toward your ears. Hold them there.", HOLD),
            line("And let them drop. Let the weight of the day slide off.", RELEASE),
            line("Very gently, press the back of your head into the pillow. Hold.", HOLD),
            line("And release. Let your neck be long and soft.", RELEASE),
            line("Scrunch up your face. Close your eyes tight, and press your lips together. Hold.", HOLD),
            line("And let your whole face go smooth.", 6),
            line("Let your jaw loosen, and your tongue rest.", 10),
            line("Now feel your whole body, from your feet to the top of your head.", 8),
            line("If you notice any tension left, breathe toward it, and let it melt as you breathe out.", 12),
            line("Your body has worked hard. It is allowed to rest now.", 8),
            line("Breathe in slowly.", 5),
            line("And breathe out.", 8),
            line("There is nothing to do and nowhere to be. Just rest.", 15),
            line("When the voice ends, stay as you are, and let sleep come when it is ready.", 5),
        ),
    ),
    GuideScript(
        QUIET_BEACH,
        listOf(
            line("Settle into a comfortable position, and let your eyes close.", 5),
            line("Take a slow breath in, and a long breath out.", 6),
            line("Imagine you are walking along a quiet beach in the early morning.", 6),
            line("The sand is cool and soft under your feet.", 6),
            line("Small waves roll in, slowly, and slide back out again.", 8),
            line("Notice the color of the water, pale blue near the shore, deeper further out.", 8),
            line("A light breeze moves across your skin. It smells of salt.", 8),
            line("You find a smooth, warm place in the sand, and you lie down.", 6),
            line("The waves keep their own easy rhythm. In, and out.", 10),
            line("There is no one you need to answer, and nothing you need to fix.", 8),
            line("Watch a single gull drift high above, carried by the wind without effort.", 10),
            line("Let your breathing slow to match the waves.", 10),
            line("Each wave that pulls back takes a little of the day with it.", 12),
            line("The sun is soft and the air is warm.", 10),
            line("Stay on this beach as long as you like, and let yourself drift.", 5),
        ),
    ),
    GuideScript(
        FOREST_PATH,
        listOf(
            line("Let your body settle, and let your eyes close.", 5),
            line("Breathe in slowly, and breathe out.", 6),
            line("Picture a wide path leading into a quiet forest.", 6),
            line("Tall trees rise on either side, and light falls through their leaves in soft patches.", 8),
            line("The ground is covered in moss and old leaves. Each step is quiet.", 8),
            line("You can smell damp earth and pine.", 8),
            line("Somewhere ahead, a stream is running over stones.", 8),
            line("You follow the sound until you reach it. The water is clear and slow.", 8),
            line("There is a flat rock beside the stream, warm from the sun. You sit down.", 8),
            line("Watch a leaf float past, turning gently, until it disappears around a bend.", 10),
            line("Let each thought that comes be like that leaf. Notice it, and let the water carry it away.", 12),
            line("The trees stand still and patient around you.", 10),
            line("Your shoulders soften. Your breath grows slow and even.", 10),
            line("Rest here, in the shade and the quiet, for as long as you need.", 5),
        ),
    ),
    GuideScript(
        RAIN_CABIN,
        listOf(
            line("Get comfortable, and let your eyes close.", 5),
            line("Take a slow breath in, and let it go.", 6),
            line("Imagine a small wooden cabin, deep in the hills, on a grey afternoon.", 6),
            line("Rain is falling steadily on the roof. It is a soft, even sound.", 8),
            line("Inside, it is warm and dry. A thick blanket is wrapped around you.", 8),
            line("There is a low, comfortable bed by the window.", 6),
            line("Through the glass you can see the rain running in thin lines down the pane.", 8),
            line("Beyond it, the trees are dark green and still.", 8),
            line("Nobody knows where you are. Nobody needs anything from you.", 10),
            line("Listen to the rain. Some drops are louder, some are quieter, and none of them need you.", 10),
            line("Let your body grow heavy against the bed.", 10),
            line("With every breath out, sink a little further into the warmth.", 12),
            line("The rain will keep falling, steady and patient, while you rest.", 10),
            line("Let it carry you toward sleep.", 5),
        ),
    ),
    outOfBedScript,
)

/** How long a muscle is held, and how long it rests afterwards. */
private const val HOLD = 6
private const val RELEASE = 12

private const val MUSCLE_RELAXATION = "muscle-relaxation"
private const val QUIET_BEACH = "quiet-beach"
private const val FOREST_PATH = "forest-path"
private const val RAIN_CABIN = "rain-cabin"
private const val OUT_OF_BED = "out-of-bed"
private const val ENGLISH = "en-US"
