package com.chifuz.enfermerapp.ui.navigation


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreVert // <--- NUEVA IMPORTACIÓN
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ui.screens.dosis.DosisScreen
import com.chifuz.enfermerapp.ui.screens.perfusion.PerfusionScreen
import com.chifuz.enfermerapp.ui.screens.sync.SyncScreen
import com.chifuz.enfermerapp.ui.screens.sync.SyncViewModel
import com.chifuz.enfermerapp.ui.screens.dosis.DosisViewModel
import com.chifuz.enfermerapp.ui.screens.perfusion.PerfusionViewModel


// Definición de las rutas y sus iconos (usando los que funcionan)
sealed class Screen(val route: String, val title: String, val icon: Int) {
    object Dosis : Screen("dosis", "", R.drawable.ic_dosis)
    object Perfusion : Screen("perfusion", "", R.drawable.ic_perfusion)
    // Ruta de sincronización base (usada por la barra inferior)
    object Sync : Screen("sync", "", R.drawable.ic_gota)

    // Constantes para la ruta con argumento
    companion object {
        const val SYNC_ROUTE_WITH_ARG = "sync/{gttsMin}"
        const val GTTS_MIN_KEY = "gttsMin"
    }
}

// Opciones del menú superior (hamburguesa)
sealed class MenuItem(val title: String, val icon: ImageVector) {
    // Mantener Icons.Default.Info para el DropdownMenuItem
    object Terms : MenuItem("Términos y Condiciones", Icons.Default.Info)
    object Share : MenuItem("Recomendar App", Icons.Default.Share)
}

// Lista de destinos de la barra de navegación inferior
val navItems = listOf(
    Screen.Dosis,
    Screen.Perfusion,
    Screen.Sync
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    // Estado para manejar el menú desplegable (hamburguesa)
    var expanded by remember { mutableStateOf(false) }
    // ESTADOS DE DIÁLOGOS
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showUpcomingDialog by remember { mutableStateOf(false) } // <--- NUEVO ESTADO

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = navItems.find { it.route == currentRoute }?.title ?: "EnfermerApp",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                // Aplicamos colores para la barra superior
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // Fondo de la barra
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, // Color del texto
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Color de los iconos (ej. Menú)
                ),
                actions = {
                    // Menú hamburguesa (Opciones secundarias)
                    IconButton(onClick = { expanded = true }) {
                        // Ícono de tres puntos verticales para menú de opciones secundarias
                        Icon(
                            imageVector = Icons.Default.MoreVert, // <--- CAMBIO CLAVE
                            contentDescription = "Opciones"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(MenuItem.Terms.title) },
                            leadingIcon = { Icon(MenuItem.Terms.icon, contentDescription = null) },
                            onClick = {
                                expanded = false
                                showDisclaimerDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(MenuItem.Share.title) },
                            leadingIcon = { Icon(MenuItem.Share.icon, contentDescription = null) },
                            onClick = {
                                expanded = false
                                showUpcomingDialog = true // <--- Mostrar diálogo "Próximamente"
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                // Aplicamos el color de Superficie como fondo de la barra inferior
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                navItems.forEach { screen ->
                    // Usamos contains en lugar de == para Sync, ya que la ruta puede tener argumentos
                    val selected = currentDestination?.destination?.hierarchy?.any { it.route?.contains(screen.route) == true } == true
                    NavigationBarItem(
                        // CAMBIO CLAVE: Usamos CustomIcon para renderizar el recurso Int
                        icon = { CustomIcon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selected,
                        // Aplicamos colores para los ítems de la barra inferior
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        onClick = {
                            // Lógica de navegación
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dosis.route,
            Modifier.padding(innerPadding)
        ) {
            // Pantalla 1: Cálculo de Dosis
            composable(Screen.Dosis.route) {
                DosisScreen(navController)
            }
            // Pantalla 2: Cálculo de Perfusión
            composable(Screen.Perfusion.route) {
                PerfusionScreen(navController)
            }

            // PANTALLA 3A: Ruta simple para navegación desde la barra inferior (Sin argumento)
            composable(Screen.Sync.route) {
                SyncScreen(gttsMinInicial = 0) // <--- Llama con 0 para inicio manual
            }

            // PANTALLA 3B: Ruta compleja para navegación desde el diálogo de Perfusión (Con argumento)
            composable(
                route = Screen.Companion.SYNC_ROUTE_WITH_ARG,
                arguments = listOf(navArgument(Screen.Companion.GTTS_MIN_KEY) { defaultValue = 0 })
            ) { backStackEntry ->
                val gttsMin = backStackEntry.arguments?.getInt(Screen.Companion.GTTS_MIN_KEY) ?: 0
                SyncScreen(gttsMinInicial = gttsMin) // <--- Pasa el ritmo inicial importado
            }
        }
    }

    // --- Diálogos de la Aplicación ---
    if (showDisclaimerDialog) {
        DisclaimerDialog(onDismiss = { showDisclaimerDialog = false })
    }

    // --- NUEVO DIÁLOGO: Próximamente ---
    if (showUpcomingDialog) {
        UpcomingFeatureDialog(onDismiss = { showUpcomingDialog = false })
    }
}

// Composable del diálogo de Descargo de Responsabilidad
@Composable
fun DisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("ENTENDIDO")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Información",
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = { Text("Descargo de Responsabilidad y Términos de Uso", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Al utilizar la aplicación EnfermerApp, usted acepta y reconoce los siguientes términos:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Herramienta de Soporte
                Text(
                    text = "1. Herramienta de Soporte, No Sustituto:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "EnfermerApp es una herramienta auxiliar para facilitar cálculos comunes en la práctica de enfermería. Bajo ninguna circunstancia esta aplicación reemplaza el juicio clínico profesional ni la supervisión médica.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 2. Validación de Cálculos
                Text(
                    text = "2. Obligación de Validación:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "El usuario (personal de enfermería, médico, o cualquier profesional de la salud) tiene la OBLIGACIÓN indelegable de verificar y validar manualmente todos los resultados generados por la aplicación antes de su administración o aplicación en el paciente. La inexactitud del cálculo puede poner en riesgo la salud del paciente.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 3. Descargo de Responsabilidad Legal
                Text(
                    text = "3. Limitación de Responsabilidad:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "El Creador de la aplicación (el Desarrollador) no se hace responsable de las consecuencias directas, indirectas, incidentales o punitivas que resulten del uso incorrecto, inadecuado o negligente de los datos proporcionados por EnfermerApp. El uso de esta aplicación es bajo la exclusiva responsabilidad del profesional que la utiliza y/o del centro de salud donde se aplican los tratamientos.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 4. Integridad de los Datos y Fórmulas
                Text(
                    text = "4. Integridad y Fórmulas:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "El Desarrollador se esfuerza por utilizar fórmulas universalmente aceptadas, pero el usuario debe asegurarse de que estas fórmulas sean apropiadas para las guías clínicas de su institución.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun CustomIcon(@androidx.annotation.DrawableRes id: Int, contentDescription: String?) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp) // Tamaño estándar para NavigationBarItem
    )
}
@Composable
fun UpcomingFeatureDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("ACEPTAR")
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Compartir",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Función Próximamente Disponible", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    text = "La función 'Recomendar App' estará disponible después de la fase de prueba cerrada, una vez que el nombre de paquete final sea establecido.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "¡Gracias por probar EnfermerApp!",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}