package bo.cdr.app.apporder.presentation.response.pojo;

/** Umbrales (minutos desde el registro) para marcar pedidos demorados en el tablero de cocina. */
public record KitchenSettingsPojo(int delayWarningMinutes, int delayCriticalMinutes) {
}
