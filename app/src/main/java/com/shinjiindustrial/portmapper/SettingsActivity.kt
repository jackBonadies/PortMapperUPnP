package com.shinjiindustrial.portmapper

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.shinjiindustrial.portmapper.ui.RuleCreationDialog
import com.shinjiindustrial.portmapper.ui.SetupPreview
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme
import com.shinjiindustrial.portmapper.ui.theme.PortMapperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    val settingsViewModel: SettingsViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Settings()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Settings() {
        val themeState by this.settingsViewModel.uiState.collectAsStateWithLifecycle()
        MyApplicationTheme(themeState) {
            val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                this.finish()
                            }) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back_action),
                                )
                            }
                        },
//                                modifier = Modifier.height(40.dp),
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = PortMapperTheme.componentColors.appBarContainer,
                            scrolledContainerColor = PortMapperTheme.componentColors.appBarScrolledContainer,
                        ),
                        scrollBehavior = scrollBehavior,
                        title = {
                            Text(
                                text = stringResource(R.string.settings),
                                fontWeight = FontWeight.Normal
                            )
                        },
                        //actions = { OverflowMenu() }
                    )
                },
                content = { it ->
                    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                    val showEnableDisable by settingsViewModel.showEnableDisable.collectAsStateWithLifecycle()
                    val showAllLocalRules by settingsViewModel.showAllLocalRules.collectAsStateWithLifecycle()
                    val backgroundRenew by settingsViewModel.backgroundRenew.collectAsStateWithLifecycle()
                    SettingsContent(
                        uiState,
                        showEnableDisable,
                        showAllLocalRules,
                        backgroundRenew,
                        settingsViewModel::updateMaterialYou,
                        settingsViewModel::updateDayNight,
                        settingsViewModel::updateShowEnableDisable,
                        settingsViewModel::updateShowAllLocalRules,
                        settingsViewModel::updateBackgroundRenew,
                        Modifier.padding(it)
                    )
                }
            )
        }
    }
//
//@Composable
//@Preview
//fun SettingsPreview()
//{
//    SetupPreview()
//    Settings()
//}


    @Composable
    fun SettingsContent(
        uiState: ThemeUiState,
        showEnableDisable: Boolean,
        showAllLocalRules: Boolean,
        backgroundRenew: Boolean,
        updateMaterialYou: (Boolean) -> Unit,
        updateDayNight: (DayNightMode) -> Unit,
        updateShowEnableDisable: (Boolean) -> Unit,
        updateShowAllLocalRules: (Boolean) -> Unit,
        updateBackgroundRenew: (Boolean) -> Unit,
        modifier: Modifier = Modifier
    ) {
        MyApplicationTheme(uiState)
        {
            Column(modifier = Modifier.then(modifier))
            {

                var showThemeAlertDialog = remember { mutableStateOf(false) }

                if (showThemeAlertDialog.value) {
                    // alert dialog...
                    AlertDialog(
                        onDismissRequest = {
                            showThemeAlertDialog.value = false
                        },
                        title = { Text(stringResource(R.string.theme)) },
                        text = {
                            ThemeRadioGroup(uiState, updateDayNight)
                        },
                        confirmButton = {
                            Button(onClick = {
                                showThemeAlertDialog.value = false
                            }
                            ) {
                                Text(stringResource(R.string.ok))
                            }
                        })
                }

                Text(
                    stringResource(R.string.appearance),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp, 8.dp, 0.dp, 4.dp),
                )
                Column(
                    modifier = Modifier
                        .clickable(onClick = {
                            showThemeAlertDialog.value = true
                        })
                        .fillMaxWidth()
                )
                {

                    Text(
                        stringResource(R.string.theme),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 0.dp)
                    )
                    Text(
                        stringResource(uiState.dayNightMode.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp)
                    )
                }
                Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
                        Row(verticalAlignment = Alignment.CenterVertically)
                        {
                            Text(
                                stringResource(R.string.material_you),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(12.dp, 18.dp, 0.dp, 18.dp)
                                    .weight(1.0f)
                            )
                            Switch(
                                uiState.materialYou,
                                onCheckedChange = {
                                    updateMaterialYou(it)
                                },
                                modifier = Modifier.padding(0.dp, 0.dp, 20.dp, 0.dp)
                            )
                        }
                    }
                    Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))
                }

                Text(
                    stringResource(R.string.advanced),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp, 16.dp, 0.dp, 4.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(12.dp, 12.dp, 12.dp, 12.dp)
                    )
                    {
                        Text(
                            stringResource(R.string.show_enable_disable_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.show_enable_disable_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        showEnableDisable,
                        onCheckedChange = {
                            updateShowEnableDisable(it)
                        },
                        modifier = Modifier.padding(0.dp, 0.dp, 20.dp, 0.dp)
                    )
                }
                Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(12.dp, 12.dp, 12.dp, 12.dp)
                    )
                    {
                        Text(
                            stringResource(R.string.show_all_local_rules_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // the derivation is in UpnpRepository.localRules; with two routers found
                        //   the same rule is listed under each of them.
                        Text(
                            stringResource(R.string.show_all_local_rules_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        showAllLocalRules,
                        onCheckedChange = {
                            updateShowAllLocalRules(it)
                        },
                        modifier = Modifier.padding(0.dp, 0.dp, 20.dp, 0.dp)
                    )
                }
                Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(12.dp, 12.dp, 12.dp, 12.dp)
                    )
                    {
                        Text(
                            stringResource(R.string.background_renew_title),
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.background_renew_summary),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        backgroundRenew,
                        onCheckedChange = {
                            updateBackgroundRenew(it)
                        },
                        modifier = Modifier.padding(0.dp, 0.dp, 20.dp, 0.dp)
                    )
                }
                Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))

//            Column(
//                modifier = Modifier.clickable(onClick = {
//
//                    showThemeAlertDialog.value = true
//
//
//                }))
//            {
//                Text("Theme", fontSize = 26.sp, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp))
//                var pref = when(AdditionalColors.ThemeSetting.value){
//                    0 -> followSystem
//                    1 -> light
//                    2 -> dark
//                    else -> followSystem
//                }
//                Text(pref, fontSize = 16.sp, modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
//            }
//            Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 4.dp, 0.dp, 0.dp))


            }
        }
    }

    @Composable
    @Preview
    fun RadioGroupPreviewTheme() {
        val themeState = ThemeUiState(DayNightMode.FORCE_NIGHT, false)
        MyApplicationTheme(themeState) {
            ThemeRadioGroup(themeState, { _ -> })
        }
    }

    @Composable
    fun ThemeRadioGroup(themeState: ThemeUiState, setDayNightMode: (DayNightMode) -> Unit) {
        Column {
            DayNightMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (mode == themeState.dayNightMode),
                        onClick = {
                            setDayNightMode(mode)
                        }
                    )
                    Text(text = stringResource(mode.labelRes))
                }
            }
        }
    }

    @Composable
    @Preview
    fun RadioGroupPreview() {
        val themeState = ThemeUiState(DayNightMode.FORCE_NIGHT, false)
        SetupPreview()
        ThemeRadioGroup(themeState, { _ -> })
    }

    @Composable
    @Preview
    fun RadioGroupPreview5() {
        RuleCreationDialog(rememberNavController(), hiltViewModel())
        Text("test")
    }

}
