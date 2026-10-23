package cat.gastrobacus.consum.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cat.gastrobacus.consum.Serveis
import cat.gastrobacus.consum.dades.ErrorApp
import cat.gastrobacus.consum.google.ErrorGoogle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface Pantalla {
    data object QuiEts : Pantalla
    data class Pin(val sociId: String) : Pantalla
    data class Area(val sociId: String) : Pantalla
}

class AppModel(app: Application) : AndroidViewModel(app) {
    val serveis = Serveis.de(app)
    val repo = serveis.repo
    val estat = repo.estat

    var pantalla by mutableStateOf<Pantalla>(Pantalla.QuiEts)
        private set
    var ocupat by mutableStateOf(false)
        private set

    /** La junta ha desfixat la pantalla manualment: no la tornem a fixar fins que ho demani. */
    var pantallaDesfixada by mutableStateOf(false)

    private val _missatges = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val missatges = _missatges.asSharedFlow()
    fun avisa(t: String) { _missatges.tryEmit(t) }

    fun triaSoci(id: String) { pantalla = Pantalla.Pin(id) }
    fun entra(id: String) { pantalla = Pantalla.Area(id) }
    fun surt() { pantalla = Pantalla.QuiEts }

    /** Executa una acció: mostra el missatge si va bé, o l'error si no. */
    fun accio(ok: String? = null, despres: (() -> Unit)? = null, bloc: suspend () -> Unit) {
        viewModelScope.launch {
            ocupat = true
            try {
                bloc()
                ok?.let { avisa(it) }
                despres?.invoke()
            } catch (e: ErrorApp) {
                avisa(e.message ?: "No s'ha pogut fer.")
            } catch (e: ErrorGoogle) {
                avisa(e.message ?: "Error amb Google.")
            } catch (e: Exception) {
                avisa("Error inesperat: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                ocupat = false
            }
        }
    }

    /** Com [accio], però retorna el resultat a [amb]. */
    fun <T> accioAmb(bloc: suspend () -> T, amb: (T) -> Unit) {
        viewModelScope.launch {
            ocupat = true
            try {
                amb(bloc())
            } catch (e: ErrorApp) {
                avisa(e.message ?: "No s'ha pogut fer.")
            } catch (e: ErrorGoogle) {
                avisa(e.message ?: "Error amb Google.")
            } catch (e: Exception) {
                avisa("Error inesperat: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                ocupat = false
            }
        }
    }
}
