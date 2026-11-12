package com.example.animator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class History(private val limit: Int = 100) {
    private class Entry(val undo: () -> Unit, val redo: () -> Unit)

    private val undoStack = ArrayList<Entry>()
    private val redoStack = ArrayList<Entry>()
    private var version by mutableIntStateOf(0)   // butonların yeniden çizilmesi için

    val canUndo: Boolean get() = version >= 0 && undoStack.isNotEmpty()
    val canRedo: Boolean get() = version >= 0 && redoStack.isNotEmpty()

    fun push(undo: () -> Unit, redo: () -> Unit) {
        undoStack.add(Entry(undo, redo))
        if (undoStack.size > limit) undoStack.removeAt(0)
        redoStack.clear()
        version++
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val e = undoStack.removeAt(undoStack.lastIndex)
        e.undo(); redoStack.add(e); version++
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val e = redoStack.removeAt(redoStack.lastIndex)
        e.redo(); undoStack.add(e); version++
        return true
    }

    fun clear() { undoStack.clear(); redoStack.clear(); version++ }

    // Çizgi listesi değiştiyse kaydet (çizim, silgi, doldurma, taşıma, renk...)
    fun pushStrokes(list: MutableList<StrokeData>, before: List<StrokeData>) {
        val after = list.toList()
        if (before.size == after.size && before.indices.all { before[it] === after[it] }) return
        push(
            undo = { list.clear(); list.addAll(before) },
            redo = { list.clear(); list.addAll(after) }
        )
    }

    fun pushInstance(inst: CharInstance, before: InstanceState) {
        val after = inst.snapshot()
        if (after.offset == before.offset && after.pose == before.pose) return
        push(undo = { inst.restore(before) }, redo = { inst.restore(after) })
    }

    fun pushSkeleton(frame: Frame, before: SkeletonState) {
        val after = frame.skeletonState()
        if (after.joints == before.joints && after.bones == before.bones && after.root == before.root) return
        push(undo = { frame.restoreSkeleton(before) }, redo = { frame.restoreSkeleton(after) })
    }
}

class InstanceState(val offset: Offset, val pose: Map<Int, Offset>)
fun CharInstance.snapshot() = InstanceState(offset, pose.toMap())
fun CharInstance.restore(s: InstanceState) {
    offset = s.offset; pose.clear(); pose.putAll(s.pose)
}

class SkeletonState(
    val joints: List<Pair<Int, Offset>>,
    val bones: List<Bone>,
    val root: Int,
    val nextId: Int
)
fun Frame.skeletonState() =
    SkeletonState(joints.map { it.id to it.pos }, bones.toList(), rootJointId, nextJointId)
fun Frame.restoreSkeleton(s: SkeletonState) {
    joints.clear(); joints.addAll(s.joints.map { Joint(it.first, it.second) })
    bones.clear(); bones.addAll(s.bones)
    rootJointId = s.root; nextJointId = s.nextId
}