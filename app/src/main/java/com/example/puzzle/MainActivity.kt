package com.example.puzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.io.InputStream

val PastelBackground = Color(0xFFF6F1F1)
val PastelAccent = Color(0xFFCDB4DB)
val ButtonColor = Color(0xFFB8E0D2)
val TextColorDark = Color(0xFF3B3B3B)
val GridLineColor = Color(0xFFD26EFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuzzleGameApp()
        }
    }
}

@Composable
fun PuzzleGameApp() {
    var currentScreen by remember { mutableStateOf("menu") }
    val difficulty = 3
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var puzzlePieces by remember { mutableStateOf<List<PuzzlePiece>>(emptyList()) }
    var isGameWon by remember { mutableStateOf(false) }
    val context = LocalContext.current

    when (currentScreen) {
        "menu" -> MenuScreen(
            onPlayClick = { currentScreen = "imagePicker" },
            onExitClick = { Process.killProcess(Process.myPid()) }
        )
        "imagePicker" -> ImagePickerScreen(
            onImageSelected = { uri ->
                imageUri = uri
                val bitmap = loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    puzzlePieces = createPuzzlePieces(bitmap, difficulty)
                    isGameWon = false
                    currentScreen = "game"
                }
            },
            onBackClick = { currentScreen = "menu" }
        )
        "game" -> {
            if (isGameWon) {
                WinScreen(
                    onPlayAgain = { currentScreen = "imagePicker" },
                    onMenuClick = { currentScreen = "menu" }
                )
            } else {
                GameScreen(
                    puzzlePieces = puzzlePieces,
                    difficulty = difficulty,
                    onPieceMoved = { updatedPieces ->
                        puzzlePieces = updatedPieces
                        if (isPuzzleSolved(updatedPieces)) {
                            isGameWon = true
                        }
                    },
                    onBackClick = { currentScreen = "imagePicker" }
                )
            }
        }
    }
}

@Composable
fun MenuScreen(onPlayClick: () -> Unit, onExitClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PastelBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PuzzleApp",

            color = PastelAccent,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 50.dp)
        )

        Button(
            onClick = onPlayClick,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp)
        ) {
            Text("Start", fontSize = 20.sp, color = TextColorDark)
        }

        Button(
            onClick = onExitClick,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Exit", fontSize = 20.sp, color = TextColorDark)
        }
    }
}

@Composable
fun ImagePickerScreen(onImageSelected: (Uri) -> Unit, onBackClick: () -> Unit) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { onImageSelected(it) } }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PastelBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select a picture",
            color = PastelAccent,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        Button(
            onClick = { imagePicker.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Text("Open the gallery", fontSize = 20.sp, color = TextColorDark)
        }

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Back", fontSize = 20.sp, color = TextColorDark)
        }
    }
}

@Composable
fun GameScreen(
    puzzlePieces: List<PuzzlePiece>,
    difficulty: Int,
    onPieceMoved: (List<PuzzlePiece>) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedPieceIndex by remember { mutableStateOf(-1) }
    val boxSize = 300.dp
    val pieceSize = boxSize / difficulty

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PastelBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Put the puzzle together!",
            color = PastelAccent,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(boxSize)
                .background(Color.White.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, GridLineColor, RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / difficulty
                for (i in 1 until difficulty) {
                    drawLine(GridLineColor, Offset(i * cellSize, 0f), Offset(i * cellSize, size.height), strokeWidth = 2f)
                    drawLine(GridLineColor, Offset(0f, i * cellSize), Offset(size.width, i * cellSize), strokeWidth = 2f)
                }
            }

            puzzlePieces.forEachIndexed { index, piece ->
                val row = piece.currentPosition / difficulty
                val col = piece.currentPosition % difficulty

                Box(
                    modifier = Modifier
                        .size(pieceSize)
                        .offset(x = pieceSize * col, y = pieceSize * row)
                        .zIndex(if (selectedPieceIndex == index) 1f else 0f)
                        .clickable { selectedPieceIndex = index }
                ) {
                    Image(
                        bitmap = piece.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = if (selectedPieceIndex == index) 2.dp else 0.dp,
                                color = PastelAccent,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedPieceIndex != -1) {
                    val emptyPosition = findEmptyPosition(puzzlePieces, difficulty)
                    val selectedPosition = puzzlePieces[selectedPieceIndex].currentPosition
                    if (arePositionsAdjacent(selectedPosition, emptyPosition, difficulty)) {
                        val updatedPieces = puzzlePieces.toMutableList()
                        updatedPieces[selectedPieceIndex] = updatedPieces[selectedPieceIndex].copy(currentPosition = emptyPosition)
                        onPieceMoved(updatedPieces)
                        selectedPieceIndex = -1
                    }
                }
            },
            enabled = selectedPieceIndex != -1,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Move", fontSize = 20.sp, color = TextColorDark)
        }

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Back", fontSize = 20.sp, color = TextColorDark)
        }
    }
}

