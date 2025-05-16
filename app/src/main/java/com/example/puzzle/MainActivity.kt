package com.example.puzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.puzzle.ui.theme.PuzzleTheme
import java.io.InputStream
import android.os.Process

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PuzzleTheme {
                PuzzleGameApp()
            }
        }
    }
}

@Composable
fun PuzzleGameApp() {
    var currentScreen by remember { mutableStateOf("menu") }
    var difficulty by remember { mutableStateOf(3) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var puzzlePieces by remember { mutableStateOf<List<PuzzlePiece>>(emptyList()) }
    var isGameWon by remember { mutableStateOf(false) }
    val context = LocalContext.current

    when (currentScreen) {
        "menu" -> MenuScreen(
            onPlayClick = { currentScreen = "difficulty" },
            onExitClick = { Process.killProcess(Process.myPid()) }
        )
        "difficulty" -> DifficultyScreen(
            onDifficultySelected = { selectedDifficulty ->
                difficulty = selectedDifficulty
                currentScreen = "imagePicker"
            },
            onBackClick = { currentScreen = "menu" }
        )
        "imagePicker" -> ImagePickerScreen(
            onImageSelected = { uri ->
                imageUri = uri
                currentScreen = "game"
            },
            onBackClick = { currentScreen = "difficulty" }
        )
        "game" -> {
            if (isGameWon) {
                WinScreen(
                    onPlayAgain = {
                        puzzlePieces = emptyList()
                        isGameWon = false
                        currentScreen = "imagePicker"
                    },
                    onMenuClick = { currentScreen = "menu" }
                )
            } else {
                if (puzzlePieces.isEmpty() && imageUri != null) {
                    LaunchedEffect(imageUri) {
                        val bitmap = loadBitmapFromUri(context, imageUri!!)
                        if (bitmap != null) {
                            puzzlePieces = createPuzzlePieces(bitmap, difficulty)
                        }
                    }
                }

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
            .background(Color(0xFF6A5ACD))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Складіть пазл!",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .size(boxSize)
                .background(Color(0x55FFFFFF))
        ) {
            // Draw grid lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / difficulty
                for (i in 1 until difficulty) {
                    // Vertical lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(i * cellSize, 0f),
                        end = Offset(i * cellSize, size.height),
                        strokeWidth = 2f
                    )
                    // Horizontal lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(0f, i * cellSize),
                        end = Offset(size.width, i * cellSize),
                        strokeWidth = 2f
                    )
                }
            }

            // Display puzzle pieces
            puzzlePieces.forEachIndexed { index, piece ->
                val row = piece.currentPosition / difficulty
                val col = piece.currentPosition % difficulty

                Box(
                    modifier = Modifier
                        .size(pieceSize)
                        .offset(
                            x = pieceSize * col,
                            y = pieceSize * row
                        )
                        .zIndex(if (selectedPieceIndex == index) 1f else 0f)
                        .clickable { selectedPieceIndex = index }
                ) {
                    Image(
                        bitmap = piece.bitmap,
                        contentDescription = "Puzzle piece",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                width = if (selectedPieceIndex == index) 2.dp else 0.dp,
                                color = Color.Yellow,
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
                        updatedPieces[selectedPieceIndex] = updatedPieces[selectedPieceIndex].copy(
                            currentPosition = emptyPosition
                        )
                        onPieceMoved(updatedPieces)
                        selectedPieceIndex = -1
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp),
            enabled = selectedPieceIndex != -1
        ) {
            Text("Перемістити", fontSize = 20.sp)
        }

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
        ) {
            Text("Назад", fontSize = 20.sp)
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

            if (row == difficulty - 1 && col == difficulty - 1) {
                continue
            }

            val pieceBitmap = Bitmap.createBitmap(squareBitmap, x, y, pieceSize, pieceSize)
            pieces.add(
                PuzzlePiece(
                    bitmap = pieceBitmap.asImageBitmap(),
                    correctPosition = correctPosition,
                    currentPosition = correctPosition
                )
            )
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

    return (Math.abs(row1 - row2) == 1 && col1 == col2) ||
            (Math.abs(col1 - col2) == 1 && row1 == row2)
}

data class PuzzlePiece(
    val bitmap: ImageBitmap,
    val correctPosition: Int,
    var currentPosition: Int
)

@Composable
fun MenuScreen(onPlayClick: () -> Unit, onExitClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A5ACD)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Пазл Гра",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 50.dp)
        )

        Button(
            onClick = onPlayClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 8.dp)
        ) {
            Text("Грати", fontSize = 20.sp)
        }

        Button(
            onClick = onExitClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
        ) {
            Text("Вийти", fontSize = 20.sp)
        }
    }
}

@Composable
fun DifficultyScreen(onDifficultySelected: (Int) -> Unit, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A5ACD)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Оберіть складність",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        Button(
            onClick = { onDifficultySelected(3) },
            modifier = Modifier
                .fillMaxWidth(0.7f)
        ) {
            Text("Легко (3x3)", fontSize = 20.sp)
        }

        Button(
            onClick = { onDifficultySelected(4) },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 16.dp)
        ) {
            Text("Середньо (4x4)", fontSize = 20.sp)
        }

        Button(
            onClick = { onDifficultySelected(5) },
            modifier = Modifier
                .fillMaxWidth(0.7f)
        ) {
            Text("Складно (5x5)", fontSize = 20.sp)
        }

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(top = 30.dp)
        ) {
            Text("Назад", fontSize = 20.sp)
        }
    }
}

@Composable
fun ImagePickerScreen(onImageSelected: (Uri) -> Unit, onBackClick: () -> Unit) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { onImageSelected(it) }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A5ACD)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Оберіть зображення",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Text("Відкрити галерею", fontSize = 20.sp)
        }

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
        ) {
            Text("Назад", fontSize = 20.sp)
        }
    }
}

@Composable
fun WinScreen(onPlayAgain: () -> Unit, onMenuClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF6A5ACD)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Вітаємо!",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Ви склали пазл!",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 16.dp)
        ) {
            Text("Грати знову", fontSize = 20.sp)
        }

        Button(
            onClick = onMenuClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
        ) {
            Text("Головне меню", fontSize = 20.sp)
        }
    }
}