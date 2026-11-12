package com.example.animator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var activeMeta by remember { mutableStateOf<ProjectMeta?>(null) }

            if (activeMeta == null) {
                ProjectListScreen(
                    onOpenProject = { meta -> activeMeta = meta },
                    onCreateNewProject = { newMeta -> activeMeta = newMeta }
                )
            } else {
                val currentMeta = activeMeta!!
                DrawingScreen(
                    activeProjectMeta = currentMeta,
                    onBackToProjects = { activeMeta = null }
                )
            }
        }
    }
}