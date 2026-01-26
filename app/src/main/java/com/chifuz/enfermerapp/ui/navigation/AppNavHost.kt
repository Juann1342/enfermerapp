package com.chifuz.enfermerapp.ui.navigation


import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert // <--- NUEVA IMPORTACIÓN
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chifuz.enfermerapp.MainActivity
import com.chifuz.enfermerapp.R
import com.chifuz.enfermerapp.ads.AdsManager
import com.chifuz.enfermerapp.data.EnfermerAppDatabase
import com.chifuz.enfermerapp.data.repository.NotaRepository
import com.chifuz.enfermerapp.ui.screens.dosis.DosisScreen
import com.chifuz.enfermerapp.ui.screens.perfusion.PerfusionScreen
import com.chifuz.enfermerapp.ui.screens.sync.SyncScreen
import com.chifuz.enfermerapp.ui.screens.edad.EdadScreen
import com.chifuz.enfermerapp.ui.screens.notas.NotasScreen
import com.chifuz.enfermerapp.ui.screens.notas.NotasViewModel
import com.chifuz.enfermerapp.ui.screens.notas.NotasViewModelFactory
import com.chifuz.enfermerapp.ui.screens.units.UnitsScreen


// Definición de las rutas y sus iconos (usando los que funcionan)
sealed class Screen(val route: String, val title: Int, val icon: Int) {
    object Dosis : Screen("dosis", R.string.menu_dosis, R.drawable.ic_dosis)
    object Perfusion : Screen("perfusion", R.string.menu_perfusion, R.drawable.ic_perfusion)
    // Ruta de sincronización base (usada por la barra inferior)
    object Sync : Screen("sync", R.string.menu_sync, R.drawable.ic_gota)

    object Edad : Screen("edad", R.string.menu_edad_nav, R.drawable.ic_edad)

    object Units : Screen("units", R.string.menu_units, R.drawable.ic_age2) //cambiar


    // Constantes para la ruta con argumento
    companion object {
        const val SYNC_ROUTE_WITH_ARG = "sync/{gttsMin}"
        const val GTTS_MIN_KEY = "gttsMin"
    }
}

// Opciones del menú superior (hamburguesa)
sealed class MenuItem(val title: Int, val icon: ImageVector) {
    // Mantener Icons.Default.Info para el DropdownMenuItem
    object Terms : MenuItem(R.string.menu_terms, Icons.Default.Info)
    object Share : MenuItem(R.string.menu_share, Icons.Default.Share)
}

