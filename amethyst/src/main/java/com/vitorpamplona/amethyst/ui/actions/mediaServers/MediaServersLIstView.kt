/**
 * Copyright (c) 2024 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.ui.actions.mediaServers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.service.Nip96MediaServers
import com.vitorpamplona.amethyst.ui.actions.relays.SettingsCategoryWithButton
import com.vitorpamplona.amethyst.ui.navigation.INav
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.CloseButton
import com.vitorpamplona.amethyst.ui.screen.loggedIn.SaveButton
import com.vitorpamplona.amethyst.ui.stringRes
import com.vitorpamplona.amethyst.ui.theme.DoubleVertPadding
import com.vitorpamplona.amethyst.ui.theme.FeedPadding
import com.vitorpamplona.amethyst.ui.theme.StdVertSpacer
import com.vitorpamplona.amethyst.ui.theme.grayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaServersListView(
    onClose: () -> Unit,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    val mediaServersViewModel: MediaServersViewModel = viewModel()
    val mediaServersState by mediaServersViewModel.fileServers.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = Unit) {
        mediaServersViewModel.load(accountViewModel.account)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringRes(id = R.string.media_servers),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        }
                    },
                    navigationIcon = {
                        CloseButton(
                            onPress = {
                                mediaServersViewModel.refresh()
                                onClose()
                            },
                        )
                    },
                    actions = {
                        SaveButton(
                            onPost = {
                                mediaServersViewModel.saveFileServers()
                                onClose()
                            },
                            isActive = true,
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
            },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = 16.dp,
                            top = padding.calculateTopPadding(),
                            end = 16.dp,
                            bottom = padding.calculateBottomPadding(),
                        ),
                verticalArrangement = Arrangement.spacedBy(5.dp, alignment = Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringRes(id = R.string.set_preferred_media_servers),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.grayText,
                )

                LazyColumn(
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = FeedPadding,
                ) {
                    renderMediaServerList(
                        mediaServersState = mediaServersState,
                        onAddServer = { server ->
                            mediaServersViewModel.addServer(server)
                        },
                        onDeleteServer = {
                            mediaServersViewModel.removeServer(serverUrl = it)
                        },
                    )

                    Nip96MediaServers.DEFAULT.let {
                        item {
                            SettingsCategoryWithButton(
                                title = stringRes(id = R.string.built_in_media_servers_title),
                                description = stringRes(id = R.string.built_in_servers_description),
                                action = {
                                    OutlinedButton(
                                        onClick = {
                                            mediaServersViewModel.addServerList(it.map { s -> s.baseUrl })
                                        },
                                    ) {
                                        Text(text = stringRes(id = R.string.use_default_servers))
                                    }
                                },
                            )
                        }
                        itemsIndexed(
                            it,
                            key = { index: Int, server: Nip96MediaServers.ServerName ->
                                server.baseUrl
                            },
                        ) { index, server ->
                            MediaServerEntry(
                                serverEntry = server,
                                isAmethystDefault = true,
                                onAddOrDelete = { serverUrl ->
                                    mediaServersViewModel.addServer(serverUrl)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.renderMediaServerList(
    mediaServersState: List<Nip96MediaServers.ServerName>,
    onAddServer: (String) -> Unit,
    onDeleteServer: (String) -> Unit,
) {
    if (mediaServersState.isEmpty()) {
        item {
            Text(
                text = stringRes(id = R.string.no_media_server_message),
                modifier = DoubleVertPadding,
            )
        }
    } else {
        itemsIndexed(
            mediaServersState,
            key = { index: Int, server: Nip96MediaServers.ServerName ->
                server.baseUrl
            },
        ) { index, entry ->
            MediaServerEntry(
                serverEntry = entry,
                onAddOrDelete = {
                    onDeleteServer(it)
                },
            )
        }
    }

    item {
        Spacer(modifier = StdVertSpacer)
        MediaServerEditField {
            onAddServer(it)
        }
    }
}

@Composable
fun MediaServerEntry(
    modifier: Modifier = Modifier,
    serverEntry: Nip96MediaServers.ServerName,
    isAmethystDefault: Boolean = false,
    onAddOrDelete: (serverUrl: String) -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f),
        ) {
            serverEntry.let {
                Text(
                    text = it.name.replaceFirstChar(Char::titlecase),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = StdVertSpacer)
                Text(
                    text = it.baseUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.grayText,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = {
                    onAddOrDelete(serverEntry.baseUrl)
                },
            ) {
                Icon(
                    imageVector = if (isAmethystDefault) Icons.Rounded.Add else Icons.Rounded.Delete,
                    contentDescription =
                        if (isAmethystDefault) {
                            stringRes(id = R.string.add_media_server)
                        } else {
                            stringRes(id = R.string.delete_media_server)
                        },
                )
            }
        }
    }
}
