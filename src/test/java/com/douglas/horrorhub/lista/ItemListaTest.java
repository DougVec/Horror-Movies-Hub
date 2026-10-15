package com.douglas.horrorhub.lista;

import com.douglas.horrorhub.lista.ItemLista.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ItemListaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);
    private static final LocalDate AMANHA = HOJE.plusDays(1);

    private static ItemLista item(Status s) {
        return new ItemLista(null, null, s, HOJE);
    }

    @Test
    void quererVerNaoTemDataNemNota() {
        var i = item(Status.QUERO_VER);
        assertNull(i.getAssistidoEm());
        assertNull(i.getNota());
    }

    @Test
    void criarJaAssistidoMarcaAData() {
        assertEquals(HOJE, item(Status.ASSISTIDO).getAssistidoEm());
    }

    @Test
    void virarAssistidoMarcaADataUmaVezSo() {
        var i = item(Status.QUERO_VER);
        i.mudarStatus(Status.ASSISTIDO, HOJE);
        i.mudarStatus(Status.ASSISTIDO, AMANHA);
        assertEquals(HOJE, i.getAssistidoEm());
    }

    @Test
    void notaSoEmFilmeAssistido() {
        assertThrows(IllegalStateException.class, () -> item(Status.QUERO_VER).avaliar(8));
        assertThrows(IllegalStateException.class, () -> item(Status.ABANDONADO).avaliar(8));
        var i = item(Status.ASSISTIDO);
        i.avaliar(8);
        assertEquals(8, i.getNota());
    }

    @Test
    void notaForaDaFaixaERejeitada() {
        var i = item(Status.ASSISTIDO);
        assertThrows(IllegalArgumentException.class, () -> i.avaliar(11));
        assertThrows(IllegalArgumentException.class, () -> i.avaliar(-1));
        assertDoesNotThrow(() -> i.avaliar(0));
        assertDoesNotThrow(() -> i.avaliar(10));
    }

    @Test
    void sairDeAssistidoLimpaDataENota() {
        var i = item(Status.ASSISTIDO);
        i.avaliar(9);
        i.mudarStatus(Status.ABANDONADO, AMANHA);
        assertNull(i.getAssistidoEm());
        assertNull(i.getNota());
        assertEquals(Status.ABANDONADO, i.getStatus());
    }
}
