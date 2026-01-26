package com.chifuz.enfermerapp.ui.screens.notas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.data.model.Nota
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotasScreen(
    viewModel: NotasViewModel,
    isConcentrationModeActive: Boolean
) {
    val notas by viewModel.notas.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // ESTADOS PARA DIÁLOGOS
    var notaAEditar by remember { mutableStateOf<Nota?>(null) }
    var notaAEliminar by remember { mutableStateOf<Nota?>(null) }
    var mostrarDialogoEdicion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.notas_buscar)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    notaAEditar = null
                    mostrarDialogoEdicion = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.notas_nueva_nota))
            }
        }
    ) { padding ->
        if (notas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.notas_vacia),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(notas) { index, nota ->
                    NotaItem(
                        nota = nota,
                        onClick = {
                            notaAEditar = nota
                            mostrarDialogoEdicion = true
                        },
                        onDelete = { notaAEliminar = nota }
                    )

                    if ((index + 1) % 3 == 0) {
                        NativeAdNotasComponent()
                    }
                }
            }
        }
    }

    // DIÁLOGO: CREAR O EDITAR
    if (mostrarDialogoEdicion) {
        NotaEditDialog(
            nota = notaAEditar,
            onDismiss = { mostrarDialogoEdicion = false },
            onSave = { titulo, contenido ->
                viewModel.guardarNota(titulo, contenido, notaAEditar?.id ?: 0)
                mostrarDialogoEdicion = false
            }
        )
    }

    // DIÁLOGO: CONFIRMAR ELIMINACIÓN
    notaAEliminar?.let { nota ->
        AlertDialog(
            onDismissRequest = { notaAEliminar = null },
            title = { Text(stringResource(R.string.notas_eliminar_titulo)) },
            text = { Text(stringResource(R.string.notas_eliminar_pregunta)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarNota(nota)
                        notaAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.notas_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { notaAEliminar = null }) {
                    Text(stringResource(R.string.notas_cancelar))
                }
            }
        )
    }
}

@Composable
fun NotaItem(
    nota: Nota,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val promoFooter = stringResource(R.string.notas_compartir_footer)
    val compartirTitulo = stringResource(R.string.notas_compartir_titulo)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nota.titulo.isNotBlank()) nota.titulo else stringResource(R.string.notas_sin_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (nota.contenido.isNotBlank()) {
                        Text(
                            text = nota.contenido,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = {
                            val fullText = "${if (nota.titulo.isNotBlank()) "${nota.titulo}\n" else ""}${nota.contenido}$promoFooter"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, fullText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, compartirTitulo))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.notas_compartir_desc),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.notas_eliminar_desc),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Text(
                text = formatTimestamp(nota.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun NotaEditDialog(
    nota: Nota?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var titulo by remember { mutableStateOf(nota?.titulo ?: "") }
    var contenido by remember { mutableStateOf(nota?.contenido ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (nota == null) stringResource(R.string.notas_nueva_nota) else stringResource(R.string.notas_editar))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text(stringResource(R.string.notas_hint_titulo)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text(stringResource(R.string.notas_hint_contenido)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(titulo, contenido) },
                enabled = contenido.isNotBlank()
            ) {
                Text(stringResource(R.string.notas_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notas_cancelar))
            }
        }
    )
}

// FUNCIONES DE UTILIDAD (AL FINAL PARA MEJOR SCOPE)
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}