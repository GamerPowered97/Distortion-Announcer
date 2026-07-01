package com.example.distortiontracker

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.distortiontracker.theme.DistortionTrackerTheme
import com.example.distortiontracker.ui.main.PermissionsScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      var hasAllPermissions by remember {
        mutableStateOf(
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
          (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms())
        )
      }

      val context = this@MainActivity
      val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

      androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
          if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            hasAllPermissions =
              (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
              (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms())
          }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
          lifecycleOwner.lifecycle.removeObserver(observer)
        }
      }

      DistortionTrackerTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
          if (hasAllPermissions) {
            com.example.distortiontracker.ui.main.MainScreen(modifier = Modifier.safeDrawingPadding())
          } else {
            PermissionsScreen(onPermissionsGranted = { hasAllPermissions = true })
          }
        } 
      }
    }
  }
}
