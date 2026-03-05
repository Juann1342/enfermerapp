package com.chifuz.enfermerapp.ui.navigation

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.chifuz.enfermerapp.ui.components.AboutDialog
import com.chifuz.enfermerapp.ui.components.SettingsDialog
import com.chifuz.enfermerapp.ui.screens.dosis.DosisScreen
import com.chifuz.enfermerapp.ui.screens.perfusion.PerfusionScreen
import com.chifuz.enfermerapp.ui.screens.sync.SyncScreen
import com.chifuz.enfermerapp.ui.screens.edad.EdadScreen
import com.chifuz.enfermerapp.ui.screens.notas.NotasScreen
import com.chifuz.enfermerapp.ui.screens.notas.NotasViewModel
import com.chifuz.enfermerapp.ui.screens.notas.NotasViewModelFactory
import com.chifuz.enfermerapp.ui.screens.units.UnitsScreen
import com.chifuz.enfermerapp.utils.PrefsManager

sealed class Screen(val route: String, val title: Int, val icon: Int) {
    object Dosis : Screen("dosis", R.string.menu_dosis, R.drawable.ic_dosis)
    object Perfusion : Screen("perfusion", R.string.menu_perfusion, R.drawable.ic_perfusion)
    object Sync : Screen("sync", R.string.menu_sync, R.drawable.ic_gota)
    object Edad : Screen("edad", R.string.menu_edad_nav, R.drawable.ic_edad)
    object Units : Screen("units", R.string.menu_units, R.drawable.ic_conversor)

    companion object {
        const val SYNC_ROUTE_WITH_ARG = "sync/{gttsMin}"
        const val GTTS_MIN_KEY = "gttsMin"
    }
}

sealed class MenuItem(val title: Int, val icon: ImageVector) {
    object Terms : MenuItem(R.string.menu_terms, Icons.Default.Description)
    object Share : MenuItem(R.string.menu_share, Icons.Default.Share)
}

