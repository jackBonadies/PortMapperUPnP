package com.shinjiindustrial.portmapper

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinjiindustrial.portmapper.ui.theme.AdditionalColors
import com.shinjiindustrial.portmapper.ui.theme.MyApplicationTheme


class SettingsActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        PortForwardApplication.CurrentActivity = this
        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(onClick = {
                                    this.finish()
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = AdditionalColors.TextColorStrong)
                                }
                            },
//                                modifier = Modifier.height(40.dp),
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AdditionalColors.TopAppBarColor),
                            title = {
                                Text(
                                    text = "Settings",
                                    color = AdditionalColors.TextColorStrong,
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            //actions = { OverflowMenu() }
                        )
                    },
                    content = { it ->
                        Settings(Modifier.padding(it))
                    }
                )
            }
        }
    }
}

@Composable
@Preview
fun SettingsPreview()
{
    SetupPreview()
    Settings()
}


@Composable
fun Settings(modifier : Modifier = Modifier)
{
    MyApplicationTheme()
    {
        Column(modifier = Modifier.then(modifier))
        {

            var showThemeAlertDialog = remember { mutableStateOf(false) }

            if(showThemeAlertDialog.value)
            {
                // alert dialog...
                AlertDialog(
                    onDismissRequest = {
                        PortForwardApplication.instance.SaveSharedPrefs()
                        showThemeAlertDialog.value = false
                                       },
                    title = { Text("Theme") },
                    text = {

                        RadioGroupExample()

                    },
                    confirmButton = {
                        Button(onClick = {
                            showThemeAlertDialog.value = false
                            PortForwardApplication.instance.SaveSharedPrefs()
                        }
                        ) {
                            Text("OK")
                        }
                    })
            }

            Text(
                "Appearance",
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
                    .fillMaxWidth())
            {

                Text("Theme", fontSize = 26.sp, color = AdditionalColors.TextColorStrong, modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp))
                var pref = when(AdditionalColors.ThemeSetting.value){
                    0 -> followSystem
                    1 -> light
                    2 -> dark
                    else -> followSystem
                }
                Text(pref, fontSize = 16.sp, modifier = Modifier
                    .padding(12.dp, 0.dp, 0.dp, 4.dp)
                    .offset(
                        0.dp, (-4).dp
                    ), color = AdditionalColors.TextColorWeak)
            }
            Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {
                    Row(verticalAlignment = Alignment.CenterVertically)
                    {
                        //Icon(Icons.Filled.Palette, "Material You", tint=MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp))
                        Text(
                            "Material You",
                            fontSize = 26.sp,
                            color = AdditionalColors.TextColorStrong,
                            modifier = Modifier
                                .padding(12.dp, 18.dp, 0.dp, 18.dp)
                                .weight(1.0f)
                        )
                        Switch(
                            AdditionalColors.ThemeSettingMaterialYou.value,
                            onCheckedChange = {

                                AdditionalColors.ThemeSettingMaterialYou.value = it
                                SharedPrefValues.MaterialYouTheme = it

                            },
                            modifier = Modifier.padding(0.dp, 0.dp, 20.dp, 0.dp)
                        )
                    }
                }
                Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp))
            }

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
//                Text(pref, fontSize = 16.sp, modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp), color = AdditionalColors.TextColorWeak)
//            }
//            Divider(thickness = 1.dp, modifier = Modifier.padding(0.dp, 4.dp, 0.dp, 0.dp))


        }
    }
}

@Composable
@Preview
fun RadioGroupPreviewTheme()
{
    MyApplicationTheme() {
        RadioGroupExample()
    }
}

val followSystem = "Follow System"
val light = "Light"
val dark = "Dark"

@Composable
fun RadioGroupExample() {
        val options = listOf(followSystem, light, dark)
        var selectedOption = remember { mutableStateOf(options[AdditionalColors.ThemeSetting.value]) }

        Column {
            options.forEach { text ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = (text == selectedOption.value),
                        onClick = {
                            var dayNightMode = when(text)
                            {
                                followSystem -> DayNightMode.FOLLOW_SYSTEM
                                light -> DayNightMode.FORCE_DAY
                                dark -> DayNightMode.FORCE_NIGHT
                                else -> DayNightMode.FOLLOW_SYSTEM
                            }

                            AdditionalColors.ThemeSetting.value = dayNightMode.intVal
                            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //useless
                            selectedOption.value = text
                            SharedPrefValues.DayNightPref = DayNightMode.FORCE_DAY

                        }
                    )
                    Text(text = text)
                }
            }
        }
}

@Composable
@Preview
fun SetupPreview()
{
    SharedPrefValues.DayNightPref = DayNightMode.FORCE_NIGHT
    SharedPrefValues.MaterialYouTheme = false
    PortForwardApplication.appContext = LocalContext.current
}

@Composable
@Preview
fun RadioGroupPreview()
{
    SetupPreview()
    RadioGroupExample()
}