package com.example.diceroller

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DiceRollerApp()
                }
            }
        }
    }
}

fun diceImage(value: Int): Int {
    return when (value) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        else -> R.drawable.dice_6
    }
}

// Sound effect: memutar res/raw/dice_sound
fun playDiceSound(context: Context) {
    val player = MediaPlayer.create(context, R.raw.dice_sound)
    player?.setOnCompletionListener { it.release() }
    player?.start()
}

// ============================================================
// [PILIHAN JUMLAH DADU] tombol pilihan 1 atau 2 dadu
// Tombol yang sedang dipilih tampil solid (Button),
// yang tidak dipilih tampil garis tepi (OutlinedButton).
// ============================================================
@Composable
fun DiceCountOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(label) }
    }
}

// Tampilan utama
@Composable
fun DiceRollerApp() {
    // ---- STATE ----
    var diceCount by remember { mutableIntStateOf(2) }   // [PILIHAN] 1 atau 2 dadu
    var dice1 by remember { mutableIntStateOf(1) }
    var dice2 by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    // Sudut rotasi dadu (0 sampai 360 derajat)
    val rotation = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // [RIWAYAT] state scroll; otomatis turun ke kocokan terbaru
    val listState = rememberLazyListState()
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ---- JUDUL ----
        Text(
            text = "🎲 Dice Roller",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---- PILIHAN JUMLAH DADU ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiceCountOption(
                label = "1 Dadu",
                selected = diceCount == 1,
                enabled = !isRolling,
                onClick = { diceCount = 1 }
            )
            DiceCountOption(
                label = "2 Dadu",
                selected = diceCount == 2,
                enabled = !isRolling,
                onClick = { diceCount = 2 }
            )
        }

        // ---- BAGIAN TENGAH: dadu, total, tombol ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Jarak atas: menentukan posisi dadu (proporsi 0.6 dari ruang kosong)
            Spacer(modifier = Modifier.weight(0.6f))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(
                    painter = painterResource(diceImage(dice1)),
                    contentDescription = "Dadu 1: $dice1",
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(rotation.value)
                )
                // Dadu kedua hanya tampil jika mode 2 dadu
                if (diceCount == 2) {
                    Image(
                        painter = painterResource(diceImage(dice2)),
                        contentDescription = "Dadu 2: $dice2",
                        modifier = Modifier
                            .size(120.dp)
                            .rotate(rotation.value)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Teks hasil menyesuaikan jumlah dadu
            Text(
                text = when {
                    isRolling -> "Mengocok..."
                    diceCount == 2 -> "Total: ${dice1 + dice2}"
                    else -> "Hasil: $dice1"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // ---- TOMBOL KOCOK ----
                Button(
                    onClick = {
                        scope.launch {
                            // Simpan pilihan saat tombol ditekan
                            val mode = diceCount

                            isRolling = true
                            playDiceSound(context)

                            // Hasil akhir ditentukan lebih dulu (Random 1 sampai 6)
                            val final1 = (1..6).random()
                            val final2 = (1..6).random()

                            // Putar dadu satu putaran penuh selama 1 detik
                            launch {
                                rotation.snapTo(0f)
                                rotation.animateTo(360f, animationSpec = tween(durationMillis = 1000))
                                rotation.snapTo(0f)
                            }

                            // Ganti gambar acak 10 kali (jeda 100 ms)
                            repeat(10) {
                                dice1 = (1..6).random()
                                if (mode == 2) dice2 = (1..6).random()
                                delay(100.milliseconds)
                            }

                            // Tampilkan hasil akhir
                            dice1 = final1
                            if (mode == 2) dice2 = final2

                            // Catat ke riwayat sesuai mode (urut dari #1, terbaru di bawah)
                            val nomor = history.size + 1
                            val catatan = if (mode == 2) {
                                "#$nomor:  $final1 & $final2  (total ${final1 + final2})"
                            } else {
                                "#$nomor:  $final1"
                            }
                            history.add(catatan)

                            isRolling = false
                        }
                    },
                    enabled = !isRolling,
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("Kocok Dadu")
                }

                // ---- TOMBOL HAPUS RIWAYAT ----
                OutlinedButton(
                    onClick = { history.clear() },
                    enabled = !isRolling && history.isNotEmpty(),
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("Hapus Riwayat")
                }
            }

            // Jarak bawah: sisa ruang kosong di bawah tombol (proporsi 0.4)
            Spacer(modifier = Modifier.weight(0.4f))
        }

        // ---- RIWAYAT (bisa di-scroll) ----
        Text(
            text = "Riwayat Kocokan (${history.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Belum ada kocokan")
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(history) { item ->
                        Text(
                            text = item,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}