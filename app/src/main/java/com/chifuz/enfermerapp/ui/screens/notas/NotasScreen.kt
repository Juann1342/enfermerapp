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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val notas by viewModel.notas.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var notaAEditar by remember { mutableStateOf<Nota?>(null) }
    var notaAEliminar by remember { mutableStateOf<Nota?>(null) }
    var mostrarDialogoEdicion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Buscador refinado
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.notas_buscar)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    notaAEditar = null
                    mostrarDialogoEdicion = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.notas_nueva_nota).uppercase()) }
            )
        }
    ) { padding ->
        if (notas.isEmpty()) {
            EmptyNotasPlaceholder(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Más espacio entre notas
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

                    // Inserción de Ad Nativo con margen
                    if ((index + 1) % 3 == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        NativeAdNotasComponent()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

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

    // Alerta de eliminación consistente
    notaAEliminar?.let { nota ->
        AlertDialog(
            onDismissRequest = { notaAEliminar = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.notas_eliminar_titulo)) },
            text = { Text(stringResource(R.string.notas_eliminar_pregunta)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarNota(nota)
                        notaAEliminar = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.notas_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { notaAEliminar = null }, modifier = Modifier.fillMaxWidth()) {
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
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
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
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (nota.contenido.isNotBlank()) {
                        Text(
                            text = nota.contenido,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Acciones rápidas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val fullText = "${if (nota.titulo.isNotBlank()) "${nota.titulo}\n" else ""}${nota.contenido}$promoFooter"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, fullText)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, compartirTitulo))
                    }) {
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Text(
                text = formatTimestamp(nota.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
            Text(
                text = if (nota == null) stringResource(R.string.notas_nueva_nota) else stringResource(R.string.notas_editar),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text(stringResource(R.string.notas_hint_titulo)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text(stringResource(R.string.notas_hint_contenido)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(titulo, contenido) },
                enabled = contenido.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.notas_aceptar).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.notas_cancelar))
            }
        }
    )
}

@Composable
fun EmptyNotasPlaceholder(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Text(
                text = stringResource(R.string.notas_vacia),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}