package com.tridente.notiz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tridente.notiz.NotizApplication
import com.tridente.notiz.ui.auth.AuthViewModel
import com.tridente.notiz.ui.auth.LoginScreen
import com.tridente.notiz.ui.auth.RegisterScreen
import com.tridente.notiz.ui.editor.NoteEditorScreen
import com.tridente.notiz.ui.editor.NoteEditorViewModel
import com.tridente.notiz.ui.folder.FolderDialog
import com.tridente.notiz.ui.folder.FolderViewModel
import com.tridente.notiz.ui.home.HomeScreen
import com.tridente.notiz.ui.home.HomeViewModel
import com.tridente.notiz.ui.share.ShareDialog
import com.tridente.notiz.ui.share.ShareViewModel

object NotizDestinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val EDITOR = "editor"
    const val NOTE_ID_ARG = "noteId"
}

@Composable
fun NotizNavGraph(
    navController: NavHostController,
    app: NotizApplication
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(app.authRepository)
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            app.noteRepository,
            app.folderRepository,
            app.syncRepository,
            app.authRepository
        )
    )
    val folderViewModel: FolderViewModel = viewModel(
        factory = FolderViewModel.Factory(app.folderRepository)
    )
    val shareViewModel: ShareViewModel = viewModel(
        factory = ShareViewModel.Factory(app.shareRepository, app.tokenManager)
    )

    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val folders by folderViewModel.allFolders.collectAsState()

    var activeShareNoteId by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val startDestination = if (currentUser != null) NotizDestinations.HOME else NotizDestinations.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NotizDestinations.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.resetState()
                    navController.navigate(NotizDestinations.REGISTER)
                },
                onLoginSuccess = {
                    navController.navigate(NotizDestinations.HOME) {
                        popUpTo(NotizDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NotizDestinations.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.resetState()
                    navController.navigate(NotizDestinations.LOGIN)
                },
                onRegisterSuccess = {
                    navController.navigate(NotizDestinations.HOME) {
                        popUpTo(NotizDestinations.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(NotizDestinations.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToEditor = { noteId ->
                    val route = if (noteId != null) "${NotizDestinations.EDITOR}/$noteId" else "${NotizDestinations.EDITOR}/new"
                    navController.navigate(route)
                },
                onOpenShareDialog = { noteId ->
                    activeShareNoteId = noteId
                },
                onCreateFolderClick = {
                    showCreateFolderDialog = true
                }
            )
        }

        composable(
            route = "${NotizDestinations.EDITOR}/{${NotizDestinations.NOTE_ID_ARG}}",
            arguments = listOf(
                navArgument(NotizDestinations.NOTE_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString(NotizDestinations.NOTE_ID_ARG)
            val initialNoteId = if (noteIdArg == "new") null else noteIdArg

            val editorViewModel: NoteEditorViewModel = viewModel(
                key = noteIdArg,
                factory = NoteEditorViewModel.Factory(app.noteRepository, initialNoteId)
            )

            NoteEditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenShareDialog = { noteId ->
                    activeShareNoteId = noteId
                }
            )
        }
    }

    // Share Dialog Overlay
    activeShareNoteId?.let { noteId ->
        ShareDialog(
            noteId = noteId,
            viewModel = shareViewModel,
            onDismiss = { activeShareNoteId = null }
        )
    }

    // Create Folder Dialog Overlay
    if (showCreateFolderDialog) {
        FolderDialog(
            folders = folders,
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { name, parentId ->
                folderViewModel.createFolder(name, parentId)
                showCreateFolderDialog = false
            }
        )
    }
}