// Lista de destinos de la barra de navegación inferior
val navItems = listOf(
    Screen.Dosis,
    Screen.Perfusion,
    Screen.Sync,
    Screen.Edad,
    Screen.Units
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val context = LocalContext.current // <--- AÑADE ESTA LÍNEA AQUÍ
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    // Estado para manejar el menú desplegable (hamburguesa)
    var expanded by remember { mutableStateOf(false) }
    // ESTADOS DE DIÁLOGOS
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    val isConcentrationModeActive = remember(navController.currentBackStackEntryAsState().value) {
        AdsManager.isPremiumActive(context)
    }

    // Función para encontrar la MainActivity desde cualquier Composable
    fun Context.findActivity(): MainActivity? = when (this) {
        is MainActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }


    Scaffold(
        topBar = {
            val activity = context as? MainActivity

            TopAppBar(
                title = {
                    val currentScreen = navItems.find { it.route == currentRoute }
                    Text(
                        text = currentScreen?.let { stringResource(it.title) } ?: "EnfermerApp",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    var showConcentrationDialog by remember { mutableStateOf(false) }
                    val context = LocalContext.current
                    val isPremium = remember(showConcentrationDialog) { AdsManager.isPremiumActive(context) }


                    IconButton(onClick = { navController.navigate("notas_screen") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nota),
                            contentDescription = stringResource(R.string.notas_titulo),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // BOTÓN DE LA ESTRELLA
                    IconButton(onClick = { showConcentrationDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.concentracion_estrella_desc),
                            modifier = Modifier.size(28.dp),
                            tint = if (isConcentrationModeActive) Color(0xFFFFD700)
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            modifier = Modifier.size(28.dp),
                            contentDescription = stringResource(R.string.opciones)
                        )
                    }

                    // LÓGICA DE DIÁLOGOS
                    if (showConcentrationDialog) {
                        if (isPremium) {
                            // Obtenemos el texto ya formateado ("5 h 10 min" o "40 min")
                            val tiempoRestante = AdsManager.getRemainingTimeFormatted(context)

                            ConcentrationActiveDialog(
                                tiempoTexto = tiempoRestante, // Cambiamos el parámetro a String
                                onDismiss = { showConcentrationDialog = false }
                            )
                        } else {
                            // Caso 2: Oferta para activar
                            ConcentrationOfferDialog(
                                onDismiss = { showConcentrationDialog = false },
                                onWatchVideo = {
                                    activity?.let {
                                        AdsManager.showRewarded(it) {
                                            showConcentrationDialog = false
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.concentracion_toast_exito),
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // ... (el resto de tus DropdownMenuItem se mantienen igual)
                        if (activity?.isPrivacyOptionsRequired == true) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_privacy_settings)) },
                                leadingIcon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                                onClick = {
                                    expanded = false
                                    com.google.android.ump.UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                                        if (error != null) {
                                            android.util.Log.e("UMP", "${error.errorCode}: ${error.message}")
                                        }
                                    }
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(MenuItem.Terms.title)) },
                            leadingIcon = { Icon(MenuItem.Terms.icon, contentDescription = null) },
                            onClick = {
                                expanded = false
                                showDisclaimerDialog = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(MenuItem.Share.title)) },
                            leadingIcon = { Icon(MenuItem.Share.icon, contentDescription = null) },
                            onClick = {
                                expanded = false
                                val playStoreUrl = "https://play.google.com/store/apps/details?id=com.chifuz.enfermerapp"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, context.getString(R.string.compartir_app_msg, playStoreUrl))
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.compartir_via))
                                context.startActivity(shareIntent)
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
                        icon = {
                            CustomIcon(
                                id = screen.icon,
                                contentDescription = stringResource(screen.title)
                            )
                        },                        label = {
                            Text(
                                text = stringResource(screen.title),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        alwaysShowLabel = true,
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

            composable(Screen.Edad.route) {
                // Aquí llamaremos a EdadScreen() en el siguiente paso
                EdadScreen()
            }
            composable(Screen.Units.route) {
                UnitsScreen()
            }
            composable("notas_screen") {
                val context = LocalContext.current

                val database = remember { EnfermerAppDatabase.getDatabase(context) }
                val repository = remember { NotaRepository(database.notaDao()) }
                val viewModel: NotasViewModel = viewModel(
                    factory = NotasViewModelFactory(repository)
                )

                NotasScreen(
                    viewModel = viewModel,
                    isConcentrationModeActive = isConcentrationModeActive
                )
            }
        }
    }

    // --- Diálogos de la Aplicación ---
    if (showDisclaimerDialog) {
        DisclaimerDialog(onDismiss = { showDisclaimerDialog = false })
    }


}



// Composable del diálogo de Descargo de Responsabilidad
@Composable
fun DisclaimerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.entendido))
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.informacion),
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = { Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.disclaimer_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Herramienta de Soporte
                Text(
                    text = stringResource(R.string.disclaimer_sec1_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.disclaimer_sec1_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 2. Validación de Cálculos
                Text(
                    text = stringResource(R.string.disclaimer_sec2_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.disclaimer_sec2_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 3. Descargo de Responsabilidad Legal
                Text(
                    text = stringResource(R.string.disclaimer_sec3_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.disclaimer_sec3_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 4. Integridad de los Datos y Fórmulas
                Text(
                    text = stringResource(R.string.disclaimer_sec4_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.disclaimer_sec4_body),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun CustomIcon(@DrawableRes id: Int, contentDescription: String?) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
fun ConcentrationOfferDialog(onDismiss: () -> Unit, onWatchVideo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(text = stringResource(R.string.concentracion_titulo)) },
        text = { Text(text = stringResource(R.string.concentracion_oferta_desc), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(onClick = onWatchVideo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.concentracion_btn_activar))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.concentracion_btn_luego), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ConcentrationActiveDialog(tiempoTexto: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)) },
        title = { Text(text = stringResource(R.string.concentracion_activo_titulo)) },
        text = {
            Text(
                // tiempoTexto se insertará en el %1$s del string
                text = stringResource(R.string.concentracion_activo_desc, tiempoTexto),
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.concentracion_btn_entendido))
            }
        }
    )
}


