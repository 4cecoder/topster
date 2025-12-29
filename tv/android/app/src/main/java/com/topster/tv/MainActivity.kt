package com.topster.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.topster.tv.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onMediaClick = { mediaItem ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("selectedMedia", mediaItem)
                                navController.navigate("details/${mediaItem.id}/${mediaItem.type}")
                            },
                            onSearchClick = {
                                navController.navigate("search")
                            }
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            onMediaClick = { mediaItem ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("selectedMedia", mediaItem)
                                navController.navigate("details/${mediaItem.id}/${mediaItem.type}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("details/{mediaId}/{mediaType}") { backStackEntry ->
                        val mediaItem = navController.previousBackStackEntry?.savedStateHandle?.get<com.topster.tv.api.models.MediaItem>("selectedMedia")
                        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                        val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"

                        if (mediaItem != null) {
                            DetailsScreen(
                                mediaItem = mediaItem,
                                onPlayClick = { playbackItem ->
                                    // Navigate to player - we'll pass the item via saved state
                                    navController.currentBackStackEntry?.savedStateHandle?.set("playbackItem", playbackItem)
                                    navController.navigate("player")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    composable("player") {
                        val playbackItem = navController.previousBackStackEntry?.savedStateHandle?.get<PlaybackItem>("playbackItem")
                        if (playbackItem != null) {
                            PlayerScreen(
                                playbackItem = playbackItem,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
