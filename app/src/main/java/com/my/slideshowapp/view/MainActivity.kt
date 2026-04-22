package com.my.slideshowapp.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.my.slideshowapp.R
import com.my.slideshowapp.model.ScreenKeyProvider
import com.my.slideshowapp.view.theme.SlideshowAppTheme
import com.my.slideshowapp.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SlideshowAppTheme {
                val mediaItems by viewModel.mediaItems.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val skipCount by viewModel.skipCount.collectAsState()
                val showDialog by viewModel.showScreenKeyDialog.collectAsState()
                val loadingState by viewModel.loadingState.collectAsState()

                if (showDialog) {
                    var keyInput by rememberSaveable { mutableStateOf(ScreenKeyProvider.screenKey) }
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissScreenKeyDialog() },
                        title = { Text(stringResource(R.string.dialog_screen_key_title)) },
                        text = {
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it },
                                label = { Text(stringResource(R.string.dialog_screen_key_label)) },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.confirmScreenKey(keyInput) }) {
                                Text(stringResource(R.string.dialog_screen_key_apply))
                            }
                        },
                        dismissButton = {
                            Button(onClick = { viewModel.dismissScreenKeyDialog() }) {
                                Text(stringResource(R.string.dialog_screen_key_cancel))
                            }
                        }
                    )
                }

                SlideshowPlayer(
                    mediaItems = mediaItems,
                    isPlaying = isPlaying,
                    skipCount = skipCount,
                    loadingState = loadingState,
                    onTogglePlayback = viewModel::togglePlayback,
                    onSkip = viewModel::skip,
                    onEditScreenKey = viewModel::openScreenKeyDialog,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
