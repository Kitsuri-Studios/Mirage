package io.kitsuri.m1rage.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.kitsuri.m1rage.R
import io.kitsuri.m1rage.BuildConfig
import java.security.MessageDigest
private val device = buildString {
    append(Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1))
    if (Build.BRAND != Build.MANUFACTURER) {
        append(" " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1))
    }
    append(" " + Build.MODEL + " ")
}

fun sha256(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(bytes).joinToString("") { "%02x".format(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
 fun InfoCard() {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val contents = StringBuilder()
            val infoCardContent: @Composable (Pair<String, String>) -> Unit = { texts ->
                contents.appendLine(texts.first).appendLine(texts.second).appendLine()
                Text(text = texts.first, style = MaterialTheme.typography.bodyLarge)
                Text(text = texts.second, style = MaterialTheme.typography.bodyMedium)
            }


            val hxoLibs = getHxoLibInfo(context)

            infoCardContent(
                "HXO Version" to BuildConfig.HXO_VERSION
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = "HXO Libraries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            hxoLibs.forEach { (abi, checksum) ->
                Spacer(Modifier.height(8.dp))
                infoCardContent(
                    abi to "SHA-256: $checksum"
                )
            }


            Spacer(Modifier.height(24.dp))
            infoCardContent(stringResource(R.string.home_device) to device)

            Spacer(Modifier.height(24.dp))
            infoCardContent(
                stringResource(R.string.home_system_abi) to
                        Build.SUPPORTED_ABIS.joinToString(", ")
            )

            val copiedMessage = stringResource(R.string.home_info_copied)
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Mirage", contents.toString()))
                    scope.launch { snackbarHost.showSnackbar(copiedMessage) }
                },
                content = { Text(stringResource(android.R.string.copy)) }
            )
        }
    }
}

fun loadAssetChecksum(
    context: Context,
    path: String
): String? = try {
    context.assets.open(path).use {
        sha256(it.readBytes())
    }
} catch (e: Exception) {
    null
}

fun getHxoLibInfo(context: Context): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()

    Build.SUPPORTED_ABIS.forEach { abi ->
        val path = "libs/$abi/libhxo.so"
        val checksum = loadAssetChecksum(context, path)

        if (checksum != null) {
            result += abi to checksum
        }
    }
    return result
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportCard() {
    val context = LocalContext.current

    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.home_support),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                text = "Created By Kitsuri Studios\nLicenced Under GNU GPL 3.0",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Kitsuri-Studios/Mirage")
                            )
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(ir.alirezaivaz.tablericons.R.drawable.ic_brand_github),
                        contentDescription = "GitHub"
                    )
                }

                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://discord.gg/tt25ff6WH6")
                            )
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(ir.alirezaivaz.tablericons.R.drawable.ic_brand_discord),
                        contentDescription = "Discord"
                    )
                }
            }
        }
    }
}