@Composable
fun WinScreen(onPlayAgain: () -> Unit, onMenuClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PastelBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Congratulations!", color = PastelAccent, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("You have completed the puzzle!", color = TextColorDark, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Play again", fontSize = 20.sp, color = TextColorDark)
        }

        Button(
            onClick = onMenuClick,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonColor),
            modifier = Modifier.fillMaxWidth(0.7f).padding(top = 16.dp)
        ) {
            Text("The main menu", fontSize = 20.sp, color = TextColorDark)
        }
    }
}

fun findEmptyPosition(pieces: List<PuzzlePiece>, difficulty: Int): Int {
    val allPositions = List(difficulty * difficulty) { it }
    val occupiedPositions = pieces.map { it.currentPosition }
    return allPositions.first { it !in occupiedPositions }
}

fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        bitmap
    } catch (e: Exception) {
        null
    }
}

fun createPuzzlePieces(fullBitmap: Bitmap, difficulty: Int): List<PuzzlePiece> {
    val size = minOf(fullBitmap.width, fullBitmap.height)
    val squareBitmap = Bitmap.createBitmap(
        fullBitmap,
        (fullBitmap.width - size) / 2,
        (fullBitmap.height - size) / 2,
        size, size
    )

    val pieceSize = size / difficulty
    val pieces = mutableListOf<PuzzlePiece>()

    for (row in 0 until difficulty) {
        for (col in 0 until difficulty) {
            val x = col * pieceSize
            val y = row * pieceSize
            val correctPosition = row * difficulty + col

            if (row == difficulty - 1 && col == difficulty - 1) continue

            val pieceBitmap = Bitmap.createBitmap(squareBitmap, x, y, pieceSize, pieceSize)
            pieces.add(PuzzlePiece(pieceBitmap.asImageBitmap(), correctPosition, correctPosition))
        }
    }

    pieces.shuffle()
    val emptyPosition = difficulty * difficulty - 1
    val availablePositions = (0 until difficulty * difficulty).toMutableList()
    availablePositions.remove(emptyPosition)

    for (i in pieces.indices) {
        pieces[i] = pieces[i].copy(currentPosition = availablePositions[i])
    }

    return pieces
}

fun isPuzzleSolved(pieces: List<PuzzlePiece>): Boolean {
    return pieces.all { it.currentPosition == it.correctPosition }
}

fun arePositionsAdjacent(pos1: Int, pos2: Int, difficulty: Int): Boolean {
    val row1 = pos1 / difficulty
    val col1 = pos1 % difficulty
    val row2 = pos2 / difficulty
    val col2 = pos2 % difficulty

    return (kotlin.math.abs(row1 - row2) == 1 && col1 == col2) ||
            (kotlin.math.abs(col1 - col2) == 1 && row1 == row2)
}

data class PuzzlePiece(
    val bitmap: ImageBitmap,
    val correctPosition: Int,
    var currentPosition: Int
)
