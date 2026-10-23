package cat.gastrobacus.consum

import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.lang.ref.WeakReference

/**
 * Fixació de pantalla (mode quiosc senzill d'Android) i arrencada automàtica.
 * No cal ser propietari del dispositiu: Android demana confirmació la primera vegada.
 */
object Quiosc {
    private var activitat: WeakReference<Activity>? = null
    fun registra(a: Activity) { activitat = WeakReference(a) }

    fun estaFixada(context: Context): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

    fun fixa() { activitat?.get()?.let { runCatching { if (!estaFixada(it)) it.startLockTask() } } }
    fun desfixa() { activitat?.get()?.let { runCatching { if (estaFixada(it)) it.stopLockTask() } } }

    /** Per obrir-se sola en arrencar, Android 10+ demana el permís «Mostra sobre altres apps». */
    fun potObrirseSola(context: Context) = Settings.canDrawOverlays(context)

    fun obreAjustPermis(context: Context) {
        desfixa()
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun obreAjustInici(context: Context) {
        desfixa()
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** Obre l'app quan la tauleta acaba d'engegar-se. */
class Arrencada : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        programaCopiaDiaria(context)
        if (Quiosc.potObrirseSola(context)) {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
