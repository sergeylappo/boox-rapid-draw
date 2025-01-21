package com.sergeylappo.booxrapiddraw

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.canDrawOverlays
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat.startForegroundService
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.onyx.android.sdk.utils.ActivityUtil.finish
import com.sergeylappo.booxrapiddraw.utils.isMyServiceRunning
import kotlin.system.exitProcess

class MainActivity : FragmentActivity() {
    override fun onResume() {
        super.onResume()

        // Replace fragment to ensure only one instance exists
        supportFragmentManager.commit {
            replace(android.R.id.content, MainFragment(), "main_fragment")
        }
    }
}

class MainFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiresPermission = !canDrawOverlays(context)
        if (requiresPermission) {
            DisplayRationale().show(childFragmentManager, "permission_rationale")
        } else {
            val context = requireContext()
            val isRunning = isMyServiceRunning(requireContext(), OverlayShowingService::class.java)
            if (isRunning) {
                stopService(context)
            } else {
                startService(context)
            }
            finish(this.activity)
        }
    }

    private fun startService(context: Context) {
        val svc = Intent(context, OverlayShowingService::class.java)
        startForegroundService(requireContext(), svc)
    }

    private fun stopService(context: Context) {
        val svc = Intent(context, OverlayShowingService::class.java).apply { action = "STOP" }
//      An attempt to start service second time would stop it.
//        If a service was not running (e.g. force-closed this would not cause a crash
//        since this does not tries to launch it as background)
        context.startService(svc)
    }
}

class DisplayRationale : DialogFragment() {
    private lateinit var permissionRequestLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        TODO maybe can be even simpler with just starting the activity without the result
        permissionRequestLauncher = registerForActivityResult(StartActivityForResult()) { _ -> }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction.
            val builder = AlertDialog.Builder(it)
            builder.setMessage(
                """This app requires a permission to draw over other apps.
                |Now you would be redirected to settings to enable the permission.
                |To enable, select "Boox Rapid Draw" and then toggle "Allow display over other apps" option.
                |
                |NOTE:
                |If you deny the permission, the app will not be able to function and would be closed.
                """.trimMargin()
            )
                .setPositiveButton("Allow") { _, _ ->
                    val requestPermissionIntent = Intent(ACTION_MANAGE_OVERLAY_PERMISSION)
                    requestPermissionIntent.setData(
                        Uri.parse("package:${requireContext().packageName}")
                    )
                    permissionRequestLauncher.launch(requestPermissionIntent)
                }
                .setNegativeButton("Close app") { _, _ ->
                    exitProcess(0)
                }
            // Create the AlertDialog object and return it.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionRequestLauncher.unregister()
    }
}
