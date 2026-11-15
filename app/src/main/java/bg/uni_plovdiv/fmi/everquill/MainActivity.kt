package bg.uni_plovdiv.fmi.everquill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import bg.uni_plovdiv.fmi.everquill.ui.navigation.MainScreen
import bg.uni_plovdiv.fmi.everquill.ui.theme.EverquillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EverquillTheme {
                MainScreen()
            }
        }
    }
}
