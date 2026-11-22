package com.taskbloom.observer;

/** Subject del patrón Observer. */
public interface SujetoRecordatorio {
    void suscribir(ObservadorRecordatorio observador);
    void desuscribir(ObservadorRecordatorio observador);
    void notificarObservadores(Recordatorio recordatorio);
}
