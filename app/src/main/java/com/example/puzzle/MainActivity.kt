package com.example.puzzle

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.puzzle.ui.theme.PuzzleTheme
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.InputStream


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
    var difficulty by remember { mutableStateOf(3) } // 3x3 by default
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var puzzlePieces by remember { mutableStateOf<List<PuzzlePiece>>(emptyList()) }
    var isGameWon by remember { mutableStateOf(false) }

    when (currentScreen) {
        "menu" -> MenuScreen(
            onPlayClick = { currentScreen = "difficulty" },
            onExitClick = { /* Close app */ }
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
                puzzlePieces = createPuzzlePieces(uri, difficulty)
                currentScreen = "game"
            },
            onBackClick = { currentScreen = "difficulty" }
        )
        "game" -> {
            if (isGameWon) {
                WinScreen(
                    onPlayAgain = {
                        puzzlePieces = createPuzzlePieces(imageUri, difficulty)
                        isGameWon = false
                    },
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
    val context = LocalContext.current
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
fun GameScreen(
    puzzlePieces: List<PuzzlePiece>,
    difficulty: Int,
    onPieceMoved: (List<PuzzlePiece>) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedPieceIndex by remember { mutableStateOf(-1) }
    val context = LocalContext.current

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
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            // Puzzle grid
            for (i in 0 until difficulty * difficulty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55FFFFFF))
                    )
                }
            }

            // Puzzle pieces
            puzzlePieces.forEachIndexed { index, piece ->
                if (piece.currentPosition != piece.correctPosition) {
                    val bitmap = remember(piece.imageUri) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(piece.imageUri)
                            BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (selectedPieceIndex == index) Modifier.zIndex(1f) else Modifier
                                )
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Puzzle piece",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize(1f / difficulty)
                                    .padding(2.dp)
                                    .clickable {
                                        selectedPieceIndex = if (selectedPieceIndex == index) -1 else index
                                    }
                                    .then(
                                        if (selectedPieceIndex == index) Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                        else Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (selectedPieceIndex != -1) {
                    // Find empty position
                    val emptyPosition = (0 until difficulty * difficulty).first { pos ->
                        puzzlePieces.none { it.currentPosition == pos }
                    }

                    // Check if selected piece is adjacent to empty position
                    if (arePositionsAdjacent(
                            puzzlePieces[selectedPieceIndex].currentPosition,
                            emptyPosition,
                            difficulty
                        )
                    ) {
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
                .padding(top = 16.dp)
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

data class PuzzlePiece(
    val imageUri: Uri,
    val correctPosition: Int,
    var currentPosition: Int
)

fun createPuzzlePieces(imageUri: Uri?, difficulty: Int): List<PuzzlePiece> {
    if (imageUri == null) return emptyList()

    val totalPieces = difficulty * difficulty
    val pieces = mutableListOf<PuzzlePiece>()

    // Create pieces with correct positions
    for (i in 0 until totalPieces) {
        pieces.add(PuzzlePiece(imageUri, i, i))
    }

    // Shuffle pieces (leave one empty space)
    val shuffledPieces = pieces.toMutableList()
    shuffledPieces.removeAt(totalPieces - 1) // Remove last piece to create empty space

    // Randomly shuffle
    shuffledPieces.shuffle()

    // Assign new positions
    var position = 0
    val result = mutableListOf<PuzzlePiece>()

    for (piece in shuffledPieces) {
        result.add(piece.copy(currentPosition = position))
        position++
    }

    // Add empty space
    position++

    return result
}

fun isPuzzleSolved(puzzlePieces: List<PuzzlePiece>): Boolean {
    return puzzlePieces.all { it.currentPosition == it.correctPosition }
}

fun arePositionsAdjacent(pos1: Int, pos2: Int, difficulty: Int): Boolean {
    val row1 = pos1 / difficulty
    val col1 = pos1 % difficulty
    val row2 = pos2 / difficulty
    val col2 = pos2 % difficulty

    return (Math.abs(row1 - row2) == 1 && col1 == col2) ||
            (Math.abs(col1 - col2) == 1 && row1 == row2)
}