val navItems = listOf(Screen.Dosis, Screen.Perfusion, Screen.Sync, Screen.Edad, Screen.Units)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(onThemeChanged: (Boolean) -> Unit, // Callback para el tema
               onLangChanged: (String) -> Unit   ) {// Callback para el idioma
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    var expanded by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    // Trigger para refrescar la UI cuando cambia el estado de premium
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val isConcentrationModeActive = remember(currentRoute, refreshTrigger) {
        AdsManager.isPremiumActive(context)
    }

    val activity = context as? MainActivity
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
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
                    var isLoadingAd by remember { mutableStateOf(false) }

                    val isPremium = remember(showConcentrationDialog, refreshTrigger) {
                        AdsManager.isPremiumActive(context)
                    }

                    // Botón de Notas
                    IconButton(onClick = { navController.navigate("notas_screen") }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nota),
                            contentDescription = stringResource(R.string.notas_titulo),
                            modifier = Modifier.size(26.dp)
                        )

                    }

                    // Botón Modo Concentración (Estrella)
                    val starColor = if (isConcentrationModeActive) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                    IconButton(onClick = { showConcentrationDialog = true }) {
                        Icon(
                          //  imageVector = if (isConcentrationModeActive) Icons.Default.Star else Icons.Default.WorkspacePremium,
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = starColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Menú de opciones
                    IconButton(onClick = { expanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.opciones))
                    }

                    // --- LÓGICA DE DIÁLOGOS (CORREGIDA) ---
                    if (showConcentrationDialog) {
                        if (isPremium) {
                            ConcentrationActiveDialog(
                                tiempoTexto = AdsManager.getRemainingTimeFormatted(context),
                                onDismiss = { showConcentrationDialog = false }
                            )
                        } else {
                            ConcentrationOfferDialog(
                                isLoadingAd = isLoadingAd,
                                onDismiss = { showConcentrationDialog = false },
                                onWatchVideo = {
                                    isLoadingAd = true
                                    activity?.let {
                                        isLoadingAd = true // Iniciamos el loader antes de llamar al manager
                                        AdsManager.showRewarded(
                                            activity = it,
                                            onAdAvailable = { isDone ->
                                                // isDone == true significa que el proceso terminó (ya sea por éxito o por timeout)
                                                if (isDone) {
                                                    isLoadingAd = false
                                                    // Si el anuncio no se cargó (mRewardedAd sigue null en el manager)
                                                    Toast.makeText(context, context.getString(R.string.concentracion_toast_fail), Toast.LENGTH_LONG).show()

                                                }
                                            },
                                            onRewardEarned = {
                                                isLoadingAd = false
                                                showConcentrationDialog = false
                                                refreshTrigger++
                                                Toast.makeText(context, context.getString(R.string.concentracion_toast_exito), Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_settings)) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                expanded = false
                                showSettingsDialog = true
                            }
                        )
                        if (activity?.isPrivacyOptionsRequired == true) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_privacy_settings)) },
                                leadingIcon = { Icon(Icons.Default.PrivacyTip, contentDescription = null) },
                                onClick = {
                                    expanded = false
                                    com.google.android.ump.UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(MenuItem.Terms.title)) },
                            leadingIcon = { Icon(MenuItem.Terms.icon, contentDescription = null) },
                            onClick = { expanded = false; showDisclaimerDialog = true }
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
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.compartir_via)))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                expanded = false
                                showAboutDialog = true
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp
            ) {
                navItems.forEach { screen ->
                    val selected = currentDestination?.destination?.hierarchy?.any { it.route?.contains(screen.route) == true } == true
                    NavigationBarItem(
                        icon = { CustomIcon(id = screen.icon, contentDescription = stringResource(screen.title)) },
                        label = { Text(text = stringResource(screen.title), style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            // El fondo de la "pastilla" cuando está seleccionado (celestito de la TopBar)
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            // El color del icono cuando está seleccionado
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            // El color del texto cuando está seleccionado
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            // Colores para el estado no seleccionado
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.Dosis.route, Modifier.padding(innerPadding)) {
            composable(Screen.Dosis.route) { DosisScreen(navController) }
            composable(Screen.Perfusion.route) { PerfusionScreen(navController) }
            composable(Screen.Sync.route) { SyncScreen(gttsMinInicial = 0) }
            composable(route = Screen.SYNC_ROUTE_WITH_ARG, arguments = listOf(navArgument(Screen.GTTS_MIN_KEY) { defaultValue = 0 })) {
                SyncScreen(gttsMinInicial = it.arguments?.getInt(Screen.GTTS_MIN_KEY) ?: 0)
            }
            composable(Screen.Edad.route) { EdadScreen() }
            composable(Screen.Units.route) { UnitsScreen() }
            composable("notas_screen") {
                val database = remember { EnfermerAppDatabase.getDatabase(context) }
                val repository = remember { NotaRepository(database.notaDao()) }
                val viewModel: NotasViewModel = viewModel(factory = NotasViewModelFactory(repository))
                NotasScreen(viewModel = viewModel, isConcentrationModeActive = isConcentrationModeActive)
            }
        }
    }

    if (showDisclaimerDialog) DisclaimerDialog(onDismiss = { showDisclaimerDialog = false })
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    if (showSettingsDialog) {
        SettingsDialog(
            currentDark = PrefsManager.isDarkMode(context),
            currentLang = PrefsManager.getLang(context),
            onDismiss = { showSettingsDialog = false },
            onThemeChanged = onThemeChanged, // Se lo pasamos a la Activity
            onLangChanged = onLangChanged    // Se lo pasamos a la Activity
        )
    }
}

@Composable
fun ConcentrationOfferDialog(onDismiss: () -> Unit, onWatchVideo: () -> Unit, isLoadingAd: Boolean) {
    AlertDialog(
        onDismissRequest = if (isLoadingAd) ({}) else onDismiss,
        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700)) },
        title = { Text(text = stringResource(R.string.concentracion_titulo)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.concentracion_oferta_desc), textAlign = TextAlign.Center)
                if (isLoadingAd) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(text = stringResource(R.string.concentracion_buscando_ad), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onWatchVideo, modifier = Modifier.fillMaxWidth(), enabled = !isLoadingAd) {
                Text(if (isLoadingAd) stringResource(R.string.concentracion_btn_cargando) else stringResource(R.string.concentracion_btn_activar))
            }
        },
        dismissButton = {
            if (!isLoadingAd) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.concentracion_btn_luego), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
fun ConcentrationActiveDialog(tiempoTexto: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
        title = { Text(text = stringResource(R.string.concentracion_activo_titulo)) },
        text = { Text(text = stringResource(R.string.concentracion_activo_desc, tiempoTexto), textAlign = TextAlign.Center) },
        confirmButton = { Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.concentracion_btn_entendido)) } }
    )
}

@Composable
fun CustomIcon(@DrawableRes id: Int, contentDescription: String?) {
    Icon(painter = painterResource(id = id), contentDescription = contentDescription, modifier = Modifier.size(24.dp))
}

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
                    imageVector = Icons.Default.Description,
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

