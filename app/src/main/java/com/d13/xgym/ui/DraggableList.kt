package com.d13.xgym.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lista con reordenamiento por long-press + arrastre vertical, centralizado.
 *
 * El composable gestiona su propia copia de trabajo de [items] (re-sincronizada
 * cuando cambia la lista de entrada, p. ej. tras persistir en la BD) y expone a
 * cada ítem un [Modifier] de arrastre y una bandera [isDragged].
 *
 * Contrato para [itemContent]:
 * - Aplica `dragModifier` **después** del `padding(vertical = ...)` del ítem, para
 *   que la altura medida coincida con la Card y [itemSpacing] complete el hueco.
 * - Usa `isDragged` para elevar el ítem con `zIndex` donde corresponda.
 *
 * @param key identidad estable de cada ítem (no usar el índice, se invalida al reordenar).
 * @param onReorder se invoca al soltar con la lista ya reordenada, para persistir.
 * @param itemSpacing separación vertical total entre ítems (suma del padding sup. + inf.).
 * @param footer contenido opcional no arrastrable al final de la lista (scrollea con ella).
 */
@Composable
fun <T> DraggableList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 12.dp,
    footer: (@Composable () -> Unit)? = null,
    itemContent: @Composable (item: T, dragModifier: Modifier, isDragged: Boolean) -> Unit
) {
    var working by remember(items) { mutableStateOf(items) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(modifier) {
        itemsIndexed(working, key = { _, item -> key(item) }) { index, item ->
            val isDragged = index == draggedIndex
            val translationY = if (isDragged) dragOffset else 0f

            val dragModifier = Modifier
                .graphicsLayer { this.translationY = translationY }
                .pointerInput(key(item)) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            // Buscar la posición por id (estable), no por `index`:
                            // este bloque sobrevive a los reordenamientos.
                            draggedIndex = working
                                .indexOfFirst { key(it) == key(item) }
                                .takeIf { it >= 0 }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y

                            val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                            val itemHeight = size.height + itemSpacing.toPx()

                            var newIdx = currentIdx
                            if (dragOffset > itemHeight && currentIdx < working.size - 1) {
                                newIdx = currentIdx + 1
                                dragOffset -= itemHeight
                            } else if (dragOffset < -itemHeight && currentIdx > 0) {
                                newIdx = currentIdx - 1
                                dragOffset += itemHeight
                            }

                            if (newIdx != currentIdx) {
                                working = working.toMutableList().apply {
                                    val moved = removeAt(currentIdx)
                                    add(newIdx, moved)
                                }
                                draggedIndex = newIdx
                            }
                        },
                        onDragEnd = {
                            draggedIndex = null
                            dragOffset = 0f
                            onReorder(working)
                        },
                        onDragCancel = {
                            draggedIndex = null
                            dragOffset = 0f
                        }
                    )
                }

            itemContent(item, dragModifier, isDragged)
        }
        if (footer != null) {
            item { footer() }
        }
    }
}
