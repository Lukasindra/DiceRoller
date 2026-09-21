package com.example.diceroller

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// ============================================================
// MainActivity: titik awal aplikasi
// ============================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Surface memberi warna latar belakang sesuai tema
                Surface(modifier = Modifier.fillMaxSize()) {
                    DiceRollerApp()
                }
            }
        }
    }
}

// ============================================================
// Fungsi pemetaan angka -> gambar (kontrol alur "when")
// Ini memenuhi syarat wajib: memakai when untuk memilih gambar.
// ============================================================
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

fun playDiceSound(context: Context) {
    val player = MediaPlayer.create(context, R.raw.dice_sound)
    player?.setOnCompletionListener { it.release() }
    player?.start()
}


@Composable
fun DiceRollerApp() {
    // ---- STATE: nilai yang jika berubah akan memperbarui tampilan ----
    var dice1 by remember { mutableIntStateOf(1) }       // nilai dadu pertama
    var dice2 by remember { mutableIntStateOf(1) }       // [DADU GANDA] nilai dadu kedua
    var isRolling by remember { mutableStateOf(false) } // true saat animasi berjalan
    val history = remember { mutableStateListOf<String>() } // [LOG RIWAYAT]

    val scope = rememberCoroutineScope() // untuk menjalankan animasi (coroutine)
    val context = LocalContext.current   // dibutuhkan untuk memutar suara

    // Column dengan Center + CenterHorizontally = semua isi berada di tengah layar
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ---- [DADU GANDA] dua gambar dadu berdampingan dalam satu Row ----
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(
                painter = painterResource(diceImage(dice1)),
                contentDescription = "Dadu 1: $dice1",
                modifier = Modifier.size(120.dp)
            )
            Image(
                painter = painterResource(diceImage(dice2)),
                contentDescription = "Dadu 2: $dice2",
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total kedua dadu (tampil setelah animasi selesai)
        Text(
            text = if (isRolling) "Mengocok..." else "Total: ${dice1 + dice2}",
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- TOMBOL KOCOK ----
        Button(
            onClick = {
                // Jalankan animasi di coroutine agar bisa memakai delay()
                scope.launch {
                    isRolling = true

                    // [SOUND EFFECT] mainkan suara saat mulai mengocok
                    // Jika belum punya file suara, beri tanda // di depan baris ini
                    playDiceSound(context)

                    // Hasil akhir ditentukan lebih dulu memakai Random
                    val final1 = (1..6).random()
                    val final2 = (1..6).random()

                    // [ANIMASI] ganti gambar acak 10 kali, jeda 100 ms tiap gantian
                    // sehingga terlihat seperti dadu sedang berputar
                    repeat(10) {
                        dice1 = (1..6).random()
                        dice2 = (1..6).random()
                        delay(100.milliseconds)
                    }

                    // Tampilkan hasil akhir
                    dice1 = final1
                    dice2 = final2

                    // [LOG RIWAYAT] tambahkan ke urutan paling atas
                    history.add(0, "$final1 & $final2  (total ${final1 + final2})")

                    isRolling = false
                }
            },
            enabled = !isRolling // tombol dimatikan selama animasi berjalan
        ) {
            Text("Kocok Dadu")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- [LOG RIWAYAT] tampilkan 5 kocokan terakhir ----
        Text(text = "Riwayat Kocokan", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text("Belum ada kocokan")
        } else {
            history.take(5).forEachIndexed { index, item ->
                Text("${index + 1}. $item")
            }
        }
    }
}