package com.example.animator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgDark = Color(0xFF101018)
private val Surface1 = Color(0xFF1B1B27)
private val Surface2 = Color(0xFF2A2A3A)
private val Accent = Color(0xFF7C4DFF)
private val Accent2 = Color(0xFF00D9A0)
private val Danger = Color(0xFFFF5470)
private val TextDim = Color(0xFF9A9AB0)

private fun formatDate(timeMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
    return sdf.format(Date(timeMs))
}

@Composable
fun ProjectListScreen(
    onOpenProject: (ProjectMeta) -> Unit,
    onCreateNewProject: (ProjectMeta) -> Unit
) {
    val context = LocalContext.current
    val projects = remember { mutableStateListOf<ProjectMeta>().also { it.addAll(listProjects(context)) } }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- ÜST BAŞLIK ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(Surface1)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("🎨 Animatör", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Projelerim ve Çalışma Alanım", color = TextDim, fontSize = 12.sp)
            }

            // Yeni Proje Oluştur Butonu
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Accent2)
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("➕ Yeni Proje", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // --- PROJE LİSTESİ ---
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📂 Henüz Proje Yok", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Aşağıdaki butona basarak ilk çizim veya animasyonunu başlat!", color = TextDim, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Accent)
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 18.dp, vertical = 11.dp)
                    ) {
                        Text("✨ İlk Projeni Oluştur", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects, key = { it.id }) { meta ->
                    ProjectCard(
                        meta = meta,
                        onOpen = { onOpenProject(meta) },
                        onDelete = {
                            deleteProjectById(context, meta.id)
                            projects.remove(meta)
                        }
                    )
                }
            }
        }
    }

    // --- YENİ PROJE & ŞABLON DİALOGU ---
    if (showCreateDialog) {
        CreateProjectDialog(
            defaultName = "Proje ${projects.size + 1}",
            onConfirm = { newMeta ->
                onCreateNewProject(newMeta)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun ProjectCard(
    meta: ProjectMeta,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface1)
            .border(1.dp, Surface2, RoundedCornerShape(16.dp))
            .clickable { onOpen() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Şablon Etiketi / Önizleme Rozeti
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(meta.template.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Proje Adı ve Tarihi
        Column {
            Text(
                meta.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "${formatDate(meta.updatedTime)} · ${meta.frameCount} Kare",
                color = TextDim,
                fontSize = 11.sp
            )
        }

        // Butonlar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent)
                    .clickable { onOpen() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("📂 Aç", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Surface2)
                    .clickable { onDelete() }
                    .padding(6.dp)
            ) {
                Text("🗑", color = Danger, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    defaultName: String,
    onConfirm: (ProjectMeta) -> Unit,
    onDismiss: () -> Unit
) {
    var projectName by remember { mutableStateOf(defaultName) }
    var selectedTemplate by remember { mutableStateOf(CanvasTemplate.BLANK_LANDSCAPE) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .background(Surface1)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨ Yeni Proje Şablonu", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Surface2)
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("✕", color = Color.White, fontSize = 13.sp)
                    }
                }

                // Proje Adı Input
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Proje Adı", color = TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Accent2,
                        unfocusedBorderColor = Surface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Şablon ve Tuval Tipi Seç:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                // Şablon Listesi Grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CanvasTemplate.entries.forEach { t ->
                        val isSelected = t == selectedTemplate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Accent else Surface2)
                                .border(
                                    1.dp,
                                    if (isSelected) Accent2 else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTemplate = t }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(t.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (isSelected) {
                                Text("✓", color = Accent2, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                            .clickable { onDismiss() }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("İptal", color = Color.White, fontSize = 13.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Accent2)
                            .clickable {
                                val id = "proj_${System.currentTimeMillis()}"
                                val name = projectName.ifBlank { "Proje 1" }
                                val meta = ProjectMeta(
                                    id = id,
                                    name = name,
                                    template = selectedTemplate
                                )
                                onConfirm(meta)
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨ Projeyi Başlat", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